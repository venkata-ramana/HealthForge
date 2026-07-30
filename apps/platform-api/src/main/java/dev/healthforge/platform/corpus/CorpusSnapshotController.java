package dev.healthforge.platform.corpus;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/corpus-snapshots")
public class CorpusSnapshotController {
    private final CorpusSnapshotService service;
    public CorpusSnapshotController(CorpusSnapshotService service) { this.service = service; }
    @PostMapping public CorpusSnapshotResponse create(@Valid @RequestBody CorpusSnapshotRequest request) { return service.create(request); }
    @GetMapping("/{corpusId}/{corpusVersion}") public CorpusSnapshotResponse get(@PathVariable String corpusId, @PathVariable String corpusVersion) { return service.get(corpusId, corpusVersion); }
    @GetMapping("/{corpusId}/{corpusVersion}/diff/{againstCorpusVersion}")
    public CorpusSnapshotDiffResponse diff(
            @PathVariable String corpusId,
            @PathVariable String corpusVersion,
            @PathVariable String againstCorpusVersion
    ) {
        return service.diff(corpusId, corpusVersion, againstCorpusVersion);
    }
}
