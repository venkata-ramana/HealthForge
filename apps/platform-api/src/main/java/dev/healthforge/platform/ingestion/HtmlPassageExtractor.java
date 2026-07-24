package dev.healthforge.platform.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class HtmlPassageExtractor {
    public static final String PARSER_VERSION = "html-text-v1";
    public static final String CHUNKING_VERSION = "document-v1";
    public List<PdfPassageExtractor.ExtractedPassage> extract(byte[] artifact) {
        var html = new String(artifact, StandardCharsets.UTF_8);
        var text = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?is)<[^>]+>", " ").replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&").replaceAll("\\s+", " ").trim();
        if (text.isBlank()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "HTML source does not contain extractable text");
        return List.of(new PdfPassageExtractor.ExtractedPassage(1, "HTML document", text));
    }
}
