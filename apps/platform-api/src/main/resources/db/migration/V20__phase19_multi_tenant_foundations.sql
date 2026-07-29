create table if not exists tenant_provisioning_request (
    provisioning_request_id varchar(160) primary key,
    organization_id varchar(120) not null,
    tenant_key varchar(120) not null,
    tenant_name varchar(200) not null,
    deployment_model varchar(80) not null,
    environment_shape varchar(120) not null,
    status varchar(60) not null,
    requested_by varchar(120) not null,
    delegated_admin varchar(120) not null,
    requested_capabilities text not null,
    onboarding_summary text not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index if not exists tenant_provisioning_request_org_idx
    on tenant_provisioning_request (organization_id, updated_at desc);
