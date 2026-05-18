package com.gov.sg.baby_bonus_enrollment.external.disbursement

import com.gov.sg.baby_bonus_enrollment.domain.DisbursementStatus
import java.time.Instant
import java.util.UUID

data class DisbursementResult(
    val disbursementId: UUID,
    val status: DisbursementStatus,
    val processedAt: Instant
)
