# Phase 9 regression operations

Phase 9 keeps the existing retrieval evaluation scripts and adds a clearer way to summarize the current trust state.

## Commands

Run a retrieval evaluation:

```bash
scripts/evaluate-retrieval.sh mvp-regulatory-corpus 2026-07-24-expanded-web-core-v4
```

Check the approved gate:

```bash
scripts/check-evaluation-gate.sh \
  evals/baselines/mvp-retrieval-quality-gate-v2.json \
  evals/reports/2026-07-24-expanded-web-core-v4-unsupported-guard.json
```

Summarize the current baseline and latest report:

```bash
scripts/summarize-evaluation-state.sh
```

## What operators should inspect

- retrieval recall drift
- unsupported-boundary pass rate
- highlighted failed cases from the latest report
- answer telemetry showing insufficient-evidence or unsupported-triggered patterns
- reviewer disagreement and approval/change-request mix
