# Baby Bonus Enrollment Service

The Baby Bonus Scheme supports parents of newborn Singapore Citizens with two financial benefits:

- A **cash gift** paid directly to the parent's bank account
- A **Child Development Account (CDA)** — a ring-fenced savings account seeded by government top-ups, usable only at approved merchants

Parents apply to enrol their newborn child. The system checks eligibility against government sources of truth, creates an enrollment record, and initiates disbursement.

### Project Documents

| Document | Purpose |
|----------|---------|
| `SCOPE.md` | Scoping decisions — what is built, deferred, and why |
| `data-sensitivity.md` | How NRIC and financial data is protected at rest, in transit, and in logs |
| `AI_USAGE.md` | How AI tools were used, where output was corrected, and where it was accepted |
| `.claude/docs/api-contract.md` | Full API contract — request/response shapes, status codes, error messages |
| `.claude/docs/domain-model.md` | Canonical domain entities and field types |
| `CLAUDE.md` | Architectural rules and conventions given to Claude Code as context |
| `.claude/TODO.md` | Ordered task list used to drive development session by session |

### Claude Code Commands

| Command | File | Purpose |
|---------|------|---------|
| `/write-tests` | `.claude/commands/write-tests.md` | Enforces TDD layer order — integration test first (`@Disabled`), then repository → use case → enable |

---

### Prerequisites

Java 21

### Running the service

The service requires an `API_KEY` environment variable. It will not start without one.

```bash
API_KEY=your-secret-key ./gradlew bootRun
```

### Running with Docker

```bash
docker build -t baby-bonus-enrollment .
docker run -e API_KEY=your-secret-key -p 8080:8080 baby-bonus-enrollment
```

### Authentication

All endpoints require an `X-API-Key` header. Any request with a missing or incorrect key receives `401 Unauthorised` — there is no distinction between the two cases.

```
X-API-Key: your-secret-key
```

**Key distribution:** The API key is a pre-shared secret. The operator sets `API_KEY` when starting the service and shares the value with authorised callers out-of-band (e.g. via a secrets manager or direct communication). There is no key-issuance endpoint.

### Running tests

```bash
./gradlew test
```

Tests use a fixed key (`test-api-key`) configured in `src/test/resources/application.properties` — no environment variable needed to run the test suite.

### Assumptions

- **In-memory database** — H2 is used for persistence. Data does not survive a restart; this is intentional for the assignment scope.
- **Mocked external services** — ICA, IROAS, and the disbursement service are backed by static JSON files under `mock-data/`. No real HTTP calls are made.
- **Synchronous disbursement** — Disbursement is initiated within the same request and completes immediately with status `PROCESSED`. The production model would be async (POST returns `PENDING`, caller polls `GET /{id}`).
- **Single pre-shared API key** — One key is configured via `API_KEY` at startup. There is no multi-caller key management or rotation endpoint.
- **NRIC format not validated** — The service trusts the caller to supply a correctly formatted NRIC. Only `@NotBlank` is enforced.

### What I would do next

- **Persistent storage** — Replace H2 with PostgreSQL and manage schema migrations with Flyway or Liquibase.
- **Real external clients** — Implement live HTTP clients for ICA, IROAS, and the disbursement service using Spring's `RestClient`.
- **Async disbursement** — Move disbursement to a background process; POST returns `PENDING` and callers poll `GET /{id}` for the final status.
- **Multi-caller authentication** — Support multiple API keys or OAuth2 client credentials to distinguish callers in audit logs and enable key rotation.
- **Observability** — Expose Spring Actuator health and metrics endpoints; wire into a monitoring stack.

