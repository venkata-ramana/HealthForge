create table if not exists answer_generation_event (
    answer_generation_event_id varchar(120) primary key,
    organization_id varchar(120) not null,
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    corpus_id varchar(120) not null,
    corpus_version varchar(120) not null,
    answer_status varchar(40) not null,
    retrieval_result_count integer not null,
    unsupported_triggered boolean not null,
    question_hash varchar(64) not null,
    created_at timestamptz not null
);

create index if not exists answer_generation_event_org_created_idx
    on answer_generation_event (organization_id, created_at desc);

create index if not exists answer_generation_event_org_status_idx
    on answer_generation_event (organization_id, answer_status, created_at desc);
