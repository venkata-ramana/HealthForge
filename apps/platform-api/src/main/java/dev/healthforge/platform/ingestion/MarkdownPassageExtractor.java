package dev.healthforge.platform.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownPassageExtractor {
    public static final String PARSER_VERSION = "markdown-text-v1";
    public static final String CHUNKING_VERSION = "heading-block-v1";

    public List<PdfPassageExtractor.ExtractedPassage> extract(byte[] artifact) {
        var lines = new String(artifact, StandardCharsets.UTF_8).split("\\R");
        var passages = new ArrayList<PdfPassageExtractor.ExtractedPassage>();
        var heading = "Markdown document";
        var buffer = new StringBuilder();
        var ordinal = 1;

        for (var rawLine : lines) {
            var line = rawLine.trim();
            if (line.startsWith("#")) {
                ordinal = flush(passages, heading, buffer, ordinal);
                heading = line.replaceFirst("^#+\\s*", "").trim();
                continue;
            }
            if (line.isBlank()) {
                if (buffer.length() > 1000) {
                    ordinal = flush(passages, heading, buffer, ordinal);
                }
                continue;
            }
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(normalize(line));
        }

        flush(passages, heading, buffer, ordinal);
        if (passages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Markdown source does not contain extractable text");
        }
        return List.copyOf(passages);
    }

    private int flush(
            List<PdfPassageExtractor.ExtractedPassage> passages,
            String locator,
            StringBuilder buffer,
            int ordinal
    ) {
        if (buffer.length() == 0) {
            return ordinal;
        }
        passages.add(new PdfPassageExtractor.ExtractedPassage(ordinal, locator, buffer.toString().trim()));
        buffer.setLength(0);
        return ordinal + 1;
    }

    private String normalize(String value) {
        return value.replaceAll("`", "")
                .replaceAll("\\[(.*?)\\]\\((.*?)\\)", "$1")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
