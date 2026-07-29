create table if not exists pilot_success_checkpoint (
    checkpoint_id varchar(120) primary key,
    organization_id varchar(120) not null,
    milestone_name varchar(255) not null,
    owner_role varchar(80) not null,
    target_outcome text not null,
    status varchar(80) not null,
    note text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists pilot_success_checkpoint_org_updated_idx
    on pilot_success_checkpoint (organization_id, updated_at desc);
