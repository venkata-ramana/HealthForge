package dev.healthforge.platform.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SourcePolicyValidator {

    private final MvpSourceProperties sourceProperties;

    public SourcePolicyValidator(MvpSourceProperties sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    public void validate(IngestionRequest request) {
        var sourcePolicy = sourceProperties.sources().get(request.manifestSourceId());
        if (sourcePolicy == null) {
            throw forbidden("Source is not approved by the MVP manifest");
        }
        if (!sourcePolicy.canonicalUrl().equals(request.canonicalUrl())) {
            throw forbidden("Canonical URL does not match the approved source");
        }
        if (!sourcePolicy.contentTypes().contains(request.expectedContentType())) {
            throw forbidden("Content type is not approved for this source");
        }
    }

    private ResponseStatusException forbidden(String reason) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
    }
}
