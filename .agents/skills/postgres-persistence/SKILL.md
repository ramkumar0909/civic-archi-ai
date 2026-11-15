---
name: postgres-persistence
description: Change CivicArch PostgreSQL/pgvector JDBC persistence, Flyway migrations, revision publication, freshness, or ingestion locking. Use for schema and embedding-index compatibility changes.
---

# PostgreSQL, revisions, and migrations

Read the mandatory [engineering standards](../../../docs/engineering-standards.md) for every task; specialized guidance below supplements them.

Read root `AGENTS.md` and the persistence sections of [detailed design](../../../docs/detailed-design.md). Resolve code paths from the repository root.

Inspect `RevisionRepository`, `Retriever`, `EvidenceStore`, `src/main/resources/db/migration`, and `PostgresIT`. Use parameterized JDBC and existing transaction conventions; domain/application code must not depend on JDBC or infrastructure implementations. Do not introduce JPA.

Append a new versioned migration for schema changes; never rewrite an applied migration. Explain compatibility with existing data, deployment order, and any backfill/reindex needed. Current pgvector dimensions are 768. Model identity must stay aligned with stored vectors and retrieval filters.

Preserve atomic publication of revisions/chunks/current revision pointers and the distinction between content retrieval time and failed attempt time. Check unchanged refreshes, ERROR retention, excluded source states, and model mismatch handling when relevant. One RUNNING ingestion is enforced in the database; do not bypass that lock or automatically steal ownership. Recovery requires confirming no ingestion is active and targeting the abandoned run per README.

Test SQL, migrations, uniqueness, transactions, and vector queries against real PostgreSQL/pgvector through `PostgresIT`; mocks cannot establish these properties. Run `./mvnw clean verify` with Docker reachable for acceptance. If Docker is unavailable, record the unverified database behavior. Do not drop local volumes or rebuild indexes without task authorization for the data loss involved.

Reference: [database migrations](../../../src/main/resources/db/migration) and [local setup/recovery](../../../README.md).
