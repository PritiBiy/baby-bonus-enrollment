## TODO

Tasks are ordered. Start each with a failing integration test — build everything the test needs from the outside in.

---

### Task 1 — POST /api/v1/enrollments

- [x] Write failing integration test: `POST /api/v1/enrollments` → `201`, correct response shape, NRICs masked.
- [x] Define `Enrollment` and `Disbursement` JPA entities
- [x] Write migration scripts to create `enrollments` and `disbursements` tables
- [x] Need to add testcontainer tests for repository layer to verify JPA mappings and migrations - **NO NEED**
- [x] Create `EnrollmentEntityRepository` and `DisbursementEntityRepository` (ports in domain; impls in repository)
- [x] Write repository tests: save + field assertions via JpaRepository, findById, findByChildNric
- [x] Define `IcaClient` and `IroasClient` interfaces (`external` package)
- [x] Implement `MockIcaClient` and `MockIroasClient`; write tests for each
- [x] Load mock data from `mock-data/ica_children.json` and `mock-data/iroas_parents.json` at startup
- [x] Define `DisbursementClient` interface; implement `MockDisbursementClient`; write tests
- [x] Implement `EnrollChildUseCase` — eligibility checks (fail-fast), save enrollment, initiate disbursement
- [x] `EligibilityReason` enum with error messages; `EligibilityException` carries enum reason
- [x] Write use case unit tests: all 5 eligibility/duplicate scenarios; `Clock.fixed` for deterministic timestamps
- [x] Implement `EnrollmentController` — wire to `EnrollChildUseCase`, return full `EnrollmentResponse`
- [x] Define response DTOs (`EnrollmentResponse`, `DisbursementResponse`) with NRIC masking
- [x] `GlobalExceptionHandler` — `EligibilityException` → 422, `DuplicateEnrollmentException` → 409, `IllegalArgumentException` → 400, catch-all → 500
- [x] All tests pass (HTTP integration test enabled, 26 tests green)
- [x] Refactor use case to small methods for each step (checkEligibility, saveEnrollment, initiateDisbursement, toDto)
- [x] Add TODO on MockDisbursementClient — stub returns PROCESSED immediately; production would be async

---

### Task 2 — POST /api/v1/enrollments

- [x] Write the failing test for other http status for the controller as mentioned in the ./claude/docs/api-contract.md
- [x] Implement global exception handler to return correct status codes and error messages for different failure scenarios (422 for eligibility failures, 409 for duplicates, etc.)
- [x] Add error mapping from domain exception to http exception.
- [x] All tests pass (27 tests green)

---

### Task 3 — Audit Logging

- [x] use slf4j with Logback; configure in `application.properties` to log to file with daily rotation
- [x] Log: enrollment submitted — masked child NRIC, caller identity (via MDC set in ApiKeyFilter)
- [x] Log: eligibility check result — pass/fail and reason
- [x] Log: disbursement initiated — enrollment ID, amount
- [x] Verify no raw NRIC appears in any log output (enforced by `Nric` value class `toString()`)
- [x] `Nric` typed value class introduced — `toString()` always returns masked form; type system prevents raw leaks
- [x] `AuditLogger` refactored to utility (`info/warn/error`) — no domain knowledge; audit semantics moved to private methods in use case
- [x] `GlobalExceptionHandler` logs ERROR via `AuditLogger` for unexpected exceptions (DB down etc.)
- [x] All tests pass (31 tests green)

---


### Task 4 — X-API-Key Authentication

- [x] Write failing integration test: missing `X-API-Key` → `401`
- [x] Write failing integration test: invalid `X-API-Key` → `401`
- [x] Implement `ApiKeyFilter`; key read from `API_KEY` env var — startup fails if not set
- [x] All tests pass (29 tests green)

---

### Task 5 — Swagger / OpenAPI

- [ ] Add `springdoc-openapi` dependency
- [ ] Annotate controller and DTOs
- [ ] Verify Swagger UI loads at `/swagger-ui.html`
- [ ] Document `X-API-Key` as a security scheme in the spec

---

### Task 6 — GET /api/v1/enrollments/{id}

- [ ] Write failing integration test: known ID → `200`, enrollment + disbursement, NRICs masked
- [ ] Write failing integration test: unknown ID → `404`
- [ ] Implement GET by ID in controller and service
- [ ] All tests pass

---

### Task 7 — GET /api/v1/enrollments?childNric=

- [ ] Write failing integration test: returns all enrollments for a child, NRICs masked
- [ ] Write failing integration test: unknown NRIC → `[]` (not 404)
- [ ] Write failing integration test: missing `childNric` param → `400`
- [ ] Implement list by `childNric` in controller and service
- [ ] All tests pass

---

### Task 8  — PATCH /api/v1/enrollments/{id}/ineligible

- [ ] Write failing integration test: overrides any status to `INELIGIBLE` with reason → `200`
- [ ] Write failing integration test: already `INELIGIBLE` → `422`
- [ ] Write failing integration test: unknown ID → `404`
- [ ] Write failing integration test: blank reason → `400`
- [ ] Implement PATCH endpoint in controller and service
- [ ] All tests pass

---

### Task 9 — Infrastructure and Submission

- [ ] `Dockerfile` — builds and runs the service
- [ ] `README.md` — setup, how to run, assumptions, "What I would do next"
- [ ] `AI_USAGE.md` — tools used, how output was reviewed, what was discarded

### Task 10 — Misc

- [ ] Create precommit hook check if cc has built in hooks for this. 
- [ ] For Disbursement 3000, create constant and give meaningful name.
- [ ] 

---