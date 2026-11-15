---
name: civicarch-delivery
description: Deliver CivicArch repository tasks from requirements and design through implementation, tests, security/governance review, documentation, and a review-ready handoff. Use for end-to-end coding work spanning multiple stages or subsystems.
---

# End-to-end coding delivery

Read root [AGENTS.md](../../../AGENTS.md), mandatory [engineering standards](../../../docs/engineering-standards.md), and the relevant subsystem skills. Paths in backticks are repository-relative. Follow the [coordinator role](../../../docs/agents/coordinator.md); perform stages in one session for small tasks. Custom agents and parallel execution are optional, not prerequisites.

1. Establish the requested behavior, observable acceptance criteria, constraints, and current working-tree baseline. Trace affected paths/tests/docs. Resolve discoverable facts from the repository and ask only about consequential missing intent.
2. For cross-layer or compatibility-sensitive work, use the architect role to state interfaces, failure behavior, resource bounds, migrations/reindexing, and validation. Do not write plans or decision documents for trivial changes; keep material rationale in the handoff or relevant existing design document.
3. Implement within authorized scope. If delegation is requested, assign explicit inputs, acceptance criteria, file ownership, and expected outputs; avoid overlapping edits. Integrate sequentially and preserve unrelated work.
4. Add meaningful regression coverage, run focused checks, then the required full gate. Serialize Maven runs in a shared checkout. Use real PostgreSQL/pgvector for SQL properties; fixtures for acquisition and deterministic model doubles for unit tests. Record unavailable Docker/models/network as specific verification gaps.
5. Review correctness and Java standards plus security/governance implications. Use reviewer/security-reviewer roles where useful; they do not edit. Resolve findings with implementation changes and rerun affected checks. Do not use a repeated review loop without progress or treat a missing external prerequisite as a code defect to bypass.
6. Update affected API/design/operational documentation and prepare a review-ready handoff: criteria met or unmet, changed behavior/files, commands/results, findings resolved/outstanding, compatibility/data effects, and remaining risks. Draft a PR description when requested; publishing requires authorization covering it.

Never edit applied migrations, mix embedding spaces, weaken gates, or assert that coverage/review certifies security. When a requested change needs incompatible schema/model behavior, surface the forward migration/reindex and existing-data implications before implementing that part. Do not perform destructive rebuilds without authorization. For sensitive findings follow SECURITY.md and keep exploit details private.

Use [task prompts](../../../docs/agentic-coding.md#prompts) to invoke individual stages or the full workflow. A task with required verification gaps is handed off with those gaps, not labeled fully verified or release-ready.
