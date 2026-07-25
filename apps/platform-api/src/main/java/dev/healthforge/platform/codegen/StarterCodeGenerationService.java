package dev.healthforge.platform.codegen;

import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class StarterCodeGenerationService {

    private final BriefService briefService;
    private final Clock clock = Clock.systemUTC();

    public StarterCodeGenerationService(BriefService briefService) {
        this.briefService = briefService;
    }

    public StarterCodeGenerationResponse generate(StarterCodeGenerationRequest request) {
        var export = briefService.exportWorkItems(request.briefId());
        var workItem = export.workItems().stream()
                .filter(item -> item.workItemId().equals(request.workItemId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approved work item was not found in the export artifact"));

        var fileName = switch (request.artifactType()) {
            case "spring_boot_endpoint_stub" -> sanitizeName(workItem.title()) + "Controller.java";
            case "spring_service_stub" -> sanitizeName(workItem.title()) + "Service.java";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported starter artifact type");
        };

        var code = switch (request.artifactType()) {
            case "spring_boot_endpoint_stub" -> endpointStub(export, workItem);
            case "spring_service_stub" -> serviceStub(export, workItem);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported starter artifact type");
        };

        return new StarterCodeGenerationResponse(
                "codegen_" + UUID.randomUUID(),
                "example_only",
                Instant.now(clock),
                request.artifactType(),
                fileName,
                "text/x-java-source",
                code,
                "Example starter code only. Not production-ready. Human engineering, security, architecture, and interoperability review remain required before use.",
                new StarterCodeGenerationResponse.Traceability(
                        export.briefId(),
                        workItem.workItemId(),
                        workItem.relatedFindingIds(),
                        workItem.standardsTouchpoints(),
                        workItem.humanReviewStatus(),
                        workItem.validationNotes()
                )
        );
    }

    private String endpointStub(BriefWorkItemExportResponse export, BriefWorkItemExportResponse.WorkItem workItem) {
        var className = sanitizeName(workItem.title()) + "Controller";
        return """
                // EXAMPLE STARTER CODE ONLY
                // Derived from approved HealthForge work-item export.
                // Brief ID: %s
                // Work Item ID: %s
                // Related Findings: %s
                // Standards Touchpoints: %s
                // Human Review Status: %s
                package dev.healthforge.generated.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/example")
                public class %s {

                    // TODO: replace placeholder route, request model, and auth boundary.
                    // TODO: validate actual payer/counterparty applicability before implementation.
                    @GetMapping("/status")
                    public String status() {
                        return "Starter stub for: %s";
                    }
                }
                """.formatted(
                export.briefId(),
                workItem.workItemId(),
                String.join(", ", workItem.relatedFindingIds()),
                String.join(", ", workItem.standardsTouchpoints()),
                workItem.humanReviewStatus(),
                className,
                workItem.rationale().replace("\"", "'")
        );
    }

    private String serviceStub(BriefWorkItemExportResponse export, BriefWorkItemExportResponse.WorkItem workItem) {
        var className = sanitizeName(workItem.title()) + "Service";
        return """
                // EXAMPLE STARTER CODE ONLY
                // Derived from approved HealthForge work-item export.
                // Brief ID: %s
                // Work Item ID: %s
                // Related Findings: %s
                // Standards Touchpoints: %s
                // Human Review Status: %s
                package dev.healthforge.generated.example;

                import org.springframework.stereotype.Service;

                @Service
                public class %s {

                    // TODO: map this stub to a reviewed bounded context before use.
                    // TODO: confirm validation notes and evidence links with a human reviewer.
                    public String describeIntent() {
                        return "%s";
                    }
                }
                """.formatted(
                export.briefId(),
                workItem.workItemId(),
                String.join(", ", workItem.relatedFindingIds()),
                String.join(", ", workItem.standardsTouchpoints()),
                workItem.humanReviewStatus(),
                className,
                workItem.rationale().replace("\"", "'")
        );
    }

    private String sanitizeName(String input) {
        var cleaned = input.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (cleaned.isBlank()) {
            return "GeneratedExample";
        }
        var parts = cleaned.split("\\s+");
        var builder = new StringBuilder();
        for (var part : parts) {
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
