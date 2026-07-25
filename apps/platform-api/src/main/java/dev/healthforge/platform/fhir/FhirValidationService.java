package dev.healthforge.platform.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;
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

import java.util.List;

@Service
public class FhirValidationService {

    private final FhirValidationCatalog catalog;
    private final ObjectMapper objectMapper;
    private final FhirValidator validator;

    public FhirValidationService(FhirValidationCatalog catalog, ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.objectMapper = objectMapper;

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

    public FhirValidationResponse validate(FhirValidationRequest request) {
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

        return new FhirValidationResponse(
                result.isSuccessful() ? "valid" : "invalid",
                new FhirValidationResponse.PackageSelection(
                        profile.packageId(),
                        profile.packageVersion(),
                        profile.profileUrl(),
                        profile.profileTitle()
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
