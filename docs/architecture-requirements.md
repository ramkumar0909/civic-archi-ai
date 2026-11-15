# Architecture Requirements Specification

| Control | Value |
|---|---|
| Document ID / version | CA-ARS / 1.0 |
| Status | Draft for maintainer review; approval pending |
| Scope / date | Milestone 1 local backend / 15 November 2026 |
| Related documents | [Architecture definition](architecture-definition.md), [Detailed design](detailed-design.md), [API specification](api-specification.md) |

## 1. Purpose and conventions

This document defines measurable obligations and verification traceability, complementing the qualitative Architecture Definition Document. IDs are stable across revisions. PASS describes recorded evidence within its stated limits; PARTIAL identifies a concrete gap; NOT VERIFIED means no execution evidence exists. These statuses are not governance approval or a compliance determination.

## 2. Requirement catalogue

| ID | Requirement / acceptance criterion | Design reference | Verification / recorded status |
|---|---|---|---|
| CA-ING-01 | Exactly ten initial enabled source paths must be approved GOV.UK paths; duplicates, external URLs and unsupported topics must fail validation before fetching. | [Acquisition](detailed-design.md#3-acquisition-and-extraction) | AdaptersTest; ten live API inspections and smoke ingestion: PASS |
| CA-ING-02 | Identical content/policy/model identity must not create duplicate current chunks or invoke embeddings again. | [Revision publication](detailed-design.md#4-versioning-and-publication) | PostgresIT, IngestionServiceTest; live second run 10 unchanged: PASS |
| CA-ING-03 | Changed content must become current only after all chunks succeed; partial publication must roll back. | [Revision publication](detailed-design.md#4-versioning-and-publication) | PostgresIT revision and rollback assertions: PASS |
| CA-ING-04 | Withdrawn, redirected and empty sources must be excluded; transient failures retain prior content with ERROR freshness and separate attempt time. | [State and recovery](detailed-design.md#7-failure-handling-and-recovery) | Adapter/state/rollback tests: PASS for tested cases; comprehensive upstream fault combinations remain PARTIAL |
| CA-EVD-01 | Every returned citation must resolve to a retrieved current chunk; URLs, text and metadata must come from stored evidence. Target reference integrity: 100%. | [Retrieval and citations](detailed-design.md#5-retrieval-and-evidence-selection) | Citation tests and five independently checked live citations: PASS for exercised cases |
| CA-EVD-02 | No hits, oversized context, empty/invalid output or unknown/duplicate IDs must yield abstention with no citations; dependency failures must yield controlled errors. | [Retrieval and citations](detailed-design.md#5-retrieval-and-evidence-selection) | QuestionServiceTest, controller tests, unsupported live question: PASS |
| CA-EVD-03 | Evidence relevance, unsupported-query abstention and retrieval quality must be reviewed against at least 20 labelled cases; no invented latency/accuracy claims. | [Evaluation](detailed-design.md#8-verification-and-observability) | Dataset and harness exist; full benchmark and human review: NOT VERIFIED |
| CA-MOD-01 | Indexed and query embeddings must have matching resolved digest and dimension; reject nonfinite, zero or non-768 vectors. | [Model compatibility](detailed-design.md#5-retrieval-and-evidence-selection) | ModelsTest, PostgresIT: PASS; changing dimensions requires migration |
| CA-API-01 | Reject blank/>4000-character questions, maxSources outside 1–10, malformed/unknown/trailing JSON and bodies above configured cap (32768 bytes default). | [API request](api-specification.md#2-question-request) | QuestionsControllerTest: PASS |
| CA-API-02 | Return documented outcomes/statuses, opaque request IDs and safe Problem Details; never expose model prompts or stack traces. | [Responses](api-specification.md#3-question-response), [Errors](api-specification.md#4-errors) | Controller tests: PASS for documented test cases |
| CA-SEC-01 | Acquisition must use HTTPS www.gov.uk and registry paths, follow no redirects, cap responses at 2 MB and limit to one request/second by default. Retry 429/5xx at most three total attempts. | [Acquisition](detailed-design.md#3-acquisition-and-extraction) | AdaptersTest and code inspection: PASS; conditional fetching/publisher-specific checks remain PARTIAL |
| CA-SEC-02 | Ingest public data only; credentials remain environment-injected, with localhost binding and health/info-only Actuator exposure. No default content logging. | [Security view](architecture-definition.md#7-security-architecture-and-residual-risks) | Configuration review/basic repository scanner: PASS within basic scan limits; independent audit NOT VERIFIED |
| CA-SEC-03 | Bound question concurrency and model work; default two workers, zero queued model tasks, 60-second deadline per model operation and cancellation. | [Runtime policy](detailed-design.md#6-configuration-and-resource-bounds) | ModelsTest timeout/saturation assertions: PASS; no full-request SLA asserted |
| CA-OPS-01 | Health must distinguish liveness, DB readiness and model availability; inference outages must not fabricate evidence. | [Health](api-specification.md#5-health-and-operation) | ModelsTest/PostgresIT and live health UP: PASS for exercised cases |
| CA-QLT-01 | Java 25 clean verify must pass unit/HTTP/integration tests, style and coverage: ≥80% lines, ≥70% branches, ≥90% citation-policy branches. | [Verification](detailed-design.md#8-verification-and-observability) | Latest local clean verify: PASS; remote CI NOT VERIFIED |
| CA-GOV-01 | Display independent-project disclaimer and separate GPL software licensing from applicable OGL attribution. | [Governance](architecture-definition.md#8-governance-and-change) | README/NOTICE/API review: PASS; third-party licensing review remains PARTIAL |

## 3. Verification evidence and open gaps

Recorded on 15 November 2026: JDK 25.0.2, Maven Wrapper 3.9.12, macOS arm64, Docker Desktop, PostgreSQL 17/pgvector and local Ollama. Latest package-rename verification passed 22 unit/HTTP tests and one integration scenario, Checkstyle, Spotless and coverage gates. Reports are regenerated under target/surefire-reports, target/failsafe-reports and target/site/jacoco; this document does not replace those reports.

The earlier packaged live smoke run used qwen3:4b and nomic-embed-text:v1.5 (768 dimensions): first ingestion changed all ten sources; second ingestion reported ten unchanged, no skipped/failed sources; health was UP. A technology-choice question returned five citations whose exact text and URLs matched current database chunks. A bank-balance question abstained. This two-question smoke test is not an accuracy benchmark and was not rerun solely for subsequent namespace/style refactors.

Open evidence: full 20-case evaluation and faithfulness review; p50/p95 latency in a specified environment; remote CI; dependency vulnerability scanning; comprehensive secret audit; clean-clone onboarding; public deployment assurance. Source-policy gaps include HTTP validators, comprehensive publisher checks and page-specific third-party licence review. No percentage savings, ROI, uptime or production-readiness claims are authorized by current evidence.

## 4. Evaluation method and change control

Keep [labelled cases](../evaluation/questions.jsonl) and [evaluation harness](../scripts/evaluate.py) as executable assets. Supported queries have expected source keys; unsupported, ambiguous, misleading and injection queries require reviewed abstention expectations. Record model digests, registry/index timestamp, thresholds, hardware and software alongside results.

The current harness reports selected-citation source hits, metadata presence, failures and latency. Metadata presence is not independent chunk resolution, and selected-citation hits are not full retrieval_hit_at_k. Independently resolve citation IDs against retrieved/stored chunks for citation_integrity_rate; review unsupported cases for abstention_correctness; capture raw retrieval hits to measure retrieval_hit_at_k. Human reviewers assess context and support separately. Automated semantic judging, if added, must be labelled experimental.

Requirement changes must retain IDs or mark them superseded, update design traceability and name the verification evidence. Maintainer approval remains pending; no formal architecture compliance review has been recorded.
