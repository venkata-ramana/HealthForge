alter table ingestion_job
    add column if not exists organization_id varchar(120) not null default 'local.default';

alter table engineering_brief
    add column if not exists organization_id varchar(120) not null default 'local.default';

alter table brief_review_decision
    add column if not exists organization_id varchar(120) not null default 'local.default';

alter table brief_approval
    add column if not exists organization_id varchar(120) not null default 'local.default';

alter table brief_audit_event
    add column if not exists organization_id varchar(120) not null default 'local.default';

create index if not exists engineering_brief_org_id_idx on engineering_brief (organization_id, created_at);
create index if not exists brief_review_decision_org_id_idx on brief_review_decision (organization_id, decided_at);
create index if not exists brief_approval_org_id_idx on brief_approval (organization_id, approved_at);
create index if not exists brief_audit_event_org_id_idx on brief_audit_event (organization_id, occurred_at);
create index if not exists ingestion_job_org_id_idx on ingestion_job (organization_id, requested_at);

create table if not exists fhir_validation_run (
    validation_run_id varchar(120) primary key,
    organization_id varchar(120) not null,
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    package_id varchar(255) not null,
    package_version varchar(80) not null,
    profile_url text not null,
    data_classification varchar(40) not null,
    validation_status varchar(40) not null,
    finding_count integer not null,
    created_at timestamptz not null
);

create index if not exists fhir_validation_run_org_id_idx on fhir_validation_run (organization_id, created_at);

create table if not exists tracked_export_event (
    tracked_export_event_id varchar(120) primary key,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    organization_id varchar(120) not null,
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    target_system varchar(40) not null,
    export_mode varchar(40) not null,
    work_item_count integer not null,
    export_reason text,
    retention_until timestamptz not null,
    occurred_at timestamptz not null
);

create index if not exists tracked_export_event_org_id_idx on tracked_export_event (organization_id, occurred_at);
