## Domain Model

```
Child
  nric:         String        // Singapore NRIC (e.g. T2400001A)
  name:         String
  dateOfBirth:  LocalDate
  citizenship:  SINGAPORE_CITIZEN | PERMANENT_RESIDENT | FOREIGNER

Parent / Guardian
  nric:         String
  name:         String
  relationship: FATHER | MOTHER | LEGAL_GUARDIAN

Enrollment
  id:           UUID
  childNric:    String
  parentNric:   String
  relation:     FATHER | MOTHER | LEGAL_GUARDIAN
  status:       PENDING | ENROLLED | INELIGIBLE
  enrolledAt:   Instant?
  createdAt:    Instant

Disbursement
  id:           UUID
  enrollmentId: UUID
  type:         CASH_GIFT | CDA_DEPOSIT
  amount:       BigDecimal
  status:       PENDING | PROCESSED | FAILED
  processedAt:  Instant?
```

---
