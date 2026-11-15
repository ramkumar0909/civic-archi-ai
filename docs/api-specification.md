# API Specification

| Control | Value |
|---|---|
| Document ID / version | CA-API / 1.0 |
| Status | Draft for maintainer review; approval pending |
| Scope / date | M1 REST contract / 15 November 2026 |
| Related documents | [Architecture definition](architecture-definition.md), [Requirements](architecture-requirements.md), [Detailed design](detailed-design.md) |

## 1. Conventions

Base URL defaults to http://127.0.0.1:8080. Request and response JSON uses camelCase and UTF-8. Timestamps use ISO 8601. UUIDs are opaque internal references. This specification is the API contract; no machine-readable OpenAPI file or Swagger UI is currently implemented.

The request filter generates an opaque UUID, returns it in X-Request-Id and uses it as response requestId. Clients cannot choose a trusted correlation ID. No authentication is provided for this local-only interface. No ingestion/admin write endpoint exists.

## 2. Question request

`POST /api/questions` with Content-Type application/json and JSON response.

```json
{
  "question": "What guidance applies to choosing tools and technology?",
  "maxSources": 5
}
```

| Field | Type | Rules |
|---|---|---|
| question | string | Required, nonblank, at most 4000 Java string characters |
| maxSources | integer or null | Optional; omitted/null defaults to 5; accepted range 1–10 |

Unknown fields, malformed JSON and trailing JSON values are rejected. The request-body cap defaults to 32768 bytes and is configurable. A configured runtime maxSources can further reduce the retrieval count; the API validation range stays 1–10. maxSources limits chunks rather than unique documents, so multiple citations may point to one source page.

```sh
curl -H 'Content-Type: application/json' http://localhost:8080/api/questions \
  -d '{"question":"What guidance applies to choosing technology?","maxSources":5}'
```

## 3. Question response

HTTP 200 represents either ANSWERED or INSUFFICIENT_EVIDENCE. Dependency failures use HTTP errors. Success contract:

| Field | Type | Meaning |
|---|---|---|
| question | string | Original question |
| answer | string | Exact published excerpts with application-generated reference labels, or fixed abstention message |
| outcome | string | ANSWERED or INSUFFICIENT_EVIDENCE |
| citations | array | Selected evidence metadata; empty on abstention |
| retrieval.returnedChunks | integer | Retrieved chunks, including those not selected as citations |
| retrieval.indexAsOf | timestamp or null | Present on answered results; oldest successful source verification time among retrieved hits; omitted on abstention |
| requestId | UUID string | Matches X-Request-Id |
| disclaimer | string | Independent-project and informational-use notice |

Citation contract:

| Field | Type | Origin / meaning |
|---|---|---|
| citationId | string | Application-assigned S1, S2, etc., from hit position; may have gaps |
| sourceKey | string | Registry key |
| title | string or null | Stored source title |
| url | string | Canonical https://www.gov.uk source URL |
| contentId | string or null | Published Content API identity |
| publishedUpdatedAt | timestamp or null | Published update time, honestly nullable |
| retrievedAt | timestamp | Successful source verification time for the stored evidence |
| section | string | Extracted heading/section label |
| chunkId | UUID string | Stable internal identity for the actual retrieved chunk |
| excerpt | string | Exact stored retrieved text |
| freshness | string | ACTIVE or ERROR; ERROR means previous indexed content survived a failed refresh |

An answered response composes citation-labelled excerpts; it does not invent a prose conclusion or certification. Citation IDs and all source metadata come from retrieval hits. Similarity and reference integrity do not establish relevance, completeness or current policy applicability.

The following is an **illustrative abstention**, with a placeholder request UUID. It contains no invented source records:

```json
{
  "question": "What is my bank account balance?",
  "answer": "I could not find sufficient support for that claim in the indexed GOV.UK guidance.",
  "outcome": "INSUFFICIENT_EVIDENCE",
  "citations": [],
  "retrieval": {"returnedChunks": 0},
  "requestId": "00000000-0000-0000-0000-000000000000",
  "disclaimer": "CivicArch AI is an independent open-source project. It is not affiliated with, endorsed by, or an official service of the UK government. Its answers are informational and do not constitute a compliance determination."
}
```

Abstention also occurs with nonzero hits when evidence selection is empty, output is invalid, references are unknown/duplicate, or context/output exceeds configured bounds. No fallback to general model knowledge occurs.

## 4. Errors

Errors use application/problem+json and Spring Problem Details: title, status, detail, requestId, and framework-provided type/instance where serialized. Clients must use status for broad handling rather than depend on incidental exception messages. Standard type is about:blank when present. Prompts, source bodies, dependency response bodies and stack traces are excluded from error details.

| HTTP status | Meaning |
|---|---|
| 400 | Invalid question, maxSources, JSON syntax/shape or trailing content |
| 405 | Unsupported method for the question endpoint |
| 413 | Request exceeds configured byte cap |
| 415 | Unsupported media type; use application/json |
| 429 | Question/model capacity exhausted; retry later |
| 503 | Database/model dependency failure, model deadline or incompatible embedding index |

Illustrative error; UUID is a placeholder:

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "Check question and maxSources.",
  "requestId": "00000000-0000-0000-0000-000000000000"
}
```

No automatic Retry-After guarantee exists. Clients should bound their own retries. A successful health check does not guarantee the next inference will complete.

## 5. Health and operation

| Endpoint | Semantics |
|---|---|
| GET /actuator/health | Aggregate health, including configured model availability and DB; detailed internals hidden |
| GET /actuator/health/liveness | Application liveness probe |
| GET /actuator/health/readiness | Readiness state and database usability; does not include model health in this group |

Health returns its Spring Actuator status and can return HTTP 503 when DOWN. Model health checks model presence in the Ollama catalog, not a full generation. Actuator exposure is restricted to health/info; there is no public env/config dump or metrics endpoint. Ingestion is an explicit non-web profile; commands, model setup and SQL diagnostics are in [README](../README.md).

## 6. Compatibility and verification

Changes to validation, fields or status semantics must update this contract and controller tests. No API versioning scheme is currently established. Reference integrity is regression-tested; independently verified live citations and remaining evaluation limitations are recorded in [requirements](architecture-requirements.md#3-verification-evidence-and-open-gaps). No benchmark or availability SLA is asserted.
