# Review security and governance

Review target: <diff, commit/base, or working tree>
Intended behavior: <acceptance criteria>

Read AGENTS.md, docs/engineering-standards.md, and relevant skills. Use the security-reviewer role. Do not edit files. Review changed trust boundaries, resource/exception/concurrency handling, SQL/input/model-output safety, safe logs, dependency/licence impact, compatibility, and proposed exceptions. Return supported findings with severity, file/line, trigger, impact, mitigation, and gaps. Do not claim scans or certification that were not performed; keep sensitive details private.
