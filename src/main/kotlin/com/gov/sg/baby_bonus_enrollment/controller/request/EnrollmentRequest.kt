package com.gov.sg.baby_bonus_enrollment.controller.request

import jakarta.validation.constraints.NotBlank

data class EnrollmentRequest(
    @field:NotBlank(message = "childNric must not be blank")
    val childNric: String,
    @field:NotBlank(message = "parentNric must not be blank")
    val parentNric: String,
    val relationship: String
)
