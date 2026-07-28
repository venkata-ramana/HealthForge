create table if not exists operations_attestation (
    attestation_id varchar(120) primary key,
    organization_id varchar(120) not null,
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    policy_area varchar(120) not null,
    environment_name varchar(120) not null,
    attestation_type varchar(120) not null,
    change_summary text not null,
    control_ids text,
    acknowledgment text not null,
    created_at timestamptz not null
);

create index if not exists operations_attestation_org_created_idx
    on operations_attestation (organization_id, created_at desc);
