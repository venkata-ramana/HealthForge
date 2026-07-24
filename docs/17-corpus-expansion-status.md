# Corpus expansion status

The `2026-07-24-cms-core` snapshot contains two immutable public CMS PDFs: CMS-0057-F and the CMS Prior Authorization API Workflow aid. Their canonical URLs, versions, and SHA-256 checksums are published in [`knowledge/snapshots/cms-core-2026-07-24.yaml`](../knowledge/snapshots/cms-core-2026-07-24.yaml).

The `2026-07-24-expanded-web-core` snapshot adds published HL7 FHIR R4 plus Da Vinci PAS, CRD, and DTR HTML sources. Its inventory and checksums are published in [`knowledge/snapshots/expanded-web-core-2026-07-24.yaml`](../knowledge/snapshots/expanded-web-core-2026-07-24.yaml).

The `2026-07-24-expanded-web-core-v3` snapshot adds the CMS Interoperability FAQ page to that broader public web corpus. Its inventory and current evaluation metrics are published in [`knowledge/snapshots/expanded-web-core-v3-2026-07-24.yaml`](../knowledge/snapshots/expanded-web-core-v3-2026-07-24.yaml).

The retrieval evaluation baseline remains intentionally partial for exact expected-source recall. After retrieval tuning plus the broader public web corpus, recall improved to 10/24 eligible cases, but adding the CMS FAQ page alone did not move that metric further. Package downloads remain a separate controlled path because they require package integrity and dependency validation.
