package dev.healthforge.platform.ingestion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SourceVersionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from source_watchlist where organization_id = 'org.sourceops.test'");
        jdbcTemplate.update("delete from source_passage where source_version_id like 'srcver-sourceops-%'");
        jdbcTemplate.update("delete from source_version where source_version_id like 'srcver-sourceops-%'");
    }

    @Test
    void returnsOperationsOverviewAndAllowsWatchlists() throws Exception {
        var sourceVersionId = "srcver-sourceops-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into source_version (
                    source_version_id, manifest_source_id, source_version, source_type, title,
                    canonical_url, artifact_uri, artifact_sha256, content_type, retrieved_at,
                    parser_version, chunking_version, status, allowed_use,
                    terms_review_decision, terms_reviewed_by, terms_reviewed_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sourceVersionId,
                "cms-0057-f-final-rule",
                "2026-07-15",
                "governing_regulation",
                "CMS Final Rule",
                "https://example.test/cms-final-rule",
                "file:///tmp/cms-final-rule.pdf",
                UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""),
                "application/pdf",
                Timestamp.from(Instant.parse("2026-06-01T12:00:00Z")),
                "pdfbox-v1",
                "page-v1",
                "active",
                "public_reference",
                "approved",
                "local.admin",
                Timestamp.from(Instant.parse("2026-06-01T12:00:00Z"))
        );

        mockMvc.perform(post("/v1/source-versions/watchlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manifest_source_id":"cms-0057-f-final-rule",
                                  "watch_reason":"Track freshness before reusing prior-auth planning evidence.",
                                  "desired_check_frequency":"weekly"
                                }
                                """)
                        .header("X-HealthForge-Actor", "sourceops.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.sourceops.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest_source_id").value("cms-0057-f-final-rule"))
                .andExpect(jsonPath("$.freshness_status").value("superseded"));

        mockMvc.perform(get("/v1/source-versions/operations")
                        .header("X-HealthForge-Actor", "sourceops.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.sourceops.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.watchlisted_sources").value(1))
                .andExpect(jsonPath("$.watchlists[0].manifest_source_id").value("cms-0057-f-final-rule"))
                .andExpect(jsonPath("$.freshness_alerts[0].freshness_status").value("superseded"));
    }
}
