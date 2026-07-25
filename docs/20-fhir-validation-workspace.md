# FHIR validation workspace

The Phase 2 prototype adds a local `POST /v1/fhir-validation/validate` endpoint for non-sensitive FHIR examples. Phase 3 expands that into a pinned package workflow with a discoverable catalog at `GET /v1/fhir-validation/catalog`. The workflow remains intentionally narrow:

- accepts only examples explicitly labeled `synthetic` or `non_sensitive`;
- validates only against a pinned catalog of package/profile selections;
- returns machine-readable validation findings with evidence links; and
- marks every result as `human_review_required` before any implementation recommendation.

The first actively supported package is `hl7.fhir.r4.core|4.0.1`, using a starter set of base R4 profiles that matter to the MVP architecture discussion:

- `Patient`
- `Observation`
- `Claim`
- `ClaimResponse`
- `Coverage`
- `Bundle`

The catalog may also contain known implementation-guide packages in a `planned` state. That means HealthForge knows the package metadata and profile inventory, but deterministic validation support is not yet enabled. This makes the package boundary visible without pretending support exists before the pinned artifacts and review workflow are ready.

This workspace is for deterministic conformance checking, not for proving payer-specific interoperability support. A passing validation result means the example conforms to the selected pinned profile as checked by the local validator; it does not replace implementation review, terminology review, integration testing, or regulatory interpretation.

Pinned package support is governed as follows:

- package metadata and profile inventory must be explicitly reviewed before entry into the catalog;
- implementation-guide packages may be cataloged before validation support is enabled;
- only entries marked `supported` may be used for deterministic validation; and
- unsupported or planned entries fail with a reviewable error instead of silently falling back.

Example request:

```json
{
  "package_id": "hl7.fhir.r4.core",
  "package_version": "4.0.1",
  "profile_url": "http://hl7.org/fhir/StructureDefinition/Observation",
  "data_classification": "synthetic",
  "resource": {
    "resourceType": "Observation",
    "code": {
      "coding": [
        {
          "system": "http://loinc.org",
          "code": "12345-6"
        }
      ]
    }
  }
}
```

This example is expected to fail because `Observation.status` is required by the base R4 profile. The response returns structured findings and official HL7 evidence links so a reviewer can inspect the rule source directly.
