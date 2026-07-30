package dev.healthforge.platform.ingestion;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Service
public class SourceVersionService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public SourceVersionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SourceVersionResponse get(String sourceVersionId) {
        var rows = jdbcTemplate.query(
                """
                select source_version_id, manifest_source_id, source_version, source_type, title,
                       canonical_url, artifact_sha256, content_type, retrieved_at, parser_version,
                       chunking_version, status, allowed_use, terms_review_decision,
                       terms_reviewed_by, terms_reviewed_at, superseded_by_source_version_id
                from source_version
                where source_version_id = ?
                """,
                (rs, rowNum) -> mapSourceVersionResponse(rs, false, null, null),
                sourceVersionId
        );
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source version was not found");
        return rows.getFirst();
    }

    public SourceVersionResponse updateLifecycle(String sourceVersionId, SourceLifecycleUpdateRequest request) {
        var normalizedStatus = request.status().trim().toLowerCase();
        if (!normalizedStatus.equals("withdrawn") && !normalizedStatus.equals("active")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Lifecycle updates currently support only active or withdrawn");
        }

        var updated = jdbcTemplate.update(
                "update source_version set status = ?, superseded_by_source_version_id = null where source_version_id = ?",
                normalizedStatus, sourceVersionId
        );
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source version was not found");
        return get(sourceVersionId);
    }

    public SourceOperationsOverviewResponse operations(AuthenticatedActor actor) {
        var trackedSources = latestTrackedSources();
        var watchlists = watchlists(actor.organizationId(), trackedSources);
        var freshnessAlerts = freshnessAlerts(trackedSources);
        var recommendations = reindexRecommendations(trackedSources, watchlists);
        return new SourceOperationsOverviewResponse(
                actor.organizationId(),
                Instant.now(clock),
                new SourceOperationsOverviewResponse.Summary(
                        trackedSources.size(),
                        watchlists.size(),
                        (int) trackedSources.stream().filter(item -> "stale".equals(item.freshnessStatus())).count(),
                        (int) trackedSources.stream().filter(item -> "superseded".equals(item.freshnessStatus())).count(),
                        trackedSources.stream().map(TrackedSource::retrievedAt).max(Instant::compareTo).orElse(null),
                        "This view highlights monitored sources, stale coverage, and corpus-refresh recommendations without claiming automated production source management."
                ),
                watchlists,
                freshnessAlerts,
                recommendations
        );
    }

    public SourceOperationsOverviewResponse.WatchlistItem createWatchlist(SourceWatchlistRequest request, AuthenticatedActor actor) {
        var trackedSource = latestTrackedSources().stream()
                .filter(item -> item.manifestSourceId().equals(request.manifestSourceId().trim()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manifest source was not found in the current evidence layer."));
        var now = Timestamp.from(Instant.now(clock));
        var watchlistId = actor.organizationId() + ".watch." + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into source_watchlist (
                    watchlist_id, organization_id, manifest_source_id, watch_reason, desired_check_frequency,
                    monitored_by, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (organization_id, manifest_source_id)
                do update set watch_reason = excluded.watch_reason,
                              desired_check_frequency = excluded.desired_check_frequency,
                              monitored_by = excluded.monitored_by,
                              updated_at = excluded.updated_at
                """,
                watchlistId,
                actor.organizationId(),
                request.manifestSourceId().trim(),
                request.watchReason().trim(),
                request.desiredCheckFrequency().trim(),
                actor.actorId(),
                now,
                now
        );
        return watchlists(actor.organizationId(), latestTrackedSources()).stream()
                .filter(item -> item.manifestSourceId().equals(request.manifestSourceId().trim()))
                .findFirst()
                .orElseThrow();
    }

    private SourceVersionResponse mapSourceVersionResponse(
            java.sql.ResultSet rs,
            boolean watchlisted,
            String watchReason,
            String monitoredBy
    ) throws java.sql.SQLException {
        var retrievedAt = rs.getTimestamp("retrieved_at").toInstant();
        var status = rs.getString("status");
        var ageDays = Math.max(0L, ChronoUnit.DAYS.between(retrievedAt, Instant.now(clock)));
        var freshnessStatus = freshnessStatus(status, ageDays);
        var recommendedAction = switch (freshnessStatus) {
            case "withdrawn" -> "Withdraw or replace this source version from current planning snapshots.";
            case "superseded" -> "Review the newer lifecycle replacement before reusing older evidence.";
            case "stale" -> "Check whether a newer public source version should be ingested and re-indexed.";
            default -> "No urgent source refresh action is currently recommended.";
        };
        var changeSummary = rs.getString("superseded_by_source_version_id") == null
                ? "No linked replacement source version is recorded for this artifact."
                : "A linked replacement source version exists and should be compared before downstream reuse.";
        return new SourceVersionResponse(
                rs.getString("source_version_id"),
                rs.getString("manifest_source_id"),
                rs.getString("source_version"),
                rs.getString("source_type"),
                rs.getString("title"),
                rs.getString("canonical_url"),
                rs.getString("artifact_sha256"),
                rs.getString("content_type"),
                retrievedAt,
                rs.getString("parser_version"),
                rs.getString("chunking_version"),
                status,
                rs.getString("allowed_use"),
                rs.getString("terms_review_decision"),
                rs.getString("terms_reviewed_by"),
                rs.getTimestamp("terms_reviewed_at").toInstant(),
                rs.getString("superseded_by_source_version_id"),
                isEligibleForCurrentSnapshot(status, rs.getString("terms_review_decision")),
                freshnessStatus,
                ageDays,
                watchlisted,
                watchReason == null ? "Not currently watchlisted." : watchReason,
                recommendedAction,
                changeSummary
        );
    }

    private List<TrackedSource> latestTrackedSources() {
        return jdbcTemplate.query("""
                select distinct on (manifest_source_id)
                       source_version_id, manifest_source_id, source_version, title, canonical_url, retrieved_at, status,
                       superseded_by_source_version_id
                from source_version
                order by manifest_source_id, retrieved_at desc, source_version_id desc
                """, (rs, rowNum) -> {
            var retrievedAt = rs.getTimestamp("retrieved_at").toInstant();
            var status = rs.getString("status");
            var sourceAgeDays = Math.max(0L, ChronoUnit.DAYS.between(retrievedAt, Instant.now(clock)));
            return new TrackedSource(
                    rs.getString("source_version_id"),
                    rs.getString("manifest_source_id"),
                    rs.getString("source_version"),
                    rs.getString("title"),
                    rs.getString("canonical_url"),
                    retrievedAt,
                    status,
                    sourceAgeDays,
                    freshnessStatus(status, sourceAgeDays),
                    rs.getString("superseded_by_source_version_id")
            );
        });
    }

    private List<SourceOperationsOverviewResponse.WatchlistItem> watchlists(String organizationId, List<TrackedSource> trackedSources) {
        var trackedByManifest = trackedSources.stream()
                .collect(java.util.stream.Collectors.toMap(
                        TrackedSource::manifestSourceId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return jdbcTemplate.query("""
                select watchlist_id, manifest_source_id, watch_reason, desired_check_frequency, updated_at
                from source_watchlist
                where organization_id = ?
                order by updated_at desc, manifest_source_id
                """, (rs, rowNum) -> {
            var trackedSource = trackedByManifest.get(rs.getString("manifest_source_id"));
            if (trackedSource == null) {
                return null;
            }
            return new SourceOperationsOverviewResponse.WatchlistItem(
                    rs.getString("watchlist_id"),
                    trackedSource.manifestSourceId(),
                    trackedSource.title(),
                    trackedSource.canonicalUrl(),
                    trackedSource.sourceVersionId(),
                    trackedSource.sourceVersion(),
                    trackedSource.freshnessStatus(),
                    trackedSource.sourceAgeDays(),
                    rs.getString("watch_reason"),
                    rs.getString("desired_check_frequency"),
                    trackedSource.recommendedAction(),
                    rs.getTimestamp("updated_at").toInstant()
            );
        }, organizationId).stream().filter(java.util.Objects::nonNull).toList();
    }

    private List<SourceOperationsOverviewResponse.FreshnessAlert> freshnessAlerts(List<TrackedSource> trackedSources) {
        return trackedSources.stream()
                .filter(item -> !"current".equals(item.freshnessStatus()))
                .limit(6)
                .map(item -> new SourceOperationsOverviewResponse.FreshnessAlert(
                        item.manifestSourceId(),
                        item.title(),
                        item.sourceVersionId(),
                        item.sourceVersion(),
                        item.freshnessStatus(),
                        item.sourceAgeDays(),
                        item.changeSummary(),
                        switch (item.freshnessStatus()) {
                            case "stale" -> "This source has not been refreshed in more than 30 days.";
                            case "superseded" -> "A newer lifecycle replacement exists for this source family.";
                            case "withdrawn" -> "This source version is no longer active for current planning.";
                            default -> "Review source freshness before relying on this evidence.";
                        }
                ))
                .toList();
    }

    private List<SourceOperationsOverviewResponse.ReindexRecommendation> reindexRecommendations(
            List<TrackedSource> trackedSources,
            List<SourceOperationsOverviewResponse.WatchlistItem> watchlists
    ) {
        var watched = watchlists.stream().map(SourceOperationsOverviewResponse.WatchlistItem::manifestSourceId).collect(java.util.stream.Collectors.toSet());
        return trackedSources.stream()
                .filter(item -> "stale".equals(item.freshnessStatus()) || "superseded".equals(item.freshnessStatus()) || watched.contains(item.manifestSourceId()))
                .limit(6)
                .map(item -> new SourceOperationsOverviewResponse.ReindexRecommendation(
                        item.manifestSourceId(),
                        item.title(),
                        item.sourceVersionId(),
                        item.sourceVersion(),
                        "stale".equals(item.freshnessStatus()) ? "refresh_snapshot" : "compare_replacement",
                        item.changeSummary(),
                        item.recommendedAction()
                ))
                .toList();
    }

    private boolean isEligibleForCurrentSnapshot(String status, String termsReviewDecision) {
        return ("indexed".equals(status) || "active".equals(status)) && "approved".equalsIgnoreCase(termsReviewDecision);
    }

    private String freshnessStatus(String status, long ageDays) {
        if ("withdrawn".equalsIgnoreCase(status)) {
            return "withdrawn";
        }
        if ("superseded".equalsIgnoreCase(status)) {
            return "superseded";
        }
        if (ageDays > 30) {
            return "stale";
        }
        return "current";
    }

    private record TrackedSource(
            String sourceVersionId,
            String manifestSourceId,
            String sourceVersion,
            String title,
            String canonicalUrl,
            Instant retrievedAt,
            String status,
            long sourceAgeDays,
            String freshnessStatus,
            String supersededBySourceVersionId
    ) {
        private String recommendedAction() {
            return switch (freshnessStatus) {
                case "withdrawn" -> "Withdraw or replace this source version from current planning snapshots.";
                case "superseded" -> "Compare the current snapshot with the newer replacement source version.";
                case "stale" -> "Schedule ingestion and re-index review for this manifest source.";
                default -> "Keep monitoring source freshness through the watchlist.";
            };
        }

        private String changeSummary() {
            return supersededBySourceVersionId == null
                    ? "No linked replacement source version is currently recorded."
                    : "A linked replacement source version exists and should be compared before reuse.";
        }
    }
}
