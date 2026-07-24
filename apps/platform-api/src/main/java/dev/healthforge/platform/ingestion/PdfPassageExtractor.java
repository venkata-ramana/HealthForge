package dev.healthforge.platform.ingestion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfPassageExtractor {

    public static final String PARSER_VERSION = "pdfbox-3.0.8";
    public static final String CHUNKING_VERSION = "page-v1";

    public List<ExtractedPassage> extract(byte[] artifact) {
        try (var document = Loader.loadPDF(artifact)) {
            var passages = new ArrayList<ExtractedPassage>();
            for (var page = 1; page <= document.getNumberOfPages(); page++) {
                var stripper = new PDFTextStripper();
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                var normalizedText = normalize(stripper.getText(document));
                if (!normalizedText.isBlank()) {
                    passages.add(new ExtractedPassage(page, "Page " + page, normalizedText));
                }
            }
            if (passages.isEmpty()) {
                throw invalidPdf("PDF does not contain extractable text");
            }
            return List.copyOf(passages);
        } catch (IOException exception) {
            throw invalidPdf("Approved source could not be parsed as a PDF");
        }
    }

    private String normalize(String value) {
        return value.replace("\u0000", "").replaceAll("[\\t\\x0B\\f\\r ]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private ResponseStatusException invalidPdf(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }

    public record ExtractedPassage(int ordinal, String locator, String normalizedText) {
    }
}
