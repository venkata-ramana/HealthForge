create table if not exists source_watchlist (
    watchlist_id varchar(160) primary key,
    organization_id varchar(120) not null,
    manifest_source_id varchar(160) not null,
    watch_reason text not null,
    desired_check_frequency varchar(80) not null,
    monitored_by varchar(255) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (organization_id, manifest_source_id)
);

create table if not exists workspace_research_pack (
    research_pack_id varchar(160) primary key,
    organization_id varchar(120) not null,
    project_id varchar(160) references workspace_project(project_id),
    name varchar(255) not null,
    summary text not null,
    recurring_questions text not null,
    question_count integer not null,
    next_review_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists source_watchlist_org_idx on source_watchlist (organization_id, updated_at desc);
create index if not exists workspace_research_pack_org_idx on workspace_research_pack (organization_id, updated_at desc);
