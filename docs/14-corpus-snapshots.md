# Corpus snapshots

A corpus snapshot is an immutable named set of indexed source versions. Create it through `POST /v1/corpus-snapshots` with a corpus ID, version, and source-version IDs. Retrieval resolves only passages belonging to that exact set, and Briefs retain its ID/version. Existing snapshots cannot be replaced; publish a new version for any source change.
