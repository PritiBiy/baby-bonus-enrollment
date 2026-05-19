# Baby Bonus Enrollment Service — Claude Guidelines

## Read These First

Before writing any code, read these documents in order:

| Document | Purpose |
|----------|---------|
| `SCOPE.md` | What is in scope, deferred, and why — governs every feature decision |
| `.claude/docs/domain-model.md` | Canonical domain entities and field types |
| `.claude/docs/api-contract.md` | Exact request/response shapes, status codes, and error messages |
| `data-sensitivity.md` | NRIC masking rules and data handling requirements |
| `mock-data/` | ICA and IROAS stub data loaded at startup |

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

## Package structure

```
com.gov.sg.baby_bonus_enrollment
├── BabyBonusEnrollmentApplication.kt
│
├── domain/
│   ├── enrollment/
│   │   ├── Enrollment.kt                ← pure data class, no JPA
│   │   ├── EnrollmentRepository.kt      ← repository interface (domain contract)
│   │   ├── Citizenship.kt
│   │   ├── Relationship.kt
│   │   └── EnrollmentStatus.kt
│   └── disbursement/
│       ├── Disbursement.kt              ← pure data class, no JPA
│       ├── DisbursementRepository.kt    ← repository interface (domain contract)
│       ├── DisbursementType.kt
│       └── DisbursementStatus.kt
│
├── repository/
│   ├── EnrollmentEntity.kt              ← JPA entity for enrollment table
│   ├── EnrollmentJpaRepository.kt       ← Spring Data JPA interface (internal)
│   ├── EnrollmentRepository.kt          ← JPA implementation of domain contract
│   ├── DisbursementEntity.kt            ← JPA entity for disbursement table
│   ├── DisbursementJpaRepository.kt     ← Spring Data JPA interface (internal)
│   └── DisbursementRepository.kt        ← JPA implementation of domain contract
│
├── usecase/
│   ├── EnrollChildUseCase.kt            ← one class per use case, fun execute(...)
│   ├── dto/
│   │   ├── CreateEnrollmentDto.kt
│   │   └── EnrollmentDto.kt
│   └── exception/
│       ├── EligibilityException.kt
│       └── DuplicateEnrollmentException.kt
│
├── controller/
│   ├── EnrollmentController.kt
│   ├── request/
│   │   ├── EnrollmentRequest.kt
│   │   └── IneligibleRequest.kt
│   ├── response/
│   │   ├── EnrollmentResponse.kt
│   │   ├── DisbursementResponse.kt
│   │   └── ErrorResponse.kt
│   └── exception/
│       └── GlobalExceptionHandler.kt
│
├── external/
│   ├── ica/
│   │   ├── IcaClient.kt                 ← interface
│   │   ├── ChildRecord.kt
│   │   ├── MockIcaClient.kt
│   │   └── exception/
│   │       └── IcaClientException.kt
│   ├── iroas/
│   │   ├── IroasClient.kt               ← interface
│   │   ├── ParentRecord.kt
│   │   ├── MockIroasClient.kt
│   │   └── exception/
│   │       └── IroasClientException.kt
│   └── disbursement/
│       ├── DisbursementClient.kt        ← interface
│       ├── DisbursementRequest.kt
│       ├── DisbursementResult.kt
│       ├── MockDisbursementClient.kt
│       └── exception/
│           └── DisbursementClientException.kt
│
├── security/
│   └── (internals TBD)
│
└── audit/
└── (internals TBD)
``` 

---

## Layer Rules

### Domain
- Contains enums and pure Kotlin data classes only — no JPA annotations, no Spring annotations, no framework dependencies
- Grouped into sub-packages by bounded context: `domain/enrollment/` and `domain/disbursement/`
- Each sub-package has a root data class (`Enrollment`, `Disbursement`) representing the domain concept — no persistence details
- Each sub-package owns its repository interface (`EnrollmentRepository`, `DisbursementRepository`) — the domain defines the contract, the repository layer fulfills it
- JPA entities and implementations live in `repository/` — named `*Entity` and `*RepositoryImpl`
### Repository
- Defined as interfaces extending `JpaRepository` — no implementation classes
- Do not define custom exceptions at this layer
- Spring `DataAccessException` subtypes bubble up; the controller layer catches unmapped ones and returns a generic 500
- Repository tests use `@SpringBootTest` + `@Transactional` — `@DataJpaTest` does not exist in Spring Boot 4.x
- Write tests against the domain-level repository interface (`EnrollmentRepository`, `DisbursementRepository`) — do not write separate tests for `*JpaRepository`; the JPA layer is covered implicitly
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
- `Request` and `Response` classes live in `controller/request/` and `controller/response/` — never in `domain/`
- `Request` classes must not import from `domain/` — use `String` for fields that map to domain enums; the controller performs the mapping
- Owns `GlobalExceptionHandler` in `controller/exception/`
- `GlobalExceptionHandler` maps:
    - Service exceptions (`EligibilityException`, `DuplicateEnrollmentException`) → appropriate 4xx
    - `DataAccessException` (unmapped repository errors) → generic 500
    - Catch-all `Exception` → generic 500, no implementation details leaked
