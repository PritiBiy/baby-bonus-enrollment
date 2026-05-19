### Baby Enrollment Service

The Baby Bonus Scheme supports parents of newborn Singapore Citizens with two financial benefits:

- A **cash gift** paid directly to the parent's bank account
- A **Child Development Account (CDA)** — a ring-fenced savings account seeded by government top-ups, usable only at approved merchants

Parents apply to enrol their newborn child. The system checks eligibility against government sources of truth, creates an enrollment record, and initiates disbursement.

### Prerequisites

Java 21

### Running the service

The service requires an `API_KEY` environment variable. It will not start without one.

```bash
API_KEY=your-secret-key ./gradlew bootRun
```

### Running with Docker

```bash
docker build -t baby-bonus-enrollment .
docker run -e API_KEY=your-secret-key -p 8080:8080 baby-bonus-enrollment
```

### Authentication

All endpoints require an `X-API-Key` header. Any request with a missing or incorrect key receives `401 Unauthorised` — there is no distinction between the two cases.

```
X-API-Key: your-secret-key
```

**Key distribution:** The API key is a pre-shared secret. The operator sets `API_KEY` when starting the service and shares the value with authorised callers out-of-band (e.g. via a secrets manager or direct communication). There is no key-issuance endpoint.

### Running tests

```bash
./gradlew test
```

Tests use a fixed key (`test-api-key`) configured in `src/test/resources/application.properties` — no environment variable needed to run the test suite.

