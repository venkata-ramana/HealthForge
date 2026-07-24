# MVP source corpus and support boundaries

## Purpose

This is the initial, deliberately small knowledge boundary for the HealthForge Regulation-to-Engineering Brief. It supports engineering analysis of **CMS-0057-F electronic prior authorization for non-drug medical items and services**, not a general-purpose compliance assistant.

The authoritative, machine-readable inventory is [`knowledge/manifests/mvp-source-corpus.yaml`](../knowledge/manifests/mvp-source-corpus.yaml). A source is not eligible for retrieval until its downloaded artifact, checksum, retrieval timestamp, and license/terms review are recorded in the ingestion metadata.

## Supported questions

HealthForge may help an engineering team investigate questions such as:

- Which CMS-0057-F prior-authorization API capability appears relevant to this scoped use case?
- What source passages explain the requirement, effective context, or CMS implementation guidance?
- Which FHIR R4 and Da Vinci PAS/CRD/DTR artifacts are candidate technical references?
- What assumptions, implementation questions, and backlog items should a human reviewer consider?

The product must cite the governing CMS source and distinguish it from implementation guidance. Da Vinci implementation guides are technical references, not a substitute for regulatory text.

## In scope

| Area | Pinned support boundary |
| --- | --- |
| Regulation | CMS Interoperability and Prior Authorization Final Rule, CMS-0057-F (2024 final rule) |
| Workflow | Prior Authorization API for non-drug medical items and services; requirement/discovery, documentation exchange, submission, and response/status discussion |
| Base standard | HL7 FHIR R4, version 4.0.1 |
| Technical guidance | Published Da Vinci PAS 2.1.0, CRD 2.1.0, and DTR 2.1.0 artifacts |
| CMS implementation context | CMS-0057-F FAQs, API/standards guidance, and the Prior Authorization API workflow aid, all versioned at ingestion |
| Data classification | Public, non-sensitive source material and synthetic examples only |

CMS explains that its recommended implementation-guide versions are directional guidance rather than a blanket mandate. HealthForge therefore presents PAS, CRD, and DTR mappings as **candidate implementation guidance**, with an explicit human applicability review.

## Explicitly out of scope

- Legal advice, certification, or a compliance determination.
- Drug prior authorization and any future rulemaking that expands that domain.
- Regulations other than CMS-0057-F, except where a source is included only as background and is labelled as such.
- FHIR R5/R6, a continuous-build implementation guide, or an unpinned package version.
- Live payer policies, provider contracts, proprietary implementation guides, and unverified web content.
- PHI, production payloads, customer documents, or any source requiring credentials in the MVP.
- Automatic submission to a payer, automatic Jira/GitHub writeback, or production-code generation.

## Corpus admission and update policy

1. A maintainer proposes a source change through a pull request against the manifest.
2. The reviewer verifies publisher/owner, canonical URL, document or package version, effective/publication dates, and allowed use.
3. Ingestion stores the original artifact immutably with retrieval timestamp and SHA-256 checksum; passages retain section/page locators.
4. A corpus version is published only after retrieval and citation evaluation cases have been run.
5. Superseded or withdrawn material remains available only for historical reproducibility and is visibly marked as such; it cannot silently replace an active source.

## Review cadence and ownership

| Source class | Owner | Cadence | Trigger for an early review |
| --- | --- | --- | --- |
| CMS final rule | HealthForge domain maintainer | Quarterly | CMS correction, FAQ, implementation update, or new rulemaking |
| CMS FAQs/workflow/standards guidance | HealthForge domain maintainer | Monthly | CMS publication or changed guidance date |
| HL7 FHIR and published Da Vinci IGs | HealthForge standards maintainer | Quarterly | New published version, package withdrawal, or reported compatibility issue |

At every review, record whether the source is unchanged, newly published, superseded, withdrawn, or requires a new evaluation baseline.

## Definition of ready for the MVP

The first corpus release is ready only when every active entry has a retrieved artifact, SHA-256 checksum, stable citation locator strategy, allowed-use review, and at least one evaluation question demonstrating a correct citation. The manifest is a source-selection decision; it is not itself an ingested corpus.
