# Baby Bonus Enrollment Service

The Baby Bonus Scheme supports parents of newborn Singapore Citizens with two financial benefits:

- A **cash gift** paid directly to the parent's bank account
- A **Child Development Account (CDA)** — a ring-fenced savings account seeded by government top-ups, usable only at approved merchants

Parents apply to enrol their newborn child. The system checks eligibility against government sources of truth, creates an enrollment record, and initiates disbursement.

### Claude Code Commands

| Command | File | Purpose |
|---------|------|---------|
| `/write-tests` | `.claude/commands/write-tests.md` | Enforces TDD layer order — integration test first (`@Disabled`), then repository → use case → enable |

---

### Prerequisites

- Java 21
- Gradle (via the included `./gradlew` wrapper — no separate installation needed)

### Running the service

The service requires an `API_KEY` environment variable. It will not start without one.

```bash
API_KEY=your-secret-key ./gradlew bootRun
```

Once running:

| | URL |
|-|-----|
| API base | [http://localhost:8080/api/v1](http://localhost:8080/api/v1) |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| H2 console | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) |

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
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.gov.sg.baby_bonus_enrollment.usecase.EnrollChildUseCaseTest"

# Run a single test method
./gradlew test --tests "com.gov.sg.baby_bonus_enrollment.usecase.EnrollChildUseCaseTest.enrolling a child who is not a Singapore citizen throws EligibilityException"
```

Tests use a fixed key (`test-api-key`) configured in `src/test/resources/application.properties` — no environment variable needed to run the test suite.

### Assumptions

- **In-memory database** — H2 is used for persistence. Data does not survive a restart; this is intentional for the assignment scope.
- **Mocked external services** — ICA, IROAS, and the disbursement service are backed by static JSON files under `mock-data/`. No real HTTP calls are made.
- **Synchronous disbursement** — Disbursement is initiated within the same request and completes immediately with status `PROCESSED`. The production model would be async (POST returns `PENDING`, caller polls `GET /{id}`).
- **Single pre-shared API key** — One key is configured via `API_KEY` at startup. There is no multi-caller key management or rotation endpoint.
- **NRIC format not validated** — The service trusts the caller to supply a correctly formatted NRIC. Only `@NotBlank` is enforced.

### Project Documents

| Document | Purpose |
|----------|---------|
| [SCOPE.md](SCOPE.md) | Scoping decisions — what is built, deferred, and why |
| [data-sensitivity.md](data-sensitivity.md) | How NRIC and financial data is protected at rest, in transit, and in logs |
| [AI_USAGE.md](AI_USAGE.md) | How AI tools were used, where output was corrected, and where it was accepted |
| [api-contract.md](.claude/docs/api-contract.md) | Full API contract — request/response shapes, status codes, error messages |
| [domain-model.md](.claude/docs/domain-model.md) | Canonical domain entities and field types |
| [CLAUDE.md](CLAUDE.md) | Architectural rules and conventions given to Claude Code as context |
| [TODO.md](.claude/TODO.md) | Ordered task list used to drive development session by session |

### What I would do next

- **Persistent storage** — Replace H2 with PostgreSQL and manage schema migrations with Flyway or Liquibase.
- **Real external clients** — Implement live HTTP clients for ICA, IROAS using Spring's `RestClient`.
- **Async disbursement** — Move disbursement to a background process; POST returns `PENDING` and callers poll `GET /{id}` for the final status.
- **Multi-caller authentication** — Support multiple API keys or OAuth2 client credentials to distinguish callers in audit logs and enable key rotation.
- **Implement DisbursementService** — Implement an API client to call the disbursement service instead of the current mock, including async response handling.
