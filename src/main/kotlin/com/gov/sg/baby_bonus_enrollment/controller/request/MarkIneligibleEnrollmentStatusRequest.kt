package com.gov.sg.baby_bonus_enrollment.controller.request

import jakarta.validation.constraints.NotBlank

data class MarkIneligibleEnrollmentStatusRequest(
    @field:NotBlank(message = "reason must not be blank")
    val reason: String
)
