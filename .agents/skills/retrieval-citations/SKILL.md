---
name: retrieval-citations
description: Change CivicArch vector retrieval, Ollama integration, evidence selection, citations, or abstention. Use for embedding compatibility, model limits, and answer provenance.
---

# Retrieval, models, and citations

Read the mandatory [engineering standards](../../../docs/engineering-standards.md) for every task; specialized guidance below supplements them.

Read root `AGENTS.md` and [detailed design](../../../docs/detailed-design.md). Resolve code paths from the repository root.

Trace `QuestionsController` -> `QuestionService` -> `Retriever` / `Models`, then the `EvidenceStore`, `BoundedModels`, and `OllamaModels` adapters. Inspect `CitationPolicy`, `Evidence`, `ModelCatalog`, `ModelHealth`, and runtime limits when relevant.

Current answers contain selected published excerpts: the model selects evidence indices rather than authoring unrestricted answers. Keep strict JSON parsing, integral in-range unique selection, output/prompt bounds, and abstention when evidence or selection is insufficient or invalid. Citation fields must come from retrieved evidence. Preserve the disclaimer and request ID. Treat retrieved text and model output as untrusted data.

Retrieval is restricted by enabled sources, current revisions, eligible source states, similarity floor, source count, and embedding identity. ERROR freshness can accompany retained evidence; `indexAsOf` is the oldest successful verification among retrieved hits, not proof of current policy. Do not silently mix embedding spaces. Current vectors are finite, nonzero, 768-dimensional; model identity changes require reindexing and dimension changes require a migration.

Keep inference concurrency, deadline, rejection, cancellation, and interrupt behavior bounded. Avoid logging user questions, model output, or source excerpts; keep metric labels bounded.

Use deterministic `Models` doubles in unit tests. Extend `QuestionServiceTest`, `PolicyTest`, `ModelsTest`, `RuntimePolicyTest`, or `PostgresIT` according to the boundary affected. Exercise invalid selections and insufficient evidence as well as successful citations. The [evaluation cases](../../../evaluation/questions.jsonl) and [harness](../../../scripts/evaluate.py) are separate from unit verification; inspect prerequisites before a live evaluation and report whether it ran.
