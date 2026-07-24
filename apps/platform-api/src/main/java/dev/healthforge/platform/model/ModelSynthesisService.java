package dev.healthforge.platform.model;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModelSynthesisService {
    private final ModelProperties properties;
    public ModelSynthesisService(ModelProperties properties) { this.properties = properties; }
    public ModelSynthesisResponse synthesize(ModelSynthesisRequest request) {
        var packet = request.evidencePacket();
        if (!"grounded".equals(packet.status()) || packet.findings().isEmpty()) throw rejected("Only grounded evidence packets may enter model orchestration");
        if (packet.findings().stream().anyMatch(f -> f.citation() == null || f.citation().sourceId() == null || f.citation().sourceVersion() == null || f.citation().locator() == null)) throw rejected("Every proposed finding requires a complete citation");
        if (!properties.enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Model provider is disabled by default; no evidence was transmitted");
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "No approved model provider adapter is configured");
    }
    private ResponseStatusException rejected(String reason) { return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason); }
}
