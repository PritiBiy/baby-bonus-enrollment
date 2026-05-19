package com.gov.sg.baby_bonus_enrollment.usecase.dto

import com.gov.sg.baby_bonus_enrollment.domain.Nric
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship

data class CreateEnrollmentDto(
    val childNric: Nric,
    val parentNric: Nric,
    val relationship: Relationship
)
