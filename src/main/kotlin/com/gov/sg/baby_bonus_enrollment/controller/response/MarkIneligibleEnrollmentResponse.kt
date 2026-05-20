package com.gov.sg.baby_bonus_enrollment.controller.response

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import java.util.UUID

data class MarkIneligibleEnrollmentResponse(
    val id: UUID,
    val childNric: String,
    val parentNric: String,
    val status: EnrollmentStatus,
    val reason: String
)
