# Retrieval evaluation runbook

Run the local service and create a pinned corpus snapshot first. Then execute:

```bash
scripts/evaluate-retrieval.sh mvp-regulatory-corpus 2026-07-24
```

The command runs every public/synthetic case in the MVP dataset, compares retrieved source IDs with expected targets, checks citation field coverage, evaluates unsupported-answer handling through `POST /v1/answers`, and writes a timestamped JSON report under `evals/reports/`. It deliberately reports misses for sources not yet present in the selected snapshot; do not treat a missing source as a passing result.

Preserve reports when modifying source ingestion, chunking, retrieval configuration, or prompts. A qualified reviewer must inspect failures before changing the baseline.

To compare a candidate run against the approved non-regression baseline, execute:

```bash
scripts/check-evaluation-gate.sh \
  evals/baselines/mvp-retrieval-quality-gate-v2.json \
  evals/reports/2026-07-24-expanded-web-core-v4-unsupported-guard.json
```

The gate returns:

- exit code `0` when the candidate passes the approved baseline thresholds;
- exit code `2` when material regression requires human review; and
- exit code `1` when the candidate falls below the pinned minimum metrics.

For a quick operator-readable summary of the current baseline and the latest report, run:

```bash
scripts/summarize-evaluation-state.sh
```
