# First 90 days: discovery-to-MVP plan

## Outcomes by day 90

- A curated and versioned CMS/FHIR/prior-authorization corpus with provenance.
- A testable prototype that produces a cited Regulation-to-Engineering Brief.
- An evaluation set reviewed by at least one qualified domain expert.
- Public architecture and contribution documentation.
- A clear go/no-go decision for expanding into validation and integrations.

## Weeks 1–2 — Domain and customer discovery

- Map the prior-authorization ecosystem: payer, provider, EHR, intermediaries, and patient touchpoints.
- Study FHIR fundamentals plus the selected implementation guides.
- Select the exact source set and capture owner, URL, version, effective date, license, and update cadence.
- Interview 3–5 target users about their last regulatory implementation; collect artifacts only where authorized.
- Write a glossary and a sample “current-state to target-state” workflow.

**Exit criterion:** one narrow use case, source manifest, persona/workflow map, and ten representative user questions.

## Weeks 3–4 — Evidence and evaluation design

- Define the EngineeringBrief schema and citation policy.
- Create 25–50 question cases with expected source passages and reviewer rubric.
- Prototype document parsing, chunking, retrieval, and citation rendering using non-sensitive material.
- Measure retrieval recall and citation correctness before optimizing answer style.
- Record security/data-flow assumptions and model-provider constraints.

**Exit criterion:** a repeatable offline evaluation that exposes unsupported or misleading answers.

## Weeks 5–8 — MVP vertical slice

- Implement the minimal source-ingestion and versioning path.
- Implement brief generation with schema validation, citation checks, and human review states.
- Build the simplest review UI: question, draft, sources, assumptions, approve/correct/reject.
- Generate proposed work items as an exportable artifact, not a direct external write.
- Run usability sessions with target users and revise the output schema.

**Exit criterion:** a user can answer a real scoped question and review evidence without leaving the product.

## Weeks 9–12 — Hardening and public launch preparation

- Expand and score the evaluation set; document known limitations.
- Add observability for retrieval, generation failures, citation coverage, and review outcomes.
- Write contribution, responsible-AI, security disclosure, and data-handling policies.
- Publish the architecture, sample corpus manifests, and a non-sensitive demo.
- Decide the next module based on feedback: FHIR validator is the likely highest-confidence follow-on.

**Exit criterion:** open-source MVP with reproducible setup, evaluated demo cases, and a credible public roadmap.

## Metrics that matter initially

| Metric | Why it matters |
| --- | --- |
| Citation precision and coverage | Trustworthiness of technical claims |
| Retrieval recall on evaluation questions | Whether required evidence can be found |
| Reviewer acceptance/correction rate | Practical usefulness and failure modes |
| Time from question to approved brief | Workflow improvement |
| Corpus freshness/version coverage | Accuracy over time |

Stars and contributors are useful community signals, but not substitutes for evidence quality and user value.
