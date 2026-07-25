# FHIR validation workspace

The Phase 2 prototype adds a local `POST /v1/fhir-validation/validate` endpoint for non-sensitive FHIR examples. It is intentionally narrow:

- accepts only examples explicitly labeled `synthetic` or `non_sensitive`;
- validates only against a pinned catalog of supported package/profile selections;
- returns machine-readable validation findings with evidence links; and
- marks every result as `human_review_required` before any implementation recommendation.

The first pinned package is `hl7.fhir.r4.core|4.0.1`, using a small starter set of base R4 profiles that matter to the MVP architecture discussion:

- `Patient`
- `Observation`
- `Claim`
- `ClaimResponse`
- `Coverage`
- `Bundle`

This workspace is for deterministic conformance checking, not for proving payer-specific interoperability support. A passing validation result means the example conforms to the selected pinned profile as checked by the local validator; it does not replace implementation review, terminology review, integration testing, or regulatory interpretation.

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
