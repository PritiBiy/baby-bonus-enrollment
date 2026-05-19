package com.gov.sg.baby_bonus_enrollment.usecase.dto

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import java.time.Instant
import java.util.UUID

data class EnrollmentDto(
    val id: UUID,
    val childNric: String,
    val parentNric: String,
    val relationship: Relationship,
    val status: EnrollmentStatus,
    val enrolledAt: Instant?,
    val disbursement: DisbursementDto?
)
