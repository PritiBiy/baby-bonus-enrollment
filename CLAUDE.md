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

---

## Spring Boot Conventions

- Controllers handle HTTP only: parse input, call service, map to response — no business logic
- Services own business logic and orchestration
- Repositories are Spring Data JPA interfaces only — no query logic in service or controller
- Use constructor injection; avoid `@Autowired` on fields
- `@Transactional` belongs on the service method that owns the unit of work
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

- **Unit tests**: service layer — happy path + each distinct error case; mock repositories
- **Integration test**: at least one full HTTP flow through `@SpringBootTest` / `MockMvc`; verifies status code, response shape, and NRIC masking
- Test names should read as sentences describing behaviour, not implementation
- Do not test Spring wiring or framework behaviour — test your code

---

## What Is Out of Scope

Refer to `SCOPE.md` for the authoritative list.

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
