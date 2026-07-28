#!/usr/bin/env bash
set -euo pipefail

baseline_file="${1:-evals/baselines/mvp-retrieval-quality-gate-v2.json}"
reports_dir="${2:-evals/reports}"

command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }

latest_report="$(find "$reports_dir" -maxdepth 1 -type f -name '*.json' | sort | tail -n 1)"
if [[ -z "${latest_report:-}" ]]; then
  echo "No evaluation report found under $reports_dir" >&2
  exit 1
fi

./scripts/check-evaluation-gate.sh "$baseline_file" "$latest_report"
