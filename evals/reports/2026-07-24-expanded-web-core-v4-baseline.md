# Evaluation baseline report

## Run metadata

| Field | Value |
| --- | --- |
| Report ID | `eval-2026-07-24-expanded-web-core-v4` |
| Evaluator(s) | Codex local evaluation run |
| Corpus ID/version | `mvp-regulatory-corpus` / `2026-07-24-expanded-web-core-v4` |
| Source artifact checksums | Published in [`knowledge/snapshots/expanded-web-core-v4-2026-07-24.yaml`](../../knowledge/snapshots/expanded-web-core-v4-2026-07-24.yaml) |
| Retrieval configuration | `postgres-fts-v1` |
| Model configuration | not applicable |
| Prompt template version | not applicable |
| Dataset version | [`evals/datasets/cms-0057-f-mvp-evaluation-cases.json`](../datasets/cms-0057-f-mvp-evaluation-cases.json) |
| Run date/time | 2026-07-24 UTC |

## Aggregate results

| Metric | Result | Gate | Pass? |
| --- | ---: | ---: | --- |
| Eligible retrieval recall | 0.4167 | ≥ 0.4167 | yes |
| Citation coverage | 1.00 | 1.00 | yes |
| Unsupported-answer handling | 0.00 | ≥ 0.00 | yes (known failure preserved) |
| Human review required on material regression | yes | always | policy |

## Known failures

- Retrieval still misses several eligible cases even with the expanded v4 corpus; see the JSON report for exact case IDs.
- Unsupported/boundary cases `eval_020` through `eval_023` currently return `grounded` instead of `insufficient_evidence`.
- This baseline is approved only as a non-regression checkpoint, not as a production-readiness threshold.

## Gate decision

- [x] Passed for the evaluated corpus/configuration as the approved non-regression baseline.
- [ ] Blocked pending correction and rerun.
- [ ] Not comparable to the previous baseline; explain why.
