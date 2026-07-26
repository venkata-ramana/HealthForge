create table if not exists actor_user (
    actor_user_id varchar(255) primary key,
    display_name varchar(255),
    auth_subject varchar(255) not null,
    identity_mode varchar(80) not null,
    created_at timestamptz not null,
    last_seen_at timestamptz not null
);

create table if not exists actor_organization (
    organization_id varchar(120) primary key,
    display_name varchar(255) not null,
    status varchar(40) not null,
    created_at timestamptz not null,
    last_seen_at timestamptz not null
);

create table if not exists actor_organization_membership (
    membership_id varchar(120) primary key,
    actor_user_id varchar(255) not null references actor_user(actor_user_id),
    organization_id varchar(120) not null references actor_organization(organization_id),
    status varchar(40) not null,
    joined_at timestamptz not null,
    last_seen_at timestamptz not null,
    unique (actor_user_id, organization_id)
);

create table if not exists actor_role_assignment (
    role_assignment_id varchar(120) primary key,
    actor_user_id varchar(255) not null references actor_user(actor_user_id),
    organization_id varchar(120) not null references actor_organization(organization_id),
    actor_role varchar(40) not null,
    granted_by varchar(255) not null,
    granted_at timestamptz not null,
    last_seen_at timestamptz not null,
    unique (actor_user_id, organization_id, actor_role)
);

create index if not exists actor_organization_status_idx on actor_organization (status, last_seen_at desc);
create index if not exists actor_membership_org_idx on actor_organization_membership (organization_id, last_seen_at desc);
create index if not exists actor_membership_user_idx on actor_organization_membership (actor_user_id, last_seen_at desc);
create index if not exists actor_role_assignment_org_idx on actor_role_assignment (organization_id, last_seen_at desc);
create index if not exists actor_role_assignment_user_idx on actor_role_assignment (actor_user_id, last_seen_at desc);
