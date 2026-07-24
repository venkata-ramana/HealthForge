create table engineering_brief (
    brief_id varchar(100) primary key,
    status varchar(40) not null,
    created_at timestamptz not null,
    question text not null,
    project_context text not null,
    corpus_id varchar(120) not null,
    corpus_version varchar(120) not null
);

create table brief_source (
    brief_id varchar(100) not null references engineering_brief(brief_id),
    source_id varchar(120) not null,
    source_version varchar(255) not null,
    source_type varchar(80) not null,
    title text not null,
    canonical_url text not null,
    primary key (brief_id, source_id, source_version)
);

create table brief_finding (
    finding_id varchar(120) primary key,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    kind varchar(40) not null,
    statement text not null,
    confidence varchar(20) not null,
    source_id varchar(120) not null,
    source_version varchar(255) not null,
    locator varchar(255) not null,
    support text not null
);

create table brief_review_decision (
    review_id varchar(120) primary key,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    finding_id varchar(120) not null references brief_finding(finding_id),
    decision varchar(40) not null,
    reviewer varchar(255) not null,
    decided_at timestamptz not null,
    rationale text not null,
    corrected_statement text
);

create index brief_finding_brief_id_idx on brief_finding (brief_id);
create index brief_review_decision_brief_id_idx on brief_review_decision (brief_id);
