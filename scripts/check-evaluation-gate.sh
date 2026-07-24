#!/usr/bin/env bash
set -euo pipefail

baseline_file="${1:?usage: check-evaluation-gate.sh BASELINE_FILE CANDIDATE_REPORT}"
candidate_report="${2:?usage: check-evaluation-gate.sh BASELINE_FILE CANDIDATE_REPORT}"

command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }

jq -e '.' "$baseline_file" >/dev/null
jq -e '.' "$candidate_report" >/dev/null

jq -n \
  --slurpfile baseline "$baseline_file" \
  --slurpfile candidate "$candidate_report" \
  --arg candidate_report "$candidate_report" '
  def baseline_doc: $baseline[0];
  def candidate_doc: $candidate[0];
  def metric($name): candidate_doc.metrics[$name];
  def baseline_metric($name): baseline_doc.approved_metrics[$name];
  def min_metric($name): baseline_doc.minimum_metrics[$name];
  def regression_drop($name): baseline_metric($name) - metric($name);
  def material_threshold($name): baseline_doc.material_regression_thresholds[$name];
  def has_material_regression($name): regression_drop($name) > material_threshold($name);
  {
    gate_id: baseline_doc.gate_id,
    baseline_report: baseline_doc.approved_baseline_report,
    candidate_report: $candidate_report,
    comparisons: {
      retrieval_recall: {
        baseline: baseline_metric("retrieval_recall"),
        candidate: metric("retrieval_recall"),
        minimum: min_metric("retrieval_recall"),
        material_regression: has_material_regression("retrieval_recall")
      },
      citation_coverage_rate: {
        baseline: baseline_metric("citation_coverage_rate"),
        candidate: metric("citation_coverage_rate"),
        minimum: min_metric("citation_coverage_rate"),
        material_regression: has_material_regression("citation_coverage_rate")
      },
      unsupported_answer_pass_rate: {
        baseline: baseline_metric("unsupported_answer_pass_rate"),
        candidate: metric("unsupported_answer_pass_rate"),
        minimum: min_metric("unsupported_answer_pass_rate"),
        material_regression: has_material_regression("unsupported_answer_pass_rate")
      }
    },
    known_failures: [
      candidate_doc.cases[]
      | select(
          (.eligible and (.retrieval_hit | not))
          or (.category == "unsupported" and .unsupported_boundary_pass == false)
        )
      | {
          id,
          category,
          severity,
          retrieval_hit,
          answer_status,
          unsupported_boundary_pass
        }
    ]
  }
  | .decision =
      (if (
        (.comparisons.retrieval_recall.candidate < .comparisons.retrieval_recall.minimum)
        or (.comparisons.citation_coverage_rate.candidate < .comparisons.citation_coverage_rate.minimum)
        or (.comparisons.unsupported_answer_pass_rate.candidate < .comparisons.unsupported_answer_pass_rate.minimum)
      ) then "blocked"
      elif (
        .comparisons.retrieval_recall.material_regression
        or .comparisons.citation_coverage_rate.material_regression
        or .comparisons.unsupported_answer_pass_rate.material_regression
      ) then "human_review_required"
      else "pass"
      end)
  ' > /tmp/healthforge-gate-result.json

cat /tmp/healthforge-gate-result.json

decision="$(jq -r '.decision' /tmp/healthforge-gate-result.json)"
if [[ "$decision" == "pass" ]]; then
  exit 0
fi
if [[ "$decision" == "human_review_required" ]]; then
  exit 2
fi
exit 1
