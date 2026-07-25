create table brief_audit_event (
    audit_event_id varchar(120) primary key,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    event_type varchar(80) not null,
    occurred_at timestamptz not null,
    summary text not null,
    details text
);

create index brief_audit_event_brief_id_idx on brief_audit_event (brief_id, occurred_at);
