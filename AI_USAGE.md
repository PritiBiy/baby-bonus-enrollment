# AI Usage

This document describes how AI tools were used during the development of this service.

---

## Tools Used

| Tool | Purpose |
|------|---------|
| Claude Code (Anthropic) | Planning, implementation, code review, and documentation |
| GitHub Copilot | Inline code completion |

---

## How CC Was Used

Each feature was started with CC in **plan mode**: the task was provided, CC produced a step-by-step implementation plan, and the user reviewed and gave corrections before any code was written. Once the plan was approved, CC switched to **edit/accept mode** to implement; output was reviewed before committing.

A `/write-tests` command was created as a Claude Code slash command (`.claude/commands/write-tests.md`) to enforce TDD discipline. It defines the test-layer order — write the integration test first and mark it `@Disabled`, then implement repository → use case → remove `@Disabled`. CC uses this as a skill automatically at the start of each feature.

`CLAUDE.md` was maintained throughout: whenever CC produced output that violated a design rule, the correction was written into `CLAUDE.md` or `write-tests.md` so the same mistake could not recur in future sessions.

---

## Where User Input Directed CC

Cases where CC's output was incorrect or off-design and was corrected:

- CC placed JPA annotations (`@Entity`, `@Table`) in the domain layer — corrected to pure Kotlin data classes; JPA entities moved to `repository/`; layer rule added to `CLAUDE.md`
- CC imported the `Relationship` enum from domain into `EnrollmentRequest` (controller importing domain) — corrected to use `String` in the request; controller performs the mapping
- CC scaffolded a service interface + implementation pattern — corrected to one use case class per operation with a single `execute()` method and no interface
- CC used raw strings in `EligibilityException` — corrected to a `EligibilityReason` enum so tests assert typed reasons rather than brittle string matches
- CC searched for `@DataJpaTest` (does not exist in Spring Boot 4.x) — corrected to `@SpringBootTest` + `@Transactional`; confirmed via online search; rule added to `CLAUDE.md` and `write-tests.md`
- CC applied `@MockitoBean` to internal Spring beans in integration tests — corrected to mock only external client interfaces (`IcaClient`, `IroasClient`, `DisbursementClient`); everything inside the project boundary runs for real
- CC hardcoded `api.key=change-me` in `application.properties` — corrected to `api.key=${API_KEY}` with no fallback so startup fails without the env var
- CC split controller tests by concern (separate files for auth, audit, happy-path) — corrected to split strictly by API endpoint so all scenarios for one endpoint live in one file
- CC used comment banners for test grouping — corrected to JUnit 5 `@Nested` classes so grouping is structural
- CC proposed `IneligibleRequest` / `AlreadyIneligibleException` as names — corrected to `MarkIneligibleEnrollmentStatusRequest` / `EnrollmentAlreadyIneligibleException` with domain context in the name
- User directed method ordering convention: business logic in call order near the top, audit one-liners at the bottom; rule captured in `CLAUDE.md`
- User directed moving audit calls inside the method that owns the operation (e.g. `auditDisbursementInitiated` inside `initiateDisbursement`) so each method is self-contained

---

## Where CC Output Was Used After Review

Cases where CC's output was accepted after verifying it was correct:

- `Nric` as `@JvmInline value class` with masked `toString()` — CC proposed; accepted because the type system makes raw NRIC leaks impossible at compile time with no runtime overhead
- `Clock` injected as a constructor parameter into `EnrollChildUseCase` — CC proposed; accepted as it allows unit tests to control time with `Clock.fixed(...)` without Spring context
- `AuditLogger` as a pure utility (`info/warn/error` methods, no domain imports), with domain-aware audit methods as private methods in each use case — accepted after discussion; keeps the logger free of domain knowledge while keeping audit semantics close to the business logic
- MDC `caller` set in `ApiKeyFilter` after successful authentication, cleared in `finally` — accepted; every downstream log line records caller identity without threading it through the call stack
- `BaseControllerTest` with shared `@SpringBootTest`/`@AutoConfigureMockMvc` setup, `@MockitoBean` declarations, and stub helpers (`stubChildInIca`, `stubParentInIroas`, `stubDisbursement`, `stubEligibleEnrollment`) — CC implemented after user directed the extraction; reviewed and accepted
- `GlobalExceptionHandler` mapping all exception types to the correct status codes and error shapes — CC implemented; reviewed against `api-contract.md` to verify each mapping
- Dockerfile multi-stage build (dependency layer, compile layer, JRE-only runtime image) — CC drafted; verified end-to-end with `docker build` and a live smoke test confirming auth and routing work inside the container
