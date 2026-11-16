# Agentic coding with Codex and GitHub Copilot

This repository supports Codex and GitHub Copilot. [AGENTS.md](../AGENTS.md) supplies shared instructions; [engineering standards](engineering-standards.md) define mandatory Java practices, security constraints, governance, and evidence requirements. Every role and skill loads them. [Agent Skills](https://agentskills.io/specification) provides the reusable workflow format. This setup adds no application runtime agent framework.

## Discovery and use

| Provider | Instructions | Skills | Native agents | Task prompts |
| --- | --- | --- | --- | --- |
| Codex | Root AGENTS.md requires engineering standards | `.agents/skills/` | `.codex/agents/*.toml` | Explicitly read `docs/prompts/` templates or invoke civicarch-delivery |
| GitHub Copilot | `.github/copilot-instructions.md` includes essentials and requires both shared documents | `.agents/skills/` | `.github/agents/*.agent.md` | `.github/prompts/*.prompt.md` routes to shared templates |

Open the repository root. In Codex, ask to use a named custom agent or invoke `$civicarch-delivery`; explicitly attach/read a SKILL.md if discovery is unavailable. In Copilot, select a custom agent or reference/select a prompt in supported IDEs. Prompt files are invocation-specific; they are not automatically applied instructions. Copilot CLI/cloud/IDE capabilities and agent/skill discovery depend on client/version. Reload a session after configuration changes where needed. Unsupported surfaces can read shared roles/skills/prompts directly.

Models, tools, and permissions inherit from the session. No adapter changes sandbox settings; non-editing roles are constrained by instructions rather than a separately configured sandbox. Defining a role does not guarantee automatic delegation. For small tasks, one assistant performs all applicable stages. If delegation is requested, assign bounded tasks with explicit file ownership, context, and expected outputs; avoid overlapping edits and serialize Maven runs.

## Agents and handoffs

Each shared role has a matching `civicarch-` native agent for both providers.

| Role | Responsibility and output |
| --- | --- |
| [coordinator](agents/coordinator.md) | Acceptance criteria, workflow, integration, evidence, final review-ready handoff |
| [architect](agents/architect.md) | Read-only design, interfaces, compatibility, tradeoffs, verification plan |
| [implementer](agents/implementer.md) | Scoped implementation, regression coverage, behavior/documentation changes |
| [tester](agents/tester.md) | Assigned test changes, reproducible defects, actual gate results and gaps |
| [reviewer](agents/reviewer.md) | Read-only correctness/Java/architecture review with supported findings |
| [security-reviewer](agents/security-reviewer.md) | Read-only security, dependency/licence, compatibility, exception review |

Architect hands implementation boundaries to the coordinator/implementer; implementer hands intended behavior to tester/reviewers; findings return to the implementer; coordinator verifies the combined result. Resolve defects and rerun affected checks without creating approval loops for routine authorized work. Required missing environment checks remain explicit gaps.

## Skills

| Skill | Coverage |
| --- | --- |
| [civicarch-delivery](../.agents/skills/civicarch-delivery/SKILL.md) | Requirements → design → implementation → tests → reviews → documentation → handoff |
| [civicarch-development](../.agents/skills/civicarch-development/SKILL.md) | Java/Spring, API, configuration, architecture |
| [govuk-ingestion](../.agents/skills/govuk-ingestion/SKILL.md) | Registry, acquisition, extraction, chunking, ingestion |
| [retrieval-citations](../.agents/skills/retrieval-citations/SKILL.md) | Models, evidence, citations, abstention |
| [postgres-persistence](../.agents/skills/postgres-persistence/SKILL.md) | JDBC/pgvector, migrations, revisions, transactions, locks |
| [quality-verification](../.agents/skills/quality-verification/SKILL.md) | Regression, style, architecture, coverage, integration, evaluation |

Shared Java/governance requirements apply across every skill. Automated checks cover only part of these requirements; agents/reviewers assess resource ownership, concurrency, compatibility, and other design/security questions. Reviews and coverage do not certify security.

## Prompts

Replace placeholders with task details, or provide those details alongside the selected template. Codex can read the shared template explicitly; Copilot prompt adapters have the same basename under `.github/prompts/` and link to the same content.

- [Plan a feature](prompts/plan-feature.md)
- [Deliver a task end to end](prompts/deliver-task.md)
- [Implement a feature](prompts/implement-feature.md)
- [Investigate a bug](prompts/investigate-bug.md)
- [Refactor](prompts/refactor.md)
- [Add regression tests](prompts/add-regression-tests.md)
- [Review a change](prompts/review-change.md)
- [Review security/governance](prompts/security-review.md)
- [Prepare PR text](prompts/prepare-pr.md)

Examples:

- API: “Use civicarch-architect to plan a validation change through QuestionsController, QuestionService, and error mapping; include compatibility and acceptance tests.”
- Ingestion: “Use civicarch-tester and govuk-ingestion to add deterministic response-size boundary coverage, without changing production behavior.”
- Retrieval: “Use civicarch-reviewer and retrieval-citations to review evidence selection and abstention in this diff; do not edit.”
- Persistence: “Use civicarch-delivery to implement the requested schema change with a forward migration, existing-data compatibility, and real database verification.”
- End to end: “Read docs/prompts/deliver-task.md and deliver this bug fix. Keep work in one session and report acceptance evidence and remaining gaps.”

## Verification and boundaries

Validate skill YAML, native-agent TOML/YAML, Copilot prompt metadata, local links, and routing to mandatory shared guidance. Run `git diff --check` and `python3 scripts/check_repository.py`. Static checks do not establish live provider discovery or behavior; report those only when actually tested. Application changes follow the existing Java/Docker quality gates; documentation-only changes need no Java build.

Walk through representative API, ingestion, retrieval/citation, and migration tasks, including a security finding, missing Docker, and an incompatible schema/model change. Confirm the expected skill/role and failure handling without executing unrelated feature changes. Actual delivery reports met/unmet criteria, check outcomes, outstanding findings, data/compatibility effects, and documentation updates.

The default endpoint is a review-ready handoff. PR text can be prepared locally; commits, publication, deployment, external messages, permission changes, and destructive data operations require user authorization covering those actions. Do not claim release readiness while required verification is unperformed.

## References

- [AGENTS.md](https://agents.md/) and [Agent Skills specification](https://agentskills.io/specification)
- [Codex skills](https://learn.chatgpt.com/docs/build-skills) and [custom agents](https://learn.chatgpt.com/docs/agent-configuration/subagents)
- [Copilot skills](https://docs.github.com/en/copilot/concepts/customizations/agent-skills), [instructions](https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions), [custom agents](https://docs.github.com/en/copilot/reference/custom-agents-configuration), and [prompt files](https://docs.github.com/en/copilot/concepts/prompting-and-context/prompt-files)
- [Java practices and governance references](engineering-standards.md#authoritative-references)
