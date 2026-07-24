# Corpus expansion status

The `2026-07-24-cms-core` snapshot contains two immutable public CMS PDFs: CMS-0057-F and the CMS Prior Authorization API Workflow aid. Their canonical URLs, versions, and SHA-256 checksums are published in [`knowledge/snapshots/cms-core-2026-07-24.yaml`](../knowledge/snapshots/cms-core-2026-07-24.yaml).

The retrieval evaluation baseline remains intentionally failing for exact expected-source recall. That result is retained because the current PDF page chunking and two-source coverage do not yet satisfy the broader 26-case dataset. The next corpus-expansion increment must add a dedicated pinned-package/HTML ingestion path for FHIR and Da Vinci guidance; do not ingest those sources through the PDF-only pipeline.
