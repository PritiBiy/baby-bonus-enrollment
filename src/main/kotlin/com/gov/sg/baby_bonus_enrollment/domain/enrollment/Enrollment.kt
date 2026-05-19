package com.gov.sg.baby_bonus_enrollment.domain.enrollment

import java.time.Instant
import java.util.UUID

data class Enrollment(
    val id: UUID = UUID.randomUUID(),
    val childNric: String,
    val parentNric: String,
    val relationship: Relationship,
    val status: EnrollmentStatus,
    val reason: String? = null,
    val enrolledAt: Instant? = null
)
