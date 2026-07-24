# Retrieval evaluation runbook

Run the local service and create a pinned corpus snapshot first. Then execute:

```bash
scripts/evaluate-retrieval.sh mvp-regulatory-corpus 2026-07-24
```

The command runs every public/synthetic case in the MVP dataset, compares retrieved source IDs with expected targets, checks citation field coverage, and writes a timestamped JSON report under `evals/reports/`. It deliberately reports misses for sources not yet present in the selected snapshot; do not treat a missing source as a passing result.

Preserve reports when modifying source ingestion, chunking, retrieval configuration, or prompts. A qualified reviewer must inspect failures before changing the baseline.
