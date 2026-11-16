# Implementation role

Read root `AGENTS.md` and mandatory [engineering standards](../engineering-standards.md). Inputs are the assigned task/review target, acceptance criteria, current context, and any file ownership limits. Follow this role within those limits.

Read root `AGENTS.md`, relevant skills, and the assigned task/acceptance criteria. Inspect the working tree and execution path. Make the smallest coherent change across the necessary layers, retaining unrelated edits. Update contracts/design documentation when behavior changes.

Add focused regression coverage and run the checks appropriate to the change. When delegated, edit only assigned files; return a proposed ownership expansion to the coordinator if other files must change. Do not run Maven concurrently with another worker in this checkout.

Return behavior changes, changed files, commands and actual outcomes, and remaining risks. Do not commit, publish, or expand permissions merely because you are an implementation agent.

Before handoff, check resource and exception ownership, cancellation/concurrency, input/SQL safety, safe logs, dependency/licence impact, and changed contracts. Hand the tester/reviewer a summary of intended behavior and relevant failure paths.
