create table if not exists collaboration_notification_event (
    collaboration_notification_event_id varchar(120) primary key,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    organization_id varchar(120) not null,
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    target_system varchar(40) not null,
    delivery_mode varchar(40) not null,
    notification_type varchar(40) not null,
    handoff_role varchar(40),
    target_locator varchar(255),
    message_summary text not null,
    approval_id varchar(120),
    delivery_status varchar(40) not null,
    external_reference varchar(255),
    retention_until timestamptz not null,
    occurred_at timestamptz not null
);

create index if not exists collaboration_notification_event_org_idx
    on collaboration_notification_event (organization_id, occurred_at desc);

create table if not exists documentation_export_event (
    documentation_export_event_id varchar(120) primary key,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    organization_id varchar(120) not null,
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    target_system varchar(40) not null,
    export_mode varchar(40) not null,
    package_format varchar(40) not null,
    approval_id varchar(120),
    target_locator varchar(255),
    delivery_status varchar(40) not null,
    external_reference varchar(255),
    trace_summary text not null,
    retention_until timestamptz not null,
    occurred_at timestamptz not null
);

create index if not exists documentation_export_event_org_idx
    on documentation_export_event (organization_id, occurred_at desc);

create table if not exists workflow_event_subscription (
    workflow_event_subscription_id varchar(120) primary key,
    organization_id varchar(120) not null,
    event_family varchar(60) not null,
    event_name varchar(80) not null,
    environment_scope varchar(60) not null,
    target_label varchar(120) not null,
    delivery_mode varchar(40) not null,
    enabled boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index if not exists workflow_event_subscription_unique_idx
    on workflow_event_subscription (organization_id, event_family, event_name, environment_scope, target_label);

create table if not exists workflow_event (
    workflow_event_id varchar(120) primary key,
    organization_id varchar(120) not null,
    brief_id varchar(100) references engineering_brief(brief_id),
    actor_id varchar(255) not null,
    actor_role varchar(40) not null,
    event_family varchar(60) not null,
    event_name varchar(80) not null,
    payload_summary text not null,
    environment_scope varchar(60) not null,
    occurred_at timestamptz not null
);

create index if not exists workflow_event_org_idx
    on workflow_event (organization_id, occurred_at desc);

create table if not exists outbound_webhook_delivery (
    outbound_webhook_delivery_id varchar(120) primary key,
    workflow_event_id varchar(120) not null references workflow_event(workflow_event_id),
    organization_id varchar(120) not null,
    target_label varchar(120) not null,
    delivery_mode varchar(40) not null,
    delivery_status varchar(40) not null,
    retry_count integer not null default 0,
    response_summary text not null,
    external_reference varchar(255),
    last_attempt_at timestamptz not null
);

create index if not exists outbound_webhook_delivery_org_idx
    on outbound_webhook_delivery (organization_id, last_attempt_at desc);
