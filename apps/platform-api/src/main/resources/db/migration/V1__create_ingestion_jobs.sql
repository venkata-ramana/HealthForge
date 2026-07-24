create table ingestion_job (
    ingestion_id varchar(80) primary key,
    manifest_source_id varchar(120) not null,
    source_version varchar(255) not null,
    canonical_url text not null,
    expected_content_type varchar(100) not null,
    requested_by varchar(255),
    status varchar(40) not null,
    requested_at timestamptz not null
);

create index ingestion_job_manifest_source_id_idx on ingestion_job (manifest_source_id);
