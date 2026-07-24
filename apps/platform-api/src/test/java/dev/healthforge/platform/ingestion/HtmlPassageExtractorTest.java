package dev.healthforge.platform.ingestion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlPassageExtractorTest {

    private final HtmlPassageExtractor extractor = new HtmlPassageExtractor();

    @Test
    void extractsHeadingScopedPassages() {
        var html = """
                <html><body>
                <h1>FHIR R4</h1>
                <p>Release 4 is the normative base for the MVP corpus.</p>
                <p>It defines core REST and resource semantics.</p>
                <h2>PAS</h2>
                <p>PAS supports prior authorization request and response exchange.</p>
                </body></html>
                """;

        var passages = extractor.extract(html.getBytes());

        assertThat(passages).hasSize(2);
        assertThat(passages.get(0).locator()).isEqualTo("FHIR R4");
        assertThat(passages.get(0).normalizedText()).contains("Release 4").contains("core REST");
        assertThat(passages.get(1).locator()).isEqualTo("PAS");
        assertThat(passages.get(1).normalizedText()).contains("prior authorization");
    }
}
