alter table source_version
    add column if not exists allowed_use varchar(80) not null default 'public_reference',
    add column if not exists terms_review_decision varchar(40) not null default 'approved',
    add column if not exists terms_reviewed_by varchar(120) not null default 'bootstrap.admin',
    add column if not exists terms_reviewed_at timestamptz not null default now(),
    add column if not exists superseded_by_source_version_id varchar(80) references source_version(source_version_id);

update source_version
set status = 'active'
where status = 'indexed'
  and exists (
      select 1
      from corpus_snapshot_source css
      where css.source_version_id = source_version.source_version_id
  );
