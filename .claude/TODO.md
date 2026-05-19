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
- [ ] Implement `EnrollmentController` — wire to `EnrollChildUseCase`, return full `EnrollmentResponse`
- [ ] Define response DTOs (`EnrollmentResponse`, `DisbursementResponse`) with NRIC masking
- [ ] All tests pass (enable HTTP integration test)

---

### Task 2 — POST /api/v1/enrollments

- [ ] Write the failing test for other http status for the controller as mentioned in the ./claude/docs/api-contract.md
- [ ] Implement global exception handler to return correct status codes and error messages for different failure scenarios (422 for eligibility failures, 409 for duplicates, etc.)
- [ ] Add error mapping from domain exception to http exception.
- [ ] All tests pass

---

### Task 3 — Audit Logging

- [ ] Log: enrollment submitted — timestamp, masked child NRIC, caller identity, outcome
- [ ] Log: eligibility check result — pass/fail and reason
- [ ] Log: disbursement initiated — enrollment ID, amount
- [ ] Verify no raw NRIC appears in any log output

---


### Task 4 — X-API-Key Authentication

- [ ] Write failing integration test: missing `X-API-Key` → `401`
- [ ] Write failing integration test: invalid `X-API-Key` → `401`
- [ ] Implement API key filter; key read from `application.properties`
- [ ] All tests pass

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
