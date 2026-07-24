alter table ingestion_job add column source_version_id varchar(80);
alter table ingestion_job add column error_message text;
