package dev.healthforge.platform.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class SourceArtifactFetcher {

    private static final int MAX_ARTIFACT_BYTES = 50 * 1024 * 1024;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public SourceArtifact fetch(IngestionRequest request) {
        try {
            var requestedUri = URI.create(request.canonicalUrl());
            var response = httpClient.send(
                    HttpRequest.newBuilder(requestedUri).timeout(Duration.ofSeconds(60)).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw badRequest("Approved source returned HTTP " + response.statusCode());
            }
            if (!requestedUri.getHost().equalsIgnoreCase(response.uri().getHost())) {
                throw badRequest("Approved source redirected to an unapproved host");
            }
            var contentType = response.headers().firstValue("Content-Type").orElse("").split(";", 2)[0];
            if (!request.expectedContentType().equalsIgnoreCase(contentType)) {
                throw badRequest("Fetched artifact content type does not match the approved source");
            }
            try (var body = response.body()) {
                return new SourceArtifact(readAtMost(body), contentType, response.uri());
            }
        } catch (IOException exception) {
            throw badRequest("Could not fetch the approved source");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw badRequest("Fetching the approved source was interrupted");
        }
    }

    private byte[] readAtMost(InputStream inputStream) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            var buffer = new byte[8192];
            var totalBytes = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > MAX_ARTIFACT_BYTES) {
                    throw badRequest("Approved source exceeds the local ingestion size limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }
}
