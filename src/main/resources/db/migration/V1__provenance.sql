CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE source_document (
 id uuid PRIMARY KEY, source_key text NOT NULL UNIQUE, base_path text NOT NULL UNIQUE,
 canonical_url text NOT NULL, content_id text, title text, document_type text, locale text,
 public_updated_at timestamptz, retrieved_at timestamptz, status text NOT NULL,
 payload_sha256 text, content_sha256 text, extraction_version text,
 current_revision_id uuid, last_success_at timestamptz, last_error_summary text
);
CREATE TABLE document_revision (
 id uuid PRIMARY KEY, source_document_id uuid NOT NULL REFERENCES source_document(id),
 revision_hash text NOT NULL, extracted_text text NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(source_document_id, revision_hash)
);
ALTER TABLE source_document ADD FOREIGN KEY(current_revision_id) REFERENCES document_revision(id);
CREATE TABLE document_chunk (
 id uuid PRIMARY KEY, revision_id uuid NOT NULL REFERENCES document_revision(id),
 ordinal integer NOT NULL, heading_path text NOT NULL, text text NOT NULL,
 token_estimate integer NOT NULL, content_sha256 text NOT NULL, embedding_model_id text NOT NULL,
 embedding_vector vector(768) NOT NULL, UNIQUE(revision_id,ordinal)
);
CREATE INDEX chunk_revision ON document_chunk(revision_id);
CREATE INDEX chunk_vector ON document_chunk USING hnsw(embedding_vector vector_cosine_ops);
CREATE TABLE ingestion_run (
 id uuid PRIMARY KEY, started_at timestamptz NOT NULL DEFAULT now(), completed_at timestamptz,
 status text NOT NULL, attempted integer NOT NULL DEFAULT 0, changed integer NOT NULL DEFAULT 0,
 unchanged integer NOT NULL DEFAULT 0, skipped integer NOT NULL DEFAULT 0, failed integer NOT NULL DEFAULT 0
);
