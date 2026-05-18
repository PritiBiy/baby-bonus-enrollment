### Scope 

### In scope
Endpoints to be implemented for the MVP:

| Method                                                                                                                                                        | Path                                                                                                                                                 | Description                                                                                                                     |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `POST`                                                                                                                                                        | `/api/v1/enrollments`                                                                                                                                | Submit an enrollment application                                                                                                |
| `GET`                                                                                                                                                         | `/api/v1/enrollments/{id}`                                                                                                                           | Retrieve enrollment status and disbursement details                                                                             |
| `GET`                                                                                                                                                         | /api/v1/enrollments?childNric=...`                                                                                                                   | List enrollments — retrieve all enrollments for a given child                                                                   |
| `PATCH` |  `/api/v1/enrollments/{id}/ineligible` | Error correction: ops function to override an enrollment to INELIGIBLE with a reason, for cases where source data was incorrect |



### Reasoning
For an MVP, the following are necessary and sufficient to launch:

1. **Enrollment submission and status retrieval** — the core write and read cycle. Without
   these two endpoints the service has no purpose.

2. **List enrollments by child** — included despite a child having at most one valid
   enrollment at any time. A child may have prior attempts that were rejected due to
   incorrect data supplied by the applicant, or stale data in ICA or IROAS. The enrollment
   history is meaningful for ops visibility and requires no additional service dependencies —
   it completes the read cycle without external calls.

3. **Error correction (`PATCH /ineligible`)** — necessary for MVP. When source data is
   found to be incorrect after the fact, ops need a way to override the record. A simple
   status update endpoint is sufficient for now.

4. **Refund is deferred.** Reversing an erroneous disbursement is a genuinely complex
   operation. Errors can arise from multiple sources:
    - Source data was incorrect at the time of enrollment — stale IROAS records, or a
      citizenship record in ICA that was later corrected
    - The application passed all eligibility checks but the declared relationship between
      parent and child was false — not detectable at enrollment time, only discovered after
      disbursement
    - A duplicate disbursement caused by a retry — the enrollment was resubmitted and the
      disbursement service processed the message twice despite idempotency checks
    - Bank account details were incorrect and funds were sent to the wrong account

   Each scenario has a different resolution path and a different risk profile. Handling
   refunds correctly requires proper checks, a clear ops process, and coordination with the
   disbursement service. It is deferred until those can be designed properly.


### Decisions and Assumptions
- Enrollment need to specify the relationship with baby. This is needed as the identity records can't specify the relation. Thus updating a domain model to include relationship.
- In production, disbursement would be a separate service. The enrollment service would make a thin synchronous call to obtain a disbursement ID, then receive the outcome asynchronously via a message queue to update the status This service later can evolve to support multiple disbursement types (e.g. CDA deposit). For the MVP, we will keep it simple by making a synchronous call to a stubbed disbursement service that returns a successful outcome immediately. This allows us to focus on core enrollment logic without needing to implement messaging infrastructure or handle asynchronous callbacks yet. This will also allow refunds to be taken care by this service instead of Enrollment and it can act as an orchestrator to pull data and send response back. 

```
Enrollment Service                    Disbursement Service
       |                                      |
       |-- POST /disbursements -------------->|  (sync call, thin)
       |                                      |  generates disbursement ID
       |<-- 202 Accepted + disbursementId ----|  saves as PENDING
       |                                      |  returns immediately
       |  saves enrollmentId + disbursementId |
       |  status = PENDING                    |
       |                                      |  processes async
       |                                      |  publishes event to queue
       |<-- DisbursementProcessedEvent -------|
       |                                      |
       | updates disbursement status          |
       | PENDING → PROCESSED / FAILED         | 
```