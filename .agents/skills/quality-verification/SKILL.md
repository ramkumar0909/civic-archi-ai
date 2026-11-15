---
name: quality-verification
description: Verify CivicArch changes and investigate test, style, architecture, coverage, or build failures. Use to select regression tests and distinguish unit checks, integration gates, and live evaluations.
---

# Tests and verification

Read the mandatory [engineering standards](../../../docs/engineering-standards.md) for every task; specialized guidance below supplements them.

Read root `AGENTS.md`, [CONTRIBUTING](../../../CONTRIBUTING.md), `pom.xml`, and `.github/workflows/ci.yml`. Resolve paths from the repository root. Use JDK 25; Docker is required for integration verification.

Choose tests by changed behavior:

| Boundary | Existing tests |
| --- | --- |
| HTTP contracts and limits | `QuestionsControllerTest` |
| Orchestration, evidence, citations | `QuestionServiceTest`, `PolicyTest` |
| Fetching, parsing, extraction | `AdaptersTest`, `ExtractionTest` |
| Ingestion transitions | `IngestionServiceTest` |
| Model bounds and runtime configuration | `ModelsTest`, `RuntimePolicyTest` |
| Layer dependencies | `ArchitectureTest` |
| SQL, migrations, pgvector, locking | `PostgresIT` |

Add regression cases that fail for the defect and assert meaningful observable behavior. Unit tests use deterministic models and local fixtures; avoid live network, real retry sleeps, or an Ollama dependency. Run the appropriate `./mvnw -Dtest=TestClass test` first. Never run Maven concurrently in the same checkout because it shares reports and build output.

`./mvnw test` is unit-only. `./mvnw clean verify` is the full acceptance gate: Surefire, Failsafe real database tests, Spotless, Checkstyle, and combined JaCoCo checks. Keep coverage at 80% line / 70% branch overall and 90% CitationPolicy branches; do not lower gates, skip relevant failures, or exclude adapters to pass. Reports are under `target/surefire-reports`, `target/failsafe-reports`, and `target/site/jacoco`.

For Java formatting use `./mvnw spotless:apply` and inspect the diff. Also run `git diff --check` and `python3 scripts/check_repository.py`. CI additionally runs an OWASP dependency check; distinguish its network-dependent result from local test results. For documentation/configuration-only work, validate syntax/references and repository hygiene; Java tests are needed only if application/build behavior is affected.

Live smoke tests and the evaluation harness have separate service/model prerequisites. Inspect [evaluation harness](../../../scripts/evaluate.py) and [requirements evidence](../../../docs/architecture-requirements.md) before running or updating evidence. Report exact commands, outcomes, and blocked checks; do not infer execution from historical reports.

Include a manual engineering/governance review using the shared standards: tooling does not establish exception/resource correctness, thread safety, licence compatibility, or secure behavior. Track unresolved findings separately from test results and identify proposed exceptions rather than declaring them accepted.
