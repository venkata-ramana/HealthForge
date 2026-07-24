create table source_version (
    source_version_id varchar(80) primary key,
    manifest_source_id varchar(120) not null,
    source_version varchar(255) not null,
    source_type varchar(80) not null,
    title text not null,
    canonical_url text not null,
    artifact_uri text not null,
    artifact_sha256 varchar(64) not null,
    content_type varchar(100) not null,
    retrieved_at timestamptz not null,
    parser_version varchar(80) not null,
    chunking_version varchar(80) not null,
    status varchar(40) not null,
    unique (manifest_source_id, artifact_sha256)
);

create table source_passage (
    passage_id varchar(100) primary key,
    source_version_id varchar(80) not null references source_version(source_version_id),
    ordinal integer not null,
    locator varchar(255) not null,
    normalized_text text not null,
    unique (source_version_id, ordinal)
);

create index source_passage_source_version_idx on source_passage (source_version_id);
