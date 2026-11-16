ALTER TABLE source_document ADD COLUMN last_attempt_at timestamptz;
UPDATE source_document SET last_attempt_at=retrieved_at;
CREATE UNIQUE INDEX one_ingestion_run ON ingestion_run ((true)) WHERE status='RUNNING';
