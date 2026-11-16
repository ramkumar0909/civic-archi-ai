---
name: civicarch-development
description: Develop and refactor CivicArch Java/Spring code, HTTP APIs, configuration, and architecture. Use for repository implementation tasks outside the more specialized ingestion, retrieval, and persistence workflows.
---

# Java, Spring, and API development

Read the mandatory [engineering standards](../../../docs/engineering-standards.md) for every task; specialized guidance below supplements them.

Read root `AGENTS.md` and `CONTRIBUTING.md`. Resolve paths below from the repository root.

Trace the request or job from its entry point to domain ports and adapters before editing. Use the versions in `pom.xml`, Java 25, existing constructor injection, explicit imports, and the repository's Google Java Format / Checkstyle configuration. Do not copy APIs from a different Spring/Jackson major version; this repository uses `tools.jackson`.

For HTTP work inspect `QuestionsController`, `Errors`, `RequestLimits`, `JsonConfiguration`, and `QuestionService`. Preserve validation, request IDs, status/error mapping, limits, and response provenance unless the task changes them. Document intended contract changes in [API specification](../../../docs/api-specification.md).

For configuration changes trace `RuntimePolicy`, `PolicyProperties`, and `application.yml`, including startup validation and environment overrides. Keep resource limits configurable and validated. Preserve the non-web, one-time ingestion profile.

Keep policies/ports in domain and orchestration in application. Consult the specialized skills when crossing ingestion, retrieval/model, or JDBC boundaries. Tests should exercise public behavior; use `QuestionsControllerTest`, `RuntimePolicyTest`, `PolicyTest`, and `ArchitectureTest` as relevant examples. Run focused tests first and the full gate when required by the change; use the quality-verification skill for gate details.

Reference: [detailed design](../../../docs/detailed-design.md). Return behavior changes, affected boundaries, verification, and remaining risks.

Before handoff, review changed Java for null/ownership contracts, exception causes, resource cleanup on failure paths, concurrency and cancellation, API compatibility, and relevant security constraints. Document significant design choices without adding speculative abstractions.
