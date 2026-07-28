create table if not exists workspace_project (
    project_id varchar(160) primary key,
    organization_id varchar(120) not null,
    name varchar(255) not null,
    kind varchar(80) not null,
    description text not null,
    owner_actor_id varchar(255) not null,
    tags text,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists workspace_project_brief (
    project_id varchar(160) not null references workspace_project(project_id),
    brief_id varchar(100) not null references engineering_brief(brief_id),
    linked_at timestamptz not null,
    primary key (project_id, brief_id)
);

create table if not exists workspace_assignment (
    assignment_id varchar(160) primary key,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    organization_id varchar(120) not null,
    assignee_actor_id varchar(255) not null,
    assignee_role varchar(40) not null,
    queue_name varchar(120) not null,
    status varchar(40) not null,
    handoff_summary text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists workflow_configuration (
    config_id varchar(160) primary key,
    organization_id varchar(120) not null,
    config_type varchar(80) not null,
    name varchar(255) not null,
    version_label varchar(80) not null,
    status varchar(40) not null,
    summary text not null,
    prompt_profile varchar(120) not null,
    retrieval_profile varchar(120) not null,
    workflow_profile varchar(120) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists workspace_saved_view (
    view_id varchar(160) primary key,
    organization_id varchar(120) not null,
    project_id varchar(160) references workspace_project(project_id),
    view_type varchar(80) not null,
    name varchar(255) not null,
    query_text text not null,
    summary text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists evidence_collection (
    collection_id varchar(160) primary key,
    organization_id varchar(120) not null,
    project_id varchar(160) references workspace_project(project_id),
    name varchar(255) not null,
    summary text not null,
    source_count integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists workspace_identity_provider (
    provider_id varchar(160) primary key,
    organization_id varchar(120) not null,
    provider_type varchar(80) not null,
    display_name varchar(255) not null,
    status varchar(40) not null,
    fallback_mode varchar(120) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists workspace_group_role_mapping (
    mapping_id varchar(220) primary key,
    provider_id varchar(160) not null references workspace_identity_provider(provider_id),
    organization_id varchar(120) not null,
    group_name varchar(255) not null,
    actor_role varchar(40) not null,
    scope_summary text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (provider_id, group_name, actor_role)
);

create index if not exists workspace_project_org_idx on workspace_project (organization_id, updated_at desc);
create index if not exists workspace_project_brief_project_idx on workspace_project_brief (project_id, linked_at desc);
create index if not exists workspace_assignment_org_idx on workspace_assignment (organization_id, updated_at desc);
create index if not exists workflow_configuration_org_idx on workflow_configuration (organization_id, updated_at desc);
create index if not exists workspace_saved_view_org_idx on workspace_saved_view (organization_id, updated_at desc);
create index if not exists evidence_collection_org_idx on evidence_collection (organization_id, updated_at desc);
create index if not exists workspace_identity_provider_org_idx on workspace_identity_provider (organization_id, provider_type);
create index if not exists workspace_group_role_mapping_org_idx on workspace_group_role_mapping (organization_id, actor_role);
