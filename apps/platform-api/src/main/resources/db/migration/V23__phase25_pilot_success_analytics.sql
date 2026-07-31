create table if not exists pilot_feedback (
    feedback_id varchar(120) primary key,
    organization_id varchar(120) not null,
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    feedback_type varchar(80) not null,
    rating integer not null,
    brief_id varchar(100) references engineering_brief(brief_id),
    finding_id varchar(120) references brief_finding(finding_id),
    note text,
    created_at timestamptz not null
);

create index if not exists pilot_feedback_org_created_idx
    on pilot_feedback (organization_id, created_at desc);

create index if not exists pilot_feedback_org_type_idx
    on pilot_feedback (organization_id, feedback_type, created_at desc);