### External
- Each external dependency has its own subpackage under `external/`
- Each defines an interface with a `Mock*` implementation alongside it
- The mock is the only implementation for this service — real HTTP clients are out of scope
- Each subpackage owns its own exception in `external/<name>/exception/`
- The service layer catches these exceptions and translates to domain exceptions
- The controller layer never sees external exceptions directly
- Return `null` for "not found" — it is an expected, normal response; throw an exception only for unexpected failures (network error, malformed response, timeout)
### Security
- Package exists; internals TBD
- All endpoints secured by default — nothing permitted without authentication unless explicitly allowlisted
- Swagger UI paths are allowlisted so they are accessible without a key
### Audit
- Called from service layer only — never from controller or repository
- Log levels:
  - `INFO` — normal flow events (enrollment submitted, eligibility passed, disbursement initiated)
  - `WARN` — recoverable business rejections (eligibility failures, duplicate enrollment)
  - `ERROR` — unexpected failures that indicate something is broken (unhandled exceptions, infrastructure errors)
---
## Design Principles (Kent Beck)

### Four Rules of Simple Design — in priority order

1. **Passes all tests** — working code first; no untested behaviour
2. **Reveals intention** — names explain *what*, not *how*; a reader should understand without comments
3. **No duplication** — every piece of knowledge has one home; extract only when you see the third repetition
4. **Fewest elements** — delete anything that is not required right now; classes, parameters, abstractions all have a cost

### Make it work → make it right → make it fast

Do not optimise prematurely. Get the simplest thing working, then clean it up. Performance is not a concern for this service.

### YAGNI

Do not build for imagined future requirements. The async disbursement flow, CDA disbursement, and refund are explicitly deferred in `SCOPE.md` — do not scaffold for them.

### TDD cycle

Write a failing test that describes the desired behaviour. Write the minimum production code to pass it. Refactor. Repeat. Tests are the specification — if behaviour is not tested, it does not exist.

### Incremental commits

Each commit should represent one coherent step. A reader should be able to follow the build-up from the git log. A single large commit is a red flag.

---

## Kotlin Conventions

- Prefer `data class` for DTOs and domain value objects
- Use `sealed class` / `enum class` for status types (`EnrollmentStatus`, `DisbursementStatus`, etc.)
- Prefer immutability: `val` over `var`, immutable collections
- Use `@JvmStatic` sparingly; favour top-level functions over companion object utilities
- Null safety is a feature — do not reach for `!!`; model optionality explicitly with `?` and handle it with `?.let`, `?:`, or `when`
- Extension functions are fine when they genuinely extend a type; avoid them as a workaround for poor structure

### Method ordering within a class

Order private methods to match the level of abstraction they operate at, reading top-down:

1. **Public entry point first** (`execute`, etc.)
2. **Business logic methods in call order** — each method appears near where it is first called, so the reader can follow the flow without jumping
3. **Each helper immediately below its caller** — if `checkEligibility` calls `failEligibility`, place `failEligibility` right after `checkEligibility`
4. **Low-level / infrastructure concerns last** — audit logging, simple formatters, and one-liner helpers whose names are self-explanatory go at the bottom; readers rarely need to inspect their bodies

---

## Spring Boot Conventions

- Controllers handle HTTP only: parse input, call service, map to response — no business logic
- Services own business logic and orchestration
- Repositories are Spring Data JPA interfaces only — no query logic in service or controller
- Use constructor injection; avoid `@Autowired` on fields
- Do not leak JPA entities into API responses — use dedicated response DTOs

---

## Security — Non-Negotiable

These rules come from `data-sensitivity.md` and `api-contract.md`. Apply them everywhere, every time.

### NRIC Masking
- Format: first 4 characters + `****` + last character
- `T2400001A` → `T240****A`
- Mask **all** NRIC fields in **all** API responses (`childNric`, `parentNric`)
- Mask in **all** audit log entries — never log a raw NRIC

### API Authentication
- All endpoints require `X-API-Key` header
- Missing or invalid key → `401 Unauthorised`
- Key value is configurable via `application.properties`; never hardcode it

### Error Responses
- All errors use `{"error": "<message>"}` shape (see `api-contract.md`)
- Never expose: stack traces, class names, SQL errors, internal field names
- Map exceptions to meaningful HTTP responses at the controller / exception handler level

---

## Eligibility Rules

Defined in `assignment.md`. A child is eligible if **all** are true:
1. Child exists in ICA records
2. Child is a Singapore Citizen
3. Parent/guardian exists in IROAS records
4. Child has no prior active enrollment

Fail fast — return the first failing condition as a `422` with the exact message from `api-contract.md`.

---

## Testing

All testing conventions, patterns, and layer-specific rules are in `.claude/commands/write-tests.md`. Read that file before writing any test.

---

## What Is Out of Scope

Refer to `SCOPE.md` for the authoritative list.

---

## Completing Changes

After making any code change, run `./gradlew build` to verify compilation. Only say **"done changes"** when the build is successful. Do not report a task as complete if the build fails.

---

## Running the Service

```bash
./gradlew bootRun
```

Tests:
```bash
./gradlew test
```

The H2 console is available at `http://localhost:8080/h2-console` when the app is running.
