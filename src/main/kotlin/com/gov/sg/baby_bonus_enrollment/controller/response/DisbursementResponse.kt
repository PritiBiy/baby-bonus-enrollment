package com.gov.sg.baby_bonus_enrollment.controller.response

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class DisbursementResponse(
    val id: UUID,
    val type: DisbursementType,
    val amount: BigDecimal,
    val status: DisbursementStatus,
    val processedAt: Instant?
)
