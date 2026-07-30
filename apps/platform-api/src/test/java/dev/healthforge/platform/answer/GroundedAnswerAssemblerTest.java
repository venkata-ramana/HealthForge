package dev.healthforge.platform.answer;

import dev.healthforge.platform.retrieval.RetrievalResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroundedAnswerAssemblerTest {

    private final GroundedAnswerAssembler assembler = new GroundedAnswerAssembler();
    private final GroundedAnswerRequest request = new GroundedAnswerRequest(
            "mvp-regulatory-corpus", "local", "What does the rule say about prior authorization?", null, List.of()
    );

    @Test
    void returnsCitedEvidenceFindingsWhenRetrievalHasResults() {
        var response = assembler.assemble(request, new RetrievalResponse(
                "mvp-regulatory-corpus", "local", "postgres-fts-v1", List.of(
                        new RetrievalResponse.RetrievalResult(
                                "passage_123", "Prior authorization requests must use the API.", 1, 1.0,
                                new RetrievalResponse.CiteableSource(
                                        "cms-0057-f-final-rule", "2024-final", "governing_regulation",
                                        "CMS Final Rule", "https://www.cms.gov/example.pdf", "Page 586"
                                )
                        )
                )
        ), Instant.parse("2026-07-24T12:00:00Z"));

        assertThat(response.status()).isEqualTo("grounded");
        assertThat(response.answer()).contains("retrieved source excerpts");
        assertThat(response.diagnostics()).isNotNull();
        assertThat(response.diagnostics().sufficiency()).isEqualTo("supported");
        assertThat(response.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.statement()).isEqualTo("Prior authorization requests must use the API.");
            assertThat(finding.citation().sourceId()).isEqualTo("cms-0057-f-final-rule");
            assertThat(finding.citation().sourceVersion()).isEqualTo("2024-final");
            assertThat(finding.citation().locator()).isEqualTo("Page 586");
        });
    }

    @Test
    void refusesToProduceAnAnswerWhenRetrievalHasNoEvidence() {
        var response = assembler.assemble(request, new RetrievalResponse(
                "mvp-regulatory-corpus", "local", "postgres-fts-v1", List.of()
        ), Instant.parse("2026-07-24T12:00:00Z"));

        assertThat(response.status()).isEqualTo("insufficient_evidence");
        assertThat(response.answer()).isNull();
        assertThat(response.findings()).isEmpty();
        assertThat(response.limitations()).singleElement().asString().contains("No citeable passage");
        assertThat(response.diagnostics()).isNotNull();
        assertThat(response.diagnostics().queryRefinements()).isNotEmpty();
    }
}
