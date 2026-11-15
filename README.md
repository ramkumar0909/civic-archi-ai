# CivicArch AI

Local-first UK government guidance retrieval with verifiable source excerpts. **Java quality gates and PostgreSQL integration pass; see the [Architecture Requirements Specification](docs/architecture-requirements.md#3-verification-evidence-and-open-gaps) for live verification results and remaining M1 limitations.**

CivicArch AI is an independent open-source project. It is not affiliated with, endorsed by, or an official service of the UK government. Its answers are informational and do not constitute a compliance determination.

## Prerequisites and build

JDK 25, Docker with Compose, and Ollama. Allow several GB for models and at least 8 GB available memory as a starting estimate; performance is not benchmarked. On macOS explicitly select Java because Homebrew Maven may otherwise use another JDK:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw --version
./mvnw verify
```

Full verification requires Docker and runs real pgvector integration tests. `./mvnw test` runs only unit tests and is not the release gate.

## Local setup

```sh
cp .env.example .env
# Edit DB_PASSWORD in .env; do not commit it.
set -a
. ./.env
set +a
docker compose up -d postgres
ollama serve
```

Run `ollama serve` in a separate terminal only if it is not already running. Alternatively use `docker compose --profile ollama up -d ollama` and execute model commands inside that container.

```sh
ollama pull qwen3:4b
ollama pull nomic-embed-text:v1.5
./mvnw package -DskipTests
java -jar target/civicarch-ai-0.1.0-SNAPSHOT.jar --spring.profiles.active=ingest
java -jar target/civicarch-ai-0.1.0-SNAPSHOT.jar
```

The package command is a convenience, not verification. The ingestion profile is a non-web one-time job. API starts on localhost:8080. Selected embedding model must produce 768-dimensional nonzero finite vectors. Model substitution requires reindexing; changing dimensions requires a new migration. Do not mutate model tags in place while reusing an index.

```sh
curl http://localhost:8080/actuator/health
curl -H 'Content-Type: application/json' http://localhost:8080/api/questions \
  -d '{"question":"What guidance should I consider when choosing technology?","maxSources":5}'
docker compose exec postgres psql -U civicarch -c 'select source_key,status,last_success_at,last_error_summary from source_document;'
```

Exactly ten source paths are configured in config/sources.yml. Their API schemas were inspected on 15 November 2026; the [Detailed Design Specification](docs/detailed-design.md#9-source-validation-evidence) records identifiers. The Service Standard overview has an empty API body and was replaced by point 5. Source validation is distinct from the recorded live smoke test; see the requirements evidence for its scope.

## Troubleshooting and retention

- Missing model: `ollama list`, then pull the configured tag. Health reports missing models as down.
- Wrong dimension/model: do not reuse the old embedding space. Create a new index with an explicit migration or intentionally rebuild local data.
- API timeout or null content: inspect source state; empty/withdrawn/redirected sources are excluded. Transient failures retain the old revision with ERROR freshness.
- DB/migration failures: inspect `docker compose logs postgres`, credentials and port 5432. Never edit applied migrations.
- Docker unavailable: start its runtime and check `docker version`; integration checks must not be treated as passed.

`docker compose down` preserves named volumes. `docker compose down -v` intentionally deletes local database and optional model volumes. Historical revisions are retained indefinitely in M1; there is no automatic pruning.

## Project information

Design documentation consists of four tailored TOGAF-aligned deliverables:

- [Architecture Definition Document — HLD](docs/architecture-definition.md)
- [Architecture Requirements Specification](docs/architecture-requirements.md)
- [Detailed Design Specification — LLD](docs/detailed-design.md)
- [API Specification](docs/api-specification.md)

These documents are drafts for maintainer review; no TOGAF certification or formal approval is claimed. Software retains the repository's existing GPL-3.0 licence; GOV.UK text is separately attributed under applicable OGL terms in NOTICE.md. Scope remains Milestone 1.

## Quality reports and operational recovery

The build enforces Checkstyle plus Spotless and JaCoCo coverage gates. Open `target/site/jacoco/index.html` after full verification. See CONTRIBUTING.md for thresholds and test phases. Configuration defaults and validated resource bounds are listed under `civicarch.runtime` and `civicarch.fetch` in application.yml; override them through Spring configuration/environment variables. The database schema remains 768-dimensional and a conflicting dimension fails startup validation.

Only one ingestion run may be RUNNING. A crashed process leaves its run visible and blocks another ingestion. After verifying that **no ingestion process is still running**, mark that specific run as failed using its UUID:

```sql
UPDATE ingestion_run SET status='FAILED', completed_at=now()
WHERE id='<confirmed-abandoned-run-uuid>' AND status='RUNNING';
```

No automatic timeout steals ownership from a potentially active process. Retry ingestion after this explicit recovery. Failed fetches update `last_attempt_at` without changing the stored content's retrieval timestamp. Unchanged successful fetches refresh metadata without embedding again. `retrieval.indexAsOf` is the oldest successful source verification time among the retrieved hits, not a guarantee of current government policy.

## Coding with AI assistants

See the [agentic coding guide](docs/agentic-coding.md) for shared repository instructions, skills, specialist agents, and task prompts for Codex and GitHub Copilot. Java practices and governance requirements are defined in the shared [engineering standards](docs/engineering-standards.md).
