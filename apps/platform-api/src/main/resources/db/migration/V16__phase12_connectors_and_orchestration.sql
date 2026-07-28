create table if not exists inbound_case (
    inbound_case_id varchar(120) primary key,
    organization_id varchar(120) not null,
    source_system varchar(80) not null,
    external_case_id varchar(255) not null,
    title text not null,
    summary text not null,
    intake_status varchar(40) not null,
    requested_role varchar(40) not null,
    requested_assignee varchar(255),
    linked_brief_id varchar(100) references engineering_brief(brief_id),
    source_locator varchar(255),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists inbound_case_org_idx on inbound_case (organization_id, created_at desc);

create table if not exists orchestration_template (
    template_id varchar(120) primary key,
    organization_id varchar(120) not null,
    name varchar(255) not null,
    template_type varchar(80) not null,
    summary text not null,
    default_queue varchar(120) not null,
    default_target_system varchar(80) not null,
    workflow_phase varchar(80) not null,
    guardrails text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists orchestration_template_org_idx on orchestration_template (organization_id, updated_at desc);

create table if not exists integration_recovery_action (
    recovery_action_id varchar(120) primary key,
    organization_id varchar(120) not null,
    source_type varchar(80) not null,
    source_id varchar(120) not null,
    connector_type varchar(80) not null,
    previous_status varchar(80),
    requested_action varchar(80) not null,
    outcome_status varchar(80) not null,
    summary text not null,
    created_at timestamptz not null
);

create index if not exists integration_recovery_action_org_idx on integration_recovery_action (organization_id, created_at desc);
