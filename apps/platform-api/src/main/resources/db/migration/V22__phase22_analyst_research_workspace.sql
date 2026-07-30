create table if not exists workspace_question_pack (
    question_pack_id varchar(160) primary key,
    organization_id varchar(120) not null,
    project_id varchar(160) references workspace_project(project_id),
    name varchar(255) not null,
    summary text not null,
    persona varchar(120) not null,
    template_kind varchar(120) not null,
    starter_question text not null,
    question_prompts text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists workspace_research_notebook (
    notebook_id varchar(160) primary key,
    organization_id varchar(120) not null,
    project_id varchar(160) references workspace_project(project_id),
    brief_id varchar(160),
    title varchar(255) not null,
    summary text not null,
    key_takeaways text not null,
    evidence_bundle_name varchar(255) not null,
    handoff_summary text not null,
    continuity_note text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists workspace_review_escalation (
    escalation_id varchar(160) primary key,
    organization_id varchar(120) not null,
    assignment_id varchar(160),
    brief_id varchar(160) not null,
    escalation_reason text not null,
    urgency varchar(80) not null,
    destination_queue varchar(120) not null,
    status varchar(80) not null,
    note text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists workspace_question_pack_org_idx on workspace_question_pack (organization_id, updated_at desc);
create index if not exists workspace_research_notebook_org_idx on workspace_research_notebook (organization_id, updated_at desc);
create index if not exists workspace_review_escalation_org_idx on workspace_review_escalation (organization_id, updated_at desc);
