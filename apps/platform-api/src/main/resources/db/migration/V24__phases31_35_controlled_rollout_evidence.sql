create table if not exists controlled_rollout_evidence (
    evidence_id varchar(160) primary key,
    organization_id varchar(120) not null,
    phase_id varchar(40) not null,
    check_id varchar(120) not null,
    status varchar(40) not null,
    owner_role varchar(80) not null,
    evidence_summary text not null,
    next_action text not null,
    actor_id varchar(255) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (organization_id, phase_id, check_id)
);

create index if not exists controlled_rollout_evidence_org_idx
    on controlled_rollout_evidence (organization_id, phase_id, updated_at desc);
