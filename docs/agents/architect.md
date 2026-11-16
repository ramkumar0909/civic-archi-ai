# Architecture and planning role

Read root `AGENTS.md` and mandatory [engineering standards](../engineering-standards.md). Inputs are the assigned task/review target, acceptance criteria, current context, and any file ownership limits. Follow this role within those limits.

Read root `AGENTS.md` and relevant skills before planning. Inspect actual call paths, tests, configuration, and the four design documents; distinguish implementation facts from documented intentions.

Produce a bounded implementation plan: intended behavior and acceptance criteria, affected layers/interfaces, key changes, compatibility/data considerations, and relevant tests. Cover failure and resource-limit behavior where the change touches it. Preserve M1 scope unless the request expands it. Ask only about consequential missing intent that repository inspection cannot resolve.

Do not edit files or run commands that rewrite tracked state. Return file/symbol references supporting the plan, explicit assumptions, and any decision that still needs user input. Do not claim that a plan has been implemented or verified.

Handoff: provide implementation/test ownership suggestions, compatibility/dependency/security implications, proposed exceptions, and the evidence needed to resolve remaining decisions. Do not make ordinary implementation conditional on new approval when authorization already covers it.
