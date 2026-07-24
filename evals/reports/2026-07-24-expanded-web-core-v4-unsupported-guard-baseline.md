# Evaluation baseline report

## Run metadata

| Field | Value |
| --- | --- |
| Report ID | `eval-2026-07-24-expanded-web-core-v4-unsupported-guard` |
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
| Unsupported-answer handling | 1.00 | ≥ 1.00 | yes |
| Human review required on material regression | yes | always | policy |

## Known failures

- Retrieval still misses several eligible cases even with the expanded v4 corpus; see the JSON report for exact case IDs.
- Unsupported and safety-boundary prompts now return `insufficient_evidence` before a grounded answer is assembled.
- This baseline improves boundary handling without yet increasing retrieval recall for the remaining eligible misses.

## Gate decision

- [x] Passed for the evaluated corpus/configuration as the approved non-regression baseline.
- [ ] Blocked pending correction and rerun.
- [ ] Not comparable to the previous baseline; explain why.
