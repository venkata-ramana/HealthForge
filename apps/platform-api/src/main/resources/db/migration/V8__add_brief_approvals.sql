create table brief_approval (
    approval_id varchar(120) primary key,
    brief_id varchar(100) not null references engineering_brief(brief_id),
    approver varchar(255) not null,
    approver_role varchar(40) not null,
    approved_at timestamptz not null,
    rationale text not null
);

create index brief_approval_brief_id_idx on brief_approval (brief_id, approved_at);
