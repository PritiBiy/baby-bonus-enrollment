# Write Tests

**Before writing any test, read this file first.** Always refer to `.claude/commands/write-tests.md` for testing conventions in this project.

Before writing any test or code, also read:
- `CLAUDE.md` → **Design Principles (Kent Beck)** — TDD cycle, YAGNI, simple design rules
- `CLAUDE.md` → **Layer Rules** — which layer owns what, what each layer is allowed to depend on

---

## Cardinal Rule

**Always write the test first. Never write production code without a failing test.**

The test describes the desired behaviour. The production code exists only to make the test pass. If there is no test, the behaviour does not exist. This applies at every layer — HTTP, service, and repository.

---

## Test Layer Order

For each feature, follow this order:

```
1. HTTP integration test       ← written FIRST, @Disabled — locks the contract
2. Migration (SQL script)      ← schema before entity
3. Repository test             ← @DataJpaTest, verifies mappings and query methods
4. Service unit test           ← plain JUnit, mocked dependencies
5. Enable HTTP integration test ← remove @Disabled once layers 2–4 are green
```

The HTTP integration test is written **before any implementation exists** to lock the API contract. It will fail — mark it `@Disabled` so it does not block lower-layer work. Remove `@Disabled` only when all layers below it are passing.

---

## HTTP Integration Test

**When to write:** First — before entities, repositories, or services exist.

**Purpose:** Lock the API contract. Assert on HTTP response only.

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("enable once repository and service layers are tested")
class EnrollmentControllerTest {

    @Autowired lateinit var restTemplate: TestRestTemplate

    @MockitoBean lateinit var icaClient: IcaClient
    @MockitoBean lateinit var iroasClient: IroasClient
    @MockitoBean lateinit var disbursementClient: DisbursementClient
}
```

**What to mock — external HTTP clients only:**
`IcaClient`, `IroasClient`, and `DisbursementClient` are mocked because they represent calls that leave the project boundary (external HTTP services). Everything inside the project boundary runs for real: repositories, services, security filters, exception handlers.

**What NOT to mock:**
Internal Spring beans — `EnrollmentService`, repositories — let the full internal stack run.

**Assert only on HTTP response:**
- Status code
- Response body shape and field values
- NRIC masking — must be explicitly asserted:
  ```kotlin
  assertThat(response.childNric).isEqualTo("T240****A")
  assertThat(response.parentNric).isEqualTo("S800****A")
  ```

**Do not assert side effects** (e.g. "was the enrollment row saved?") — that belongs in repository tests.

One test class per controller. One test method per distinct scenario in `.claude/docs/api-contract.md`.

---

## Migration

**When to write:** Before defining the JPA entity.

Write SQL in `src/main/resources/schema.sql`. The entity is derived from the schema — not the reverse.

```sql
CREATE TABLE enrollment (
  id          UUID         PRIMARY KEY,
  child_nric  VARCHAR(10)  NOT NULL,
  -- ...
);
```

---

## Repository Test

**When to write:** After migration exists, before service.

**Purpose:** Verify JPA mappings, column types, nullable constraints, and enum persistence.

**`@DataJpaTest` does not exist in Spring Boot 4.x.** Use `@SpringBootTest` + `@Transactional` instead. The `@Transactional` annotation causes each test to roll back automatically, providing isolation. H2 is the actual runtime database — no TestContainers needed.

```kotlin
@SpringBootTest
@Transactional
class EnrollmentRepositoryTest {

    @Autowired lateinit var repository: EnrollmentRepository

    @Test
    fun `saves and reloads an enrollment with all fields intact`() { ... }
}
```

**Keep it to one test per repository** — save an entity with all fields set, reload by ID, assert each field. That single test covers mappings, column types, nullable constraints, and enum persistence in one pass.

**Side-effect assertions live here.** If you want to verify a record was created as a result of an operation, assert it in a repository test — not in the HTTP integration test.

---

## Service Unit Test

**When to write:** After repository test passes, before enabling the HTTP integration test.

**Purpose:** Verify business logic — eligibility rules, exception throwing, DTO mapping.

```kotlin
@ExtendWith(MockitoExtension::class)
class EnrollmentServiceTest {

