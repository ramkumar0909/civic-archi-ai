# Contributing

Use JDK 25 and run ./mvnw verify with Docker available. Unit tests use deterministic models; do not require Ollama. Keep domain policy independent of framework libraries. Add regression tests for provenance, extraction and citation changes. Do not introduce private data, credentials or M2+ functionality.

Use respectful, constructive communication; harassment is unacceptable. Report sensitive vulnerabilities privately to the repository maintainer rather than in public issues. Contributions follow the existing GPL-3.0 licence; source material retains its own licence.

## Mandatory Java quality gates

`./mvnw clean verify` on JDK 25 runs Surefire unit/HTTP tests, Failsafe real PostgreSQL/pgvector tests, Spotless, Checkstyle and JaCoCo checks. Docker must be reachable. `./mvnw test` is a fast unit-only check and does not establish release acceptance.

Use `./mvnw spotless:apply` to format Java; Checkstyle owns explicit imports, braces, naming and unused-import hygiene. Coverage combines unit and integration execution in `target/jacoco.exec`; reports are in `target/site/jacoco/index.html` and `jacoco.xml`. Minimums are 80% line and 70% branch coverage overall, and 90% branch coverage for CitationPolicy. Only the application launcher is excluded. Add meaningful regression cases rather than lowering thresholds or excluding adapters.

CI uploads test, coverage, style and dependency reports even on failure. Application/domain boundary tests prevent dependencies on infrastructure implementations; domain code remains framework-free. Persistence stays JDBC with Flyway-managed SQL, not JPA.
