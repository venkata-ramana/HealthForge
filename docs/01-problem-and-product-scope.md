# Problem framing and initial product scope

## The actual job to be done

A healthcare engineering team needs to turn an externally published requirement into a safe, reviewable change to a real product. Today this work is split across regulation text, CMS technical materials, HL7 FHIR implementation guides, internal architecture documents, issue trackers, and code repositories.

The difficult part is not merely finding an answer. It is producing an answer that is:

- grounded in authoritative sources;
- specific to an engineering role and system capability;
- explicit about uncertainty and applicability;
- traceable back to source passages and versions; and
- reviewable by domain, security, product, and engineering owners.

HealthForge should therefore be a **decision-support and engineering-workflow platform**, not a general healthcare chatbot or an autonomous compliance authority.

## Primary users

| User | Primary need | First-release outcome |
| --- | --- | --- |
| Product/technical lead | Understand what a rule changes | Cited technical-impact brief |
| Solution architect | Identify capabilities and integrations affected | Proposed system/workflow changes |
| Backend/FHIR engineer | Map requirements to standards and implementation work | FHIR/IG references and implementation tasks |
| QA engineer | Derive testable acceptance criteria | Test scenarios and validation examples |
| Compliance reviewer | Review rationale and retain evidence | Decision record with citations and approvals |

## First vertical slice: Regulation-to-Engineering Brief

**Input:** a curated, versioned authoritative document set and a bounded question (for example, “What changes does this rule require for our prior-authorization API?”).

**Output:** a structured brief containing:

1. plain-language summary;
2. applicability and assumptions;
3. source citations at passage level;
4. impacted business capabilities and personas;
5. relevant FHIR resources, implementation guides, and workflow touchpoints;
6. proposed architecture/API changes;
7. backlog-ready work items and acceptance criteria; and
8. human-review status and feedback.

**Non-goals for the first slice:** legal advice, a certification/compliance verdict, automatic production-code changes, unreviewed Jira/GitHub writes, processing PHI, or support for every regulation/IG.

## Narrow initial domain

Start with **CMS interoperability and electronic prior authorization**. It has a clear technical surface area (FHIR, payer/provider workflows, PAS/CRD/DTR) and aligns with the roadmap. Begin with a deliberately curated corpus rather than open-web retrieval.

## Product principles that become requirements

- **Traceability over fluency:** every material claim requires source support.
- **Human approval is a control:** generated recommendations are drafts, not determinations.
- **Versioned knowledge:** answers identify the document/IG version and retrieval date.
- **Bounded context:** tenant and project context are isolated; no PHI is needed for the MVP.
- **Interoperability-first:** model standards and workflows explicitly rather than embedding them in prompts.
- **Evaluation before expansion:** introduce a capability only after measurable groundedness and usability checks.

## Critical questions to answer during discovery

1. Which exact CMS rules and implementation-guide versions will the MVP support?
2. Who is the design partner, and what is their current regulation-to-backlog workflow?
3. What source licensing, redistribution, and update rules apply to each corpus?
4. What level of citation completeness is acceptable for a technical-impact brief?
5. Which integrations belong in the MVP: none, export only, or direct issue-tracker writeback?
6. What deployment boundary is required before any customer material is processed?
