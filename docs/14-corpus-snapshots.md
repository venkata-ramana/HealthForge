# Corpus snapshots

A corpus snapshot is an immutable named set of source versions. Create it through `POST /v1/corpus-snapshots` with a corpus ID, version, and source-version IDs. By default, snapshot creation accepts only current-eligible versions: `indexed` or `active` sources whose terms review decision is `approved`. Retrieval resolves only passages belonging to that exact set, and Briefs retain its ID/version.

If you must reconstruct historical evidence, pass `include_historical_sources: true`. That explicit override allows `withdrawn` or `superseded` source versions to enter the snapshot without changing their lifecycle state. Existing snapshots cannot be replaced; publish a new version for any source change.
