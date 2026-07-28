alter table tracked_export_event
    add column if not exists writeback_approval_id varchar(120);

alter table tracked_export_event
    add column if not exists approval_actor_id varchar(255);

alter table tracked_export_event
    add column if not exists approval_actor_role varchar(40);

alter table tracked_export_event
    add column if not exists approval_recorded_at timestamptz;

alter table tracked_export_event
    add column if not exists target_locator varchar(255);

alter table tracked_export_event
    add column if not exists execution_status varchar(40) not null default 'preview_generated';

alter table tracked_export_event
    add column if not exists execution_result text;

alter table tracked_export_event
    add column if not exists external_reference varchar(255);

alter table tracked_export_event
    add column if not exists retry_count integer not null default 0;

alter table tracked_export_event
    add column if not exists retried_from_event_id varchar(120);

alter table tracked_export_event
    add column if not exists executed_at timestamptz;

create index if not exists tracked_export_event_org_status_idx on tracked_export_event (organization_id, execution_status, occurred_at);
