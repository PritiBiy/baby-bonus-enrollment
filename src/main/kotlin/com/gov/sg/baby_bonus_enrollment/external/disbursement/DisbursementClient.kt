package com.gov.sg.baby_bonus_enrollment.external.disbursement

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import java.math.BigDecimal
import java.util.UUID

interface DisbursementClient {
    fun initiate(enrollmentId: UUID, type: DisbursementType, amount: BigDecimal): DisbursementResult
}
