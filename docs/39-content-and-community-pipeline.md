# Content and community pipeline

This pipeline turns shipped HealthForge capabilities into repeatable technical content and community outreach without drifting into hype.

## Principle

Content should follow shipped work, not run ahead of it.

## Publishing cadence

For each significant shipped phase:

1. update the README and release notes
2. choose one technical article angle
3. choose one demo or talk angle
4. capture screenshots or terminal artifacts from the real product
5. publish only claims that map to merged code and reviewed docs

## Suggested recurring content types

- milestone article
- architecture explainer
- operator / evaluator walkthrough
- healthcare interoperability workflow demo
- open-source contributor onboarding post

## Editorial calendar model

| Milestone trigger | Content output | Audience |
| --- | --- | --- |
| New workflow shipped | Technical article | builders and engineers |
| New trust/governance feature shipped | Evaluation or safety walkthrough | architects, evaluators, enterprise stakeholders |
| Major UX or demo improvement | Recorded demo or meetup talk | prospects, open-source community |
| New roadmap segment opened | “what we are building next” update | contributors and followers |

## Source material to reuse

- README examples
- docs architecture diagrams
- web UI screenshots
- evaluation dashboard screenshots
- synthetic FHIR demo outputs
- release notes and merged issue summaries

## Boundary rules for public content

- never imply compliance certification
- never imply PHI readiness
- never present synthetic demo flows as production proof
- favor traceability and caveats over polished but inflated claims

## Reusable templates

- [`docs/templates/article-outline.md`](./templates/article-outline.md)
- [`docs/templates/demo-talk-outline.md`](./templates/demo-talk-outline.md)
