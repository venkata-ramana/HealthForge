create table if not exists retrieval_feedback (
    retrieval_feedback_id varchar(120) primary key,
    organization_id varchar(120) not null,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    finding_id varchar(120) not null references brief_finding(finding_id),
    source_id varchar(120),
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    feedback_type varchar(60) not null,
    note text,
    created_at timestamptz not null
);

create index if not exists retrieval_feedback_org_idx on retrieval_feedback (organization_id, created_at desc);
create index if not exists retrieval_feedback_type_idx on retrieval_feedback (organization_id, feedback_type, created_at desc);
