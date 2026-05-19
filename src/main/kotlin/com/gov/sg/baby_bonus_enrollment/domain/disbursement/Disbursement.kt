package com.gov.sg.baby_bonus_enrollment.domain.disbursement

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Disbursement(
    val id: UUID = UUID.randomUUID(),
    val enrollmentId: UUID,
    val type: DisbursementType,
    val amount: BigDecimal,
    val status: DisbursementStatus,
    val processedAt: Instant? = null
)
