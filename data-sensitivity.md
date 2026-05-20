# Data Sensitivity

This document describes how the Baby Bonus Enrollment Service protects sensitive data in a production deployment. The service handles NRIC identifiers and financial disbursement records for families applying for the Baby Bonus Scheme. Two categories of data require specific controls: national identity numbers (NRIC) and financial records (cash gift amounts, disbursement state).

---

## Data at Rest

| Area | Control |
|------|---------|
| Storage encryption | AES-256 at the PostgreSQL storage layer — protects against raw disk access |
| NRIC columns | Application-layer column encryption backed by KMS — protects against privileged DB access even if storage encryption is bypassed |
| DB credentials | Managed via secrets manager (e.g. AWS Secrets Manager), rotated automatically, never in source control |
| DB access | Least privilege — application user has `SELECT`, `INSERT`, `UPDATE` on required tables only; no `DROP` or cross-schema access |
| Backups | Encrypted with the same KMS key policy as the live database |

---

## Data in Transit

| Area | Control |
|------|---------|
| Transport | HTTPS enforced — TLS 1.2 minimum, TLS 1.3 preferred; plain HTTP rejected |
| API keys | Transmitted in `X-API-Key` header — protected by TLS; never in plaintext on the wire |
| Internal calls | Service-to-service calls (ICA, IROAS, disbursement service) over mTLS |
| Protocol downgrade | HSTS headers set — prevents HTTP fallback |

---

## Data in Logs

| Rule | Detail |
|------|--------|
| NRIC | Always masked — `T2400001A` → `T240****A` — never logged in plaintext; enforced by the `Nric` value class |
| Financial amounts | Structured audit log only — not in debug or application logs |
| Audit logs | Append-only, access-controlled separately, retained per regulatory policy |
| Framework logging | Default Spring/Hibernate logging must be reviewed — can capture request params and headers containing PII |
| NRIC in query param | `?childNric=` is captured in access logs by default — scrub at the API gateway or move to request body in production |

---

## Known Gaps in This Implementation

| Gap | Production Mitigation |
|-----|----------------------|
| H2 in-memory database | Replace with PostgreSQL; add AES-256 storage encryption and KMS-backed column encryption for NRIC fields |
| NRIC stored in plaintext | Application-layer column encryption backed by KMS |
| Static API key | Replace with short-lived JWT or OAuth2 client credentials with rotation policy |
| `PATCH /ineligible` shares the same API key as external callers | Ops endpoints should require separate elevated credentials — not accessible with the key issued to external callers |
| NRIC in query parameter | Scrub at API gateway or move to request body |
| Plain HTTP in development | Enforced HTTPS at load balancer; HTTP rejected at application level |
