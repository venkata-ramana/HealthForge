package dev.healthforge.platform.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class SourcePolicyValidatorTest {

    private final SourcePolicyValidator validator = new SourcePolicyValidator(new MvpSourceProperties(Map.of(
            "cms-0057-f-final-rule",
            new MvpSourceProperties.SourcePolicy(
                    "https://www.cms.gov/files/document/cms-0057-f.pdf",
                    "CMS Interoperability and Prior Authorization Final Rule (CMS-0057-F)",
                    "governing_regulation",
                    Set.of("application/pdf")
            )
    )));

    @Test
    void acceptsAnApprovedSourceRequest() {
        assertThatCode(() -> validator.validate(new IngestionRequest(
                "cms-0057-f-final-rule",
                "2024-final",
                "https://www.cms.gov/files/document/cms-0057-f.pdf",
                "application/pdf",
                "local-developer"
        ))).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnapprovedSourceUrl() {
        assertThatThrownBy(() -> validator.validate(new IngestionRequest(
                "cms-0057-f-final-rule",
                "2024-final",
                "https://example.test/unapproved.pdf",
                "application/pdf",
                "local-developer"
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Canonical URL does not match");
    }
}
