package com.gov.sg.baby_bonus_enrollment.external.disbursement

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Component
class MockDisbursementClient : DisbursementClient {

    override fun initiate(enrollmentId: UUID, type: DisbursementType, amount: BigDecimal): DisbursementResult =
        DisbursementResult(UUID.randomUUID(), DisbursementStatus.PROCESSED, Instant.now())
}
