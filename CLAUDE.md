# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read These First

Before writing any code, read these documents in order:

| Document | Purpose                                                                                    |
|----------|--------------------------------------------------------------------------------------------|
| `SCOPE.md` | What is in scope, deferred, and why — governs every feature decision                       |
| `.claude/docs/domain-model.md` | Canonical domain entities and field types                                                  |
| `.claude/docs/api-contract.md` | Exact request/response shapes, status codes, and error messages                            |
| `data-sensitivity.md` | NRIC masking rules and data handling requirements                                          |
| `mock-data/` | ICA and IROAS stub data loaded at startup refer                                            |
| `.claude/TODO.md` | Active task list — check here before starting work to understand what is planned or in progress |

Do not invent behaviour that contradicts these documents. If a contract is ambiguous, ask before assuming.

---

## Project

Kotlin + Spring Boot service handling Baby Bonus enrollment applications. Checks eligibility against in-memory ICA and IROAS stubs, creates enrollment records, and initiates a synchronous stub disbursement.

- **Language**: Kotlin
- **Framework**: Spring Boot 4.x
- **Persistence**: H2 in-memory via Spring Data JPA
- **Build**: Gradle (Kotlin DSL)
- **Java**: 21

---

## Commands

```bash
# Build and verify compilation
./gradlew build

# Run the service (API_KEY env var is required — startup fails without it)
API_KEY=secret ./gradlew bootRun

# Run all tests (src/test/resources/application.properties sets api.key=test-api-key)
./gradlew test

# Run a single test class
./gradlew test --tests "com.gov.sg.baby_bonus_enrollment.usecase.EnrollChildUseCaseTest"

# Run a single test method
./gradlew test --tests "com.gov.sg.baby_bonus_enrollment.usecase.EnrollChildUseCaseTest.enrolling a child who is not a Singapore citizen throws EligibilityException"
```

The H2 console is available at `http://localhost:8080/h2-console` when the app is running.

After **any** code change, run `./gradlew build`. Only say **"done"** when the build is green.

---

## Key architectural patterns

### `Nric` value class

`domain/Nric.kt` is a `@JvmInline value class`. Its `toString()` always returns the masked form (`T240****A`). Wrapping an NRIC in this type makes it impossible to accidentally log or serialize the raw value — the type system enforces masking.

### Repository naming

Domain interfaces are named `*EntityRepository` (e.g. `EnrollmentEntityRepository`). Implementations in `repository/` are named `*EntityRepositoryImpl`. This asymmetry exists because the domain defines the contract and the name reflects what it stores (entities), while the suffix distinguishes the implementation.

### Integration test base class

All controller tests extend `BaseControllerTest`, which provides:
- `MockMvc` + `ObjectMapper` autowired
- `@MockitoBean` for all three external clients (`IcaClient`, `IroasClient`, `DisbursementClient`)
- Shared stub helpers: `stubChildInIca`, `stubParentInIroas`, `stubDisbursement`, `stubEligibleEnrollment`
- Constants: `API_KEY = "test-api-key"`, `DEFAULT_CHILD_NRIC`, `DEFAULT_PARENT_NRIC`

`src/test/resources/application.properties` sets `api.key=test-api-key` so the `ApiKeyFilter` passes during tests.

### `Clock` injection

`EnrollChildUseCase` takes a `Clock` bean constructor parameter. Production uses `Clock.systemUTC()`. Use case tests use `Clock.fixed(...)` for deterministic `enrolledAt` timestamps.

---

## Layer Rules

### Domain
- Contains enums and pure Kotlin data classes only — no JPA annotations, no Spring annotations, no framework dependencies
- Grouped into sub-packages by bounded context: `domain/enrollment/` and `domain/disbursement/`
- Each sub-package has a root data class (`Enrollment`, `Disbursement`) representing the domain concept — no persistence details
- Each sub-package owns its repository interface — the domain defines the contract, the repository layer fulfills it

### Repository
- Implementations live in `repository/` — named `*EntityRepositoryImpl`; delegate to an injected `*JpaRepository`
- Do not define custom exceptions at this layer
- Spring `DataAccessException` subtypes bubble up; the controller layer catches unmapped ones and returns a generic 500
- Repository tests use `@SpringBootTest` + `@Transactional` — `@DataJpaTest` does not exist in Spring Boot 4.x
- Write tests against the domain-level repository interface — do not write separate tests for `*JpaRepository`; the JPA layer is covered implicitly

