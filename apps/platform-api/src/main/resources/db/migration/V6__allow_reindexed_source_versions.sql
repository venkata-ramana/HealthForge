alter table source_version
    drop constraint source_version_manifest_source_id_artifact_sha256_key;

alter table source_version
    add constraint source_version_manifest_checksum_parser_chunking_key
    unique (manifest_source_id, artifact_sha256, parser_version, chunking_version);
