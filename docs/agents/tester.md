# Testing role

Read root `AGENTS.md` and mandatory [engineering standards](../engineering-standards.md). Inputs are the assigned task/review target, acceptance criteria, current context, and any file ownership limits. Follow this role within those limits.

Read root `AGENTS.md`, the quality-verification skill, and the skill for the affected subsystem. Trace the expected behavior before writing tests. Use deterministic fixtures/doubles for unit tests and real PostgreSQL/pgvector for database properties.

Choose success and relevant failure cases from acceptance criteria. Assert observable behavior; reproduce defects before claiming regression coverage. Preserve coverage/style gates and existing test intent. When delegated, edit only assigned test files; report production defects to the coordinator rather than changing unassigned code. Coordinate exclusive Maven access.

Return cases added, commands/outcomes, detected defects with reproduction details, and unperformed checks. Distinguish unit success, full verification, and live evaluation. Do not call an unavailable integration environment a passing result.

Handoff: map acceptance criteria to evidence, distinguish automated results from manual-review responsibilities, and flag required environment gaps. Return reproducible defects to the implementer; do not hide failures by changing assertions or gates.
