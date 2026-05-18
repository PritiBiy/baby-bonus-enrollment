# Write Tests

Follow these conventions when writing any test in this project.

Before writing any test or code, read:
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

**Purpose:** Verify JPA mappings, column types, nullable constraints, enum persistence, and custom query methods.

**No TestContainers needed.** H2 is the actual runtime database for this service — not a substitute for a different production DB. `@DataJpaTest` applies `schema.sql` and provides a clean in-memory H2 instance per test. Adding TestContainers would spin up a containerised H2 with no added value. Revisit if the project migrates to PostgreSQL.

```kotlin
@DataJpaTest
class EnrollmentRepositoryTest {

    @Autowired lateinit var enrollmentRepository: EnrollmentRepository

    @Test
    fun `saves and reloads an enrollment with all fields intact`() { ... }

    @Test
    fun `finds enrollments by child NRIC`() { ... }

    @Test
    fun `returns empty list when no enrollment exists for child NRIC`() { ... }
}
```

**Assert:**
- Fields persist and reload with correct values and types
- Nullable fields accept null (`enrolledAt`, `reason`)
- Enum columns map correctly (`status`, `relationship`)
- Custom query methods return correct results

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

## What NOT to do

| Don't | Do instead |
|-------|-----------|
| Write production code before a failing test | Write the test first; let it drive the design |
| Assert DB state from the HTTP integration test | Assert DB state in a repository test |
| Mock internal Spring services in integration test | Let the full internal stack run |
| Mock repositories in integration test | Let real H2 + repositories run |
| Use TestContainers with H2 | Use `@DataJpaTest` — H2 is already the real DB |
| Use `!!` in test code | Use `assertNotNull` or `requireNotNull` |

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
