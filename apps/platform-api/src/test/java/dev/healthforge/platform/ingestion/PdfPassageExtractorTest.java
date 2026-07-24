package dev.healthforge.platform.ingestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfPassageExtractorTest {

    @Test
    void extractsOneCiteablePassagePerPage() throws Exception {
        var extractor = new PdfPassageExtractor();
        var passages = extractor.extract(twoPagePdf());

        assertThat(passages).hasSize(2);
        assertThat(passages.get(0)).extracting(PdfPassageExtractor.ExtractedPassage::locator, PdfPassageExtractor.ExtractedPassage::normalizedText)
                .containsExactly("Page 1", "First source passage");
        assertThat(passages.get(1)).extracting(PdfPassageExtractor.ExtractedPassage::locator, PdfPassageExtractor.ExtractedPassage::normalizedText)
                .containsExactly("Page 2", "Second source passage");
    }

    private byte[] twoPagePdf() throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            addPage(document, "First source passage");
            addPage(document, "Second source passage");
            document.save(output);
            return output.toByteArray();
        }
    }

    private void addPage(PDDocument document, String text) throws Exception {
        var page = new PDPage();
        document.addPage(page);
        try (var content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.newLineAtOffset(72, 720);
            content.showText(text);
            content.endText();
        }
    }
}
