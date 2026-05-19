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

### Task 1 — POST /api/v1/enrollments (sessions 2–3)

- Used CC in plan mode to scaffold integration test, migration, domain types, and external client interfaces; reworked test to use `MockMvc.perform()` after `MockMvcWebTestClient` failed due to missing reactive streams dependency.
- CC initially put JPA annotations (`@Entity`, `@Table`) in the domain layer; corrected to keep domain as pure data classes and move JPA entities to `repository/` as `*Entity` — used as thinking partner to settle the right separation.
- CC imported `Relationship` from domain into `EnrollmentRequest` violating layer rules; corrected to `String` and rule added to `CLAUDE.md`.
- CC attempted to write source/stub implementations in tests instead of `@MockitoBean` interface mocks; corrected and rule added to `write-tests.md`.
- CC searched for `@DataJpaTest` (does not exist in Spring Boot 4.x); used online search to confirm `@SpringBootTest` + `@Transactional` is the correct replacement, rule added to `CLAUDE.md` and `write-tests.md`.
- Used as thinking partner to settle domain vs repository boundary: domain owns pure data classes and repository interfaces (ports-and-adapters); JPA entities and `*RepositoryImpl` stay in repository; CC initially placed both interface and impl in repository, corrected after discussion.
- `createdAt` removed from domain `Enrollment` after discussion — identified as a persistence concern belonging to the entity only; `enrolledAt` kept as it represents a business event timestamp.
- Confirmed null-return-for-not-found rule applies even though repository interfaces now live in domain — service layer decides whether not-found is an error.

