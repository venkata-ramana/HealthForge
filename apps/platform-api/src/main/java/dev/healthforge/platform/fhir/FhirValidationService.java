package dev.healthforge.platform.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import dev.healthforge.platform.auth.AuthenticatedActor;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FhirValidationService {

    private final FhirValidationCatalog catalog;
    private final ObjectMapper objectMapper;
    private final FhirValidator validator;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public FhirValidationService(FhirValidationCatalog catalog, ObjectMapper objectMapper) {
        this(catalog, objectMapper, null);
    }

    @Autowired
    public FhirValidationService(FhirValidationCatalog catalog, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.catalog = catalog;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;

        var context = FhirContext.forR4();
        var validationSupportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(context),
                new CommonCodeSystemsTerminologyService(context),
                new InMemoryTerminologyServerValidationSupport(context),
                new SnapshotGeneratingValidationSupport(context)
        );
        var instanceValidator = new FhirInstanceValidator(validationSupportChain);
        instanceValidator.setBestPracticeWarningLevel(BestPracticeWarningLevel.Warning);

        this.validator = context.newValidator();
        this.validator.registerValidatorModule(instanceValidator);
    }

    public FhirValidationCatalogResponse catalog() {
        return catalog.catalog();
    }

    public FhirValidationResponse validate(FhirValidationRequest request, AuthenticatedActor actor) {
        var profile = catalog.resolve(request.packageId(), request.packageVersion(), request.profileUrl());
        var serializedResource = serialize(request);

        ValidationResult result;
        try {
            result = validator.validateWithResult(
                    serializedResource,
                    new ValidationOptions().addProfile(profile.profileUrl())
            );
        } catch (DataFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "FHIR resource payload could not be parsed");
        }

        var findings = result.getMessages().stream()
                .map(message -> toFinding(message, profile))
                .toList();

        if (actor != null && jdbcTemplate != null) {
            jdbcTemplate.update("""
                    insert into fhir_validation_run (
                        validation_run_id, organization_id, actor_id, actor_role, package_id, package_version,
                        profile_url, data_classification, validation_status, finding_count, created_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "validation_" + UUID.randomUUID(),
                    actor.organizationId(),
                    actor.actorId(),
                    actor.role().name().toLowerCase(Locale.ROOT),
                    profile.packageId(),
                    profile.packageVersion(),
                    profile.profileUrl(),
                    request.dataClassification(),
                    result.isSuccessful() ? "valid" : "invalid",
                    findings.size(),
                    Timestamp.from(Instant.now(clock))
            );
        }

        return new FhirValidationResponse(
                result.isSuccessful() ? "valid" : "invalid",
                new FhirValidationResponse.PackageSelection(
                        profile.packageId(),
                        profile.packageVersion(),
                        profile.packageTitle(),
                        profile.packageKind(),
                        profile.profileUrl(),
                        profile.profileTitle(),
                        profile.validationBoundary(),
                        profile.validationScope()
                ),
                request.dataClassification(),
                true,
                "human_review_required",
                result.isSuccessful()
                        ? "FHIR example conforms to the selected pinned profile. A qualified human must still review any implementation action."
                        : "FHIR example has validation findings. Treat the result as review evidence only; implementation recommendations require human review.",
                findings,
                List.of(profile.packageEvidenceLink(), profile.profileEvidenceLink())
        );
    }

    public FhirValidationResponse validate(FhirValidationRequest request) {
        return validate(request, null);
    }

    private String serialize(FhirValidationRequest request) {
        try {
            return objectMapper.writeValueAsString(request.resource());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FHIR resource payload could not be serialized");
        }
    }

    private FhirValidationResponse.ValidationFinding toFinding(
            SingleValidationMessage message,
            FhirValidationCatalog.PinnedProfile profile
    ) {
        var location = message.getLocationString();
        return new FhirValidationResponse.ValidationFinding(
                message.getSeverity() == null ? "unknown" : message.getSeverity().name().toLowerCase(),
                location == null || location.isBlank() ? "/" : location,
                message.getMessage(),
                message.getMessageId(),
                List.of(profile.profileEvidenceLink(), profile.packageEvidenceLink())
        );
    }
}
