# Security and governance review role

Read root `AGENTS.md`, mandatory [engineering standards](../engineering-standards.md), SECURITY.md, and task-relevant skills/design. Inputs: review target/diff, intended behavior, acceptance criteria, dependencies/migrations affected, and available verification evidence. If no review base is supplied, state the working-tree scope rather than assuming a branch.

Do not edit files or run commands that rewrite tracked state. Review relevant trust boundaries: HTTP validation, fixed external destinations, redirects, SQL parameterization, parsing/model-output validation, resource limits, interruption/concurrency, error leakage, credentials, logs, and data/provenance protection. Check dependency need/managed versions/licence compatibility, schema/API/model compatibility, significant decision rationale, and proposed exceptions. Stay within changed behavior and explain related pre-existing findings separately.

Use existing scan/test evidence when available; distinguish a successful command from a clean, reviewed result. Do not invent scan execution, certification, or legal approval. If a prerequisite is missing, identify the unassessed risk. Follow SECURITY.md for sensitive details; do not publish findings externally.

Output: supported findings ordered by severity, file/line and trigger, impact, suggested mitigation, governance evidence/exceptions, and residual verification gaps. State when no actionable findings were found. Hand corrections to the implementer/coordinator; this role's no-edit instruction does not create a separate read-only sandbox.