    @Mock lateinit var enrollmentRepository: EnrollmentRepository
    @Mock lateinit var disbursementRepository: DisbursementRepository
    @Mock lateinit var icaClient: IcaClient
    @Mock lateinit var iroasClient: IroasClient
    @Mock lateinit var disbursementClient: DisbursementClient

    @InjectMocks lateinit var service: EnrollmentServiceImpl
}
```

**Rules:**
- No Spring context — plain JUnit + Mockito only
- One test per distinct behaviour path
- Test names are full sentences describing behaviour:
  ```kotlin
  fun `enrolling a child who is not a Singapore citizen throws EligibilityException`()
  fun `enrolling a child with an existing active enrollment throws DuplicateEnrollmentException`()
  ```
- Assert exceptions with exact message from `.claude/docs/api-contract.md`:
  ```kotlin
  assertThatThrownBy { service.enroll(request) }
      .isInstanceOf(EligibilityException::class.java)
      .hasMessage("Child is not a Singapore Citizen")
  ```

---

## Test Resources

```
src/main/resources/mock-data/    ← loaded by stub clients (IcaStubClient, IroasStubClient) at startup
src/test/resources/mock-data/    ← test fixtures; used by stub client unit tests and any test that needs JSON data
```

**Rules:**
- Stub clients (`IcaStubClient`, `IroasStubClient`) read from `src/main/resources/mock-data/` via `ClassPathResource` — they own all `ObjectMapper` / JSON parsing logic
- Tests that need JSON data (e.g. testing the stub client itself) load from `src/test/resources/mock-data/`
- **Never put `ObjectMapper` or JSON file parsing in a test.** If a test needs a parsed record, either construct it inline or delegate to the stub client
- The integration test mocks `IcaClient` at the interface level and returns typed records directly — it does not read JSON files at all
- Test resource files can diverge from production data to cover edge cases (e.g. a child with missing fields, a parent with an unusual NRIC)

**When to use WireMock instead:**
If `IcaClient` were a real HTTP client making calls to an external URL, WireMock would stub that URL and return JSON — exercising the full HTTP → deserialisation chain. That is not the design here. `MockIcaClient` *is* the ICA integration; the external boundary is the `IcaClient` interface, not a URL. Use interface-level mocking.

---

## What NOT to do

| Don't | Do instead |
|-------|-----------|
| Write production code before a failing test | Write the test first; let it drive the design |
| Assert DB state from the HTTP integration test | Assert DB state in a repository test |
| Mock internal Spring services in integration test | Let the full internal stack run |
| Mock repositories in integration test | Let real H2 + repositories run |
| Use TestContainers with H2 | Use `@DataJpaTest` — H2 is already the real DB |
| Use `!!` in test code | Use `assertNotNull` or `requireNotNull` |
| Put `ObjectMapper` / JSON parsing in a test | Construct records inline or let the stub client own parsing |
| Read from `src/main/resources/mock-data/` in tests | Use `src/test/resources/mock-data/` for test fixtures |

---

## Package Structure

```
src/test/kotlin/com/gov/sg/baby_bonus_enrollment/
├── controller/
│   └── EnrollmentControllerTest.kt      ← @Disabled until all lower layers pass
├── repository/
│   ├── EnrollmentRepositoryTest.kt
│   └── DisbursementRepositoryTest.kt
└── service/
    └── EnrollmentServiceTest.kt
```

---

## Reference

- Design principles and TDD cycle: `CLAUDE.md` → Design Principles (Kent Beck)
- Layer ownership and dependency rules: `CLAUDE.md` → Layer Rules
- API contracts (status codes, error messages, response shapes): `.claude/docs/api-contract.md`
- Domain model (field types, enums): `.claude/docs/domain-model.md`
