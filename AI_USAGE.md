AI Usage

This document describes how AI tools were used during the development of this service.


### Tools Used

|  Tool | Purpose                                                   | Comment                                                                    |
|---|-----------------------------------------------------------|----------------------------------------------------------------------------|
|  Claude (Anthropic) | For planning, boilerplate generation, reviewing decisions | Generated documents as disucssed such as API_contracts, domain, and README |
|  GitHub Copilot | inline code completion                                    |                                                                            |

### Review and Validation Process
- Created API_Contract.md to define the scope.md 
- Rephrased writings in different files such as reasoning in scope.md, README.md sections 
- Took help to align the package structure after giving rules to come up with the structure and layered rules.

### Task 1 — POST /api/v1/enrollments (sessions 2–4)

- Used CC in plan mode to scaffold integration test, migration, domain types, and external client interfaces; reworked test to use `MockMvc.perform()` after `MockMvcWebTestClient` failed due to missing reactive streams dependency.
- CC initially put JPA annotations (`@Entity`, `@Table`) in the domain layer; corrected to keep domain as pure data classes and move JPA entities to `repository/` as `*Entity` — used as thinking partner to settle the right separation.
- CC imported `Relationship` from domain into `EnrollmentRequest` violating layer rules; corrected to `String` and rule added to `CLAUDE.md`.
- CC attempted to write source/stub implementations in tests instead of `@MockitoBean` interface mocks; corrected and rule added to `write-tests.md`.
- CC searched for `@DataJpaTest` (does not exist in Spring Boot 4.x); used online search to confirm `@SpringBootTest` + `@Transactional` is the correct replacement, rule added to `CLAUDE.md` and `write-tests.md`.
- Used as thinking partner to settle domain vs repository boundary: domain owns pure data classes and repository interfaces (ports-and-adapters); JPA entities and `*RepositoryImpl` stay in repository; CC initially placed both interface and impl in repository, corrected after discussion.
- `createdAt` removed from domain `Enrollment` after discussion — identified as a persistence concern belonging to the entity only; `enrolledAt` kept as it represents a business event timestamp.
- Confirmed null-return-for-not-found rule applies even though repository interfaces now live in domain — service layer decides whether not-found is an error.
- Switched from service layer pattern to use case pattern (`EnrollChildUseCase` with `execute()`); CC initially scaffolded a service interface + impl, corrected to one class per use case with no interface.
- `EligibilityReason` enum introduced to carry error messages for eligibility failures; CC initially used raw strings in `EligibilityException`, corrected to enum so the reason is type-safe and tests assert `exception.reason` rather than a string.
- `Clock` injected as constructor dependency into `EnrollChildUseCase` so tests can control time with `Clock.fixed(...)`; CC initially placed `Clock` as a `@Bean` only — clarified that unit tests construct the use case directly with a fixed clock, the `@Bean` wires it only for the Spring context.
- Migrated all test assertions from AssertJ / `kotlin.test` to Kotest (`shouldBe`, `shouldNotBe`, `shouldThrow`).

### Task 2 — Error scenarios and validation (session 5)

- CC initially separated test groups using comment banners (`// --- 422 eligibility failures ---`); corrected to JUnit 5 `@Nested` inner classes so the grouping is structural, not cosmetic.
- `@NotBlank` validation added to `EnrollmentRequest` with `spring-boot-starter-validation`; `MethodArgumentNotValidException` handler added to `GlobalExceptionHandler` to return the field's constraint message as the error body.
- `Relationship.valueOf("INVALID")` produces a JVM-verbose message; wrapped in try/catch in controller to throw `IllegalArgumentException("Invalid value for relationship")` with the exact message from the API contract.

### Task 4 — X-API-Key Authentication (session 5)

- CC initially wrote `api.key=change-me` hardcoded in `application.properties`; corrected to read from `API_KEY` env var (`api.key=${API_KEY}`) with no fallback — startup fails if the env var is not set.
- `src/test/resources/application.properties` sets `api.key=test-api-key` so tests run without the env var; production config is separate.

### Task 3 — Audit Logging (session 5)

- Introduced `Nric` as a `@JvmInline value class` with `toString()` returning the masked form — the type system prevents any code path from accidentally logging a raw NRIC; no runtime overhead.
- `AuditLogger` in `audit/` accepts `Nric` parameters; `toString()` auto-masks in SLF4J format strings — no explicit masking call needed at each log site.
- `CreateEnrollmentDto` updated to hold `Nric` instead of `String`; controller wraps the incoming string at the boundary; use case no longer needs the `mask()` helper.
- MDC caller set in `ApiKeyFilter` after successful authentication; cleared in `finally` block; log pattern includes `%X{caller}` so every log line records the caller identity without threading it through the call stack.
- `AuditLogger` refactored to a pure utility (`info/warn/error` methods, no domain imports) — domain-aware audit methods (`auditEnrollmentSubmitted` etc.) moved to private methods in `EnrollChildUseCase`; use case owns what to log, logger owns how to write it.
- Log levels: eligibility failures and duplicates use `WARN` (recoverable business rejections); normal flow uses `INFO`; unexpected exceptions in `GlobalExceptionHandler` use `ERROR`.
- User directed moving the try-catch out of `execute` into `checkEligibility` so failure logging is co-located with the failure. Simplified further using a `Nothing`-returning `failEligibility` helper — each eligibility check becomes a one-liner with no try-catch.
- User directed method ordering convention: business logic in call order near the top, low-level concerns (audit one-liners) at the bottom since their names are self-explanatory. Rule captured in `CLAUDE.md`.
- User directed moving `auditEligibilityPassed` into `checkEligibility` and `auditDisbursementInitiated` into `initiateDisbursement` — each method now owns its own audit logging, and `execute` reads as a clean sequence of steps.
- User identified repeated `EnrollmentResponse(...)` mapping across all three controller methods; extracted as a private `EnrollmentDto.toResponse()` extension function — controller endpoints reduced to single-line expressions.

### Test Refactoring — Controller tests split by API endpoint

- Refactoring was initiated by the user, who identified that common mock setup was repeated across tests and that grouping by API endpoint would be clearer.
- User directed: extract repeated stub setup into helper methods with minimal parameters, and split by API endpoint so all success and error scenarios for one API live in one file.
- CC implemented `BaseControllerTest` with shared `@SpringBootTest`/`@AutoConfigureMockMvc` setup, `@MockitoBean` declarations, and stub helpers (`stubChildInIca`, `stubParentInIroas`, `stubDisbursement`, `stubEligibleEnrollment`).
- CC initially proposed splitting into four files by concern (`PostEnrollmentControllerTest`, `GetEnrollmentByIdControllerTest`, `AuditLoggingControllerTest`, `UnauthorizedControllerTest`); user corrected this to split strictly by API endpoint — so audit logging and auth tests fold into `PostEnrollmentControllerTest` as `@Nested` inner classes since they exercise the POST endpoint.
- All 33 tests continue to pass.

### Task 7 — GET /api/v1/enrollments?childNric=

- Repository layer (`findByChildNric`, `findByEnrollmentId`) was already in place from Task 1; Task 7 only needed the use case and controller wiring.
- User directed removing the 400-missing-param test initially, then corrected: code was added to `GlobalExceptionHandler` for `MissingServletRequestParameterException`, so a test is required — test was added back.
- User directed using `@RequestParam childNric: String` (required=true by default) instead of `required = false` with a manual null check; `MissingServletRequestParameterException` handler in `GlobalExceptionHandler` produces the correct error shape and message using `e.parameterName`.
- 36 tests green.

