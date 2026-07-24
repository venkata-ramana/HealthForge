package dev.healthforge.platform.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HtmlPassageExtractor {
    public static final String PARSER_VERSION = "html-text-v1";
    public static final String CHUNKING_VERSION = "heading-block-v1";

    private static final Pattern BLOCK_PATTERN = Pattern.compile("(?is)<(h[1-6]|p|li)[^>]*>(.*?)</\\1>");

    public List<PdfPassageExtractor.ExtractedPassage> extract(byte[] artifact) {
        var html = new String(artifact, StandardCharsets.UTF_8)
                .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        var matcher = BLOCK_PATTERN.matcher(html);
        var passages = new ArrayList<PdfPassageExtractor.ExtractedPassage>();
        var currentHeading = "HTML document";
        var buffer = new StringBuilder();
        var ordinal = 1;

        while (matcher.find()) {
            var tag = matcher.group(1).toLowerCase(Locale.ROOT);
            var text = normalize(matcher.group(2));
            if (text.isBlank()) {
                continue;
            }
            if (tag.startsWith("h")) {
                ordinal = flush(passages, currentHeading, buffer, ordinal);
                currentHeading = text;
                continue;
            }
            if (buffer.length() > 0 && buffer.length() + text.length() > 1400) {
                ordinal = flush(passages, currentHeading, buffer, ordinal);
            }
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(text);
        }
        ordinal = flush(passages, currentHeading, buffer, ordinal);
        if (passages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "HTML source does not contain extractable text");
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
        passages.add(new PdfPassageExtractor.ExtractedPassage(ordinal, locator, buffer.toString()));
        buffer.setLength(0);
        return ordinal + 1;
    }

    private String normalize(String value) {
        return value.replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
