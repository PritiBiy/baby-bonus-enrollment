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

### Task 1 — POST /api/v1/enrollments (session 2)

Used Claude Code (CC) in **plan mode** to design and implement the first TDD layer for the enrollment endpoint.

**What Claude generated:**
- `EnrollmentControllerTest` — integration test locking the POST /api/v1/enrollments contract, with `@MockitoBean` for external clients only
- `EnrollmentController` with `EnrollmentRequest` in `controller/request/`
- `schema.sql` migration, domain enums (`Citizenship`, `Relationship`, `EnrollmentStatus`, `DisbursementType`, `DisbursementStatus`), and JPA entities (`Enrollment`, `Disbursement`)
- External client interfaces (`IcaClient`, `IroasClient`, `DisbursementClient`) and data records
- `TestResourceReader` utility and `responses/create-enrollment-201.json` for file-based response assertions
- `write-tests.md` slash command documenting the project's TDD conventions

**Issues found and corrected:**
- Initial test suggestions were not using mocks for external dependencies — Claude was writing source/stub implementations instead of `@MockitoBean` interface mocks. The rule was clarified and added to `write-tests.md` and `CLAUDE.md`.
- `EnrollmentRequest` was initially importing `Relationship` from the `domain` package, violating the layer rule that controller request classes must not depend on domain types. Fixed to use `String`, and the rule was explicitly added to the Controller section of `CLAUDE.md`.
- `MockMvcWebTestClient` was used initially but requires reactive streams on the classpath; switched to standard `MockMvc.perform()`.
- `@AutoConfigureMockMvc` import required the Spring Boot 4.x-specific package `org.springframework.boot.webmvc.test.autoconfigure`.

