package com.gov.sg.baby_bonus_enrollment.usecase.dto

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class DisbursementDto(
    val id: UUID,
    val type: DisbursementType,
    val amount: BigDecimal,
    val status: DisbursementStatus,
    val processedAt: Instant?
)
