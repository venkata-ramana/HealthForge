package dev.healthforge.platform.corpus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CorpusSnapshotRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotEmpty List<@NotBlank String> sourceVersionIds,
        Boolean includeHistoricalSources
) {}
