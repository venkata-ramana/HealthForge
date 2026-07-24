#!/usr/bin/env bash
set -euo pipefail

api_url="${HEALTHFORGE_API_URL:-http://localhost:8080}"
corpus_id="${1:?usage: evaluate-retrieval.sh CORPUS_ID CORPUS_VERSION [REPORT_PATH]}"
corpus_version="${2:?usage: evaluate-retrieval.sh CORPUS_ID CORPUS_VERSION [REPORT_PATH]}"
report_path="${3:-evals/reports/retrieval-$(date -u +%Y%m%dT%H%M%SZ).json}"
dataset="evals/datasets/cms-0057-f-mvp-evaluation-cases.json"

command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }
curl --fail --silent --show-error "$api_url/v1/corpus-snapshots/$corpus_id/$corpus_version" >/dev/null

mkdir -p "$(dirname "$report_path")"
case_file="$(mktemp)"
trap 'rm -f "$case_file"' EXIT

jq -c '.cases[]' "$dataset" | while IFS= read -r case_json; do
  question="$(jq -r '.question' <<<"$case_json")"
  targets="$(jq -c '[.expected_evidence_targets[].source_id]' <<<"$case_json")"
  response="$(curl --fail --silent --show-error -X POST "$api_url/v1/retrieval/search" -H 'Content-Type: application/json' --data "$(jq -nc --arg corpus_id "$corpus_id" --arg corpus_version "$corpus_version" --arg query "$question" '{corpus_id:$corpus_id,corpus_version:$corpus_version,query:$query,limit:8}')")"
  actual="$(jq -c '[.results[].source.source_id]' <<<"$response")"
  matched="$(jq -n --argjson targets "$targets" --argjson actual "$actual" '[ $targets[] | select(. as $id | $actual | index($id)) ]')"
  citation_coverage="$(jq '[.results[] | select(.source.source_id and .source.source_version and .source.locator)] as $cited | ($cited | length) == (.results | length)' <<<"$response")"
  jq -nc --argjson case "$case_json" --argjson targets "$targets" --argjson actual "$actual" --argjson matched "$matched" --argjson citation_coverage "$citation_coverage" '{id:$case.id,category:$case.category,severity:$case.severity,expected_source_ids:$targets,retrieved_source_ids:$actual,matched_source_ids:$matched,eligible:($targets|length>0),retrieval_hit:($matched|length>0),citation_coverage:$citation_coverage}' >>"$case_file"
  printf '\n' >>"$case_file"
done

jq -s --arg generated_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" --arg corpus_id "$corpus_id" --arg corpus_version "$corpus_version" '
  {generated_at:$generated_at, corpus:{corpus_id:$corpus_id,corpus_version:$corpus_version}, cases:., metrics:{total_cases:length, eligible_cases:([.[]|select(.eligible)]|length), retrieval_hits:([.[]|select(.eligible and .retrieval_hit)]|length), retrieval_recall:(([.[]|select(.eligible and .retrieval_hit)]|length) / ([.[]|select(.eligible)]|length)), citation_coverage:([.[]|select(.citation_coverage)]|length)}}
' "$case_file" >"$report_path"
printf 'Wrote %s\n' "$report_path"
