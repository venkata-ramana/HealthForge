create table corpus_snapshot (
    corpus_id varchar(120) not null,
    corpus_version varchar(120) not null,
    created_at timestamptz not null,
    retrieval_configuration varchar(120) not null,
    primary key (corpus_id, corpus_version)
);

create table corpus_snapshot_source (
    corpus_id varchar(120) not null,
    corpus_version varchar(120) not null,
    source_version_id varchar(80) not null references source_version(source_version_id),
    primary key (corpus_id, corpus_version, source_version_id),
    foreign key (corpus_id, corpus_version) references corpus_snapshot(corpus_id, corpus_version)
);
