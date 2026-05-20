# Data Sensitivity

Covers how NRIC identifiers and financial records are protected in production.

---

## Data Classification

| Data | Sensitivity | Reason |
|------|-------------|--------|
| Child / Parent NRIC | High | Unique national identifier |
| Enrollment status | Medium | Reveals eligibility outcome |
| Cash gift / CDA amount | Medium | Financial record tied to individual |
| Disbursement status | Medium | Reveals payment state |
| Audit logs | High | Contains masked PII and financial event history |

---

## Data at Rest

| Area | Control |
|------|---------|
| Storage encryption | AES-256 enabled at the PostgreSQL storage layer — protects against raw disk access |
| NRIC columns | Application-layer column encryption backed by KMS — protects against privileged DB access even if storage encryption is bypassed |
| DB credentials | Managed via secrets manager (e.g. AWS Secrets Manager), rotated automatically, never in source control |
| DB access | Least privilege — application user has `SELECT`, `INSERT`, `UPDATE` on required tables only; no `DROP` or cross-schema access |
| Backups | Encrypted with same KMS key policy as live database |

---

## Data in Transit

| Area | Control |
|------|---------|
| Transport | HTTPS enforced — TLS 1.2 minimum, TLS 1.3 preferred; plain HTTP rejected |
| API keys | Transmitted in `X-API-Key` header — protected by TLS, never in plaintext on the wire |
| Internal calls | Service-to-service calls (e.g. to disbursement service) over mTLS |
| Protocol downgrade | HSTS headers set — prevents HTTP fallback |

---

## Data in Logs

| Rule | Detail |
|------|--------|
| NRIC | Always masked — `T2400001A` → `T240****A` — never logged in plaintext |
| API keys | Always masked — first 4 chars + `****` |
| Financial amounts | Audit log only — not in debug or application logs |
| Audit logs | Append-only store, access-controlled separately, retained per regulatory policy |
| Framework logging | Default Spring/Hibernate logging must be reviewed — often captures request params and headers containing PII |
| NRIC in query param | `?childNric=` is logged by default in access logs — scrub at API gateway or move to request body in production |

---

## Access Control

| Area | Control |
|------|---------|
| API keys | Rotate every 90 days; rotate immediately on suspected compromise; support multiple valid keys during rotation |
| Production auth | Replace static API key with short-lived JWT or OAuth2 client credentials |
| Ops endpoints | `PATCH /ineligible` requires elevated credentials — not accessible with the same key issued to external callers |
| Audit trail | All access logged with caller identity — audit log is the access record |

---

## Known Gaps in This Implementation

| Gap | Production Mitigation |
|-----|---------------------|
| NRIC stored in plaintext | Application-layer column encryption backed by KMS |
| Static API key | Replace with short-lived JWT or OAuth2 client credentials |
| NRIC in query parameter | Scrub at API gateway or move to request body |
| No key rotation mechanism | Implement multi-key support with rotation policy |
| Plain HTTP in development | Enforced HTTPS at load balancer; HTTP rejected at application level |
