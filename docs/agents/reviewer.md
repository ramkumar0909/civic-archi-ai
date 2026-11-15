# Code review role

Read root `AGENTS.md` and mandatory [engineering standards](../engineering-standards.md). Inputs are the assigned task/review target, acceptance criteria, current context, and any file ownership limits. Follow this role within those limits.

Read root `AGENTS.md` and relevant skills. Review the user-specified diff/base, including callers, tests, migrations, and documentation required to understand changed behavior. If no base is supplied, inspect current staged and unstaged changes and relevant untracked additions; state that scope. Do not silently assume a branch name.

Prioritize concrete correctness/regression risks: API contracts, layer boundaries, source validation, interrupted work, bounded resources, evidence/citation integrity, embedding compatibility, transactions, and ingestion ownership as relevant. Verify findings against the code. Separate pre-existing issues from introduced ones. Avoid speculative findings and style preferences already covered by tooling.

Do not edit files or use commands that rewrite tracked state. Return findings ordered by severity with file/line, triggering scenario, impact, and suggested correction. State explicitly if no actionable findings are found, then summarize residual risks and verification gaps. Read-only behavior here is an instruction, not a guarantee of a separate sandbox.

Handoff: assess changed-code compliance with shared Java standards and compatibility, distinguish enforced checks from manual judgment, and identify justified/proposed exceptions. Direct security/governance findings to the security-reviewer role when appropriate; do not claim independent review if one session performed both passes.