### Use Case
- One class per use case — no interface, no `Impl` suffix
- Each class has a single `execute(...)` method
- Owns DTOs in `usecase/dto/` — controller maps `Request` → `Dto`, use case returns `Dto`, controller maps to `Response`
- Owns exceptions in `usecase/exception/`
- Catches external client exceptions and translates to domain exceptions — never lets external exceptions propagate upward
- Never imports anything from `controller/`

### Controller
- Handles HTTP only: parse input, map to use case DTO, call use case, map result to response
- No business logic
- Maps `Request` → use case `Dto` — never passes raw request objects into the use case
- Maps use case `Dto` → `Response` — never exposes entities or use case DTOs in responses
- `Request` classes must not import from `domain/` — use `String` for fields that map to domain enums; the controller performs the mapping
- `GlobalExceptionHandler` maps:
    - `EligibilityException` → 422, `DuplicateEnrollmentException` → 409
    - `NotFoundException` → 404, `EnrollmentAlreadyIneligibleException` → 422
    - `IllegalArgumentException` → 400
    - `DataAccessException` (unmapped repository errors) → generic 500
    - Catch-all `Exception` → generic 500, no implementation details leaked

### External
- Each external dependency has its own subpackage under `external/`
- Each defines an interface with a `Mock*` implementation alongside it — the mock is the only implementation for this service
- Return `null` for "not found" — expected response; throw an exception only for unexpected failures (network error, malformed response, timeout)
- The use case catches these exceptions and translates to domain exceptions; the controller never sees external exceptions

### Security
- `ApiKeyFilter` validates `X-API-Key` header on all requests; missing or invalid key → `401 {"error":"Unauthorised"}`
- Swagger UI paths (`/swagger-ui/**`, `/v3/api-docs/**`) bypass the filter via `shouldNotFilter`
- `api.key` property is sourced from `API_KEY` env var — startup fails if not set

### Audit
- Log levels: `INFO` for normal flow, `WARN` for business rejections (eligibility fail, duplicate), `ERROR` for unexpected failures
- `ApiKeyFilter` puts `caller=api-key` into MDC so every log line includes the caller identity

---

## Design Principles (Kent Beck)

### Four Rules of Simple Design — in priority order

1. **Passes all tests** — working code first; no untested behaviour
2. **Reveals intention** — names explain *what*, not *how*
3. **No duplication** — every piece of knowledge has one home; extract only when you see the third repetition
4. **Fewest elements** — delete anything that is not required right now

### YAGNI

Do not build for imagined future requirements. The async disbursement flow, CDA disbursement, and refund are explicitly deferred in `SCOPE.md` — do not scaffold for them.

### TDD cycle

Write a failing test that describes the desired behaviour. Write the minimum production code to pass it. Refactor. Repeat. Tests are the specification — if behaviour is not tested, it does not exist.

### Incremental commits

Each commit should represent one coherent step. A reader should be able to follow the build-up from the git log.

---

## Kotlin Conventions

- Prefer `data class` for DTOs and domain value objects
- Use `sealed class` / `enum class` for status types
- Prefer immutability: `val` over `var`, immutable collections
- Null safety is a feature — model optionality explicitly with `?`; handle with `?.let`, `?:`, or `when`; do not use `!!`
- Use with block for multiple operations on the same object; use `apply` for configuring objects in one expression

### Method ordering within a class

1. **Public entry point first** (`execute`, etc.)
2. **Business logic methods in call order** — each method near where it is first called
3. **Each helper immediately below its caller**
4. **Low-level / infrastructure concerns last** — audit logging, simple formatters

---

## Security — Non-Negotiable

### NRIC Masking
- Format: first 4 characters + `****` + last character — `T2400001A` → `T240****A`
- Mask **all** NRIC fields in **all** API responses and audit log entries
- Use `Nric(value).masked()` or wrap in a `Nric` value class — never format manually

### Error Responses
- All errors use `{"error": "<message>"}` shape (see `api-contract.md`)
- Never expose: stack traces, class names, SQL errors, internal field names

---

## Eligibility Rules

A child is eligible if **all** are true:
1. Child exists in ICA records
2. Child is a Singapore Citizen
3. Parent/guardian exists in IROAS records
4. Child has no prior `ENROLLED` enrollment

Fail fast — return the first failing condition as a `422` with the exact message from `api-contract.md`.

---

## Testing

All testing conventions, patterns, and layer-specific rules are in `.claude/commands/write-tests.md`. Read that file before writing any test.

### Mock data paths

- `src/main/resources/mock-data/ica_children.json` — loaded by `MockIcaClient` at startup
- `src/main/resources/mock-data/iroas_parents.json` — loaded by `MockIroasClient` at startup
- `src/test/resources/mock-data/` — test-only fixtures for stub client unit tests
