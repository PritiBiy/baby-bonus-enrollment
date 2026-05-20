package com.gov.sg.baby_bonus_enrollment.controller.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class EnrollmentRequest(
    @field:NotBlank(message = "childNric must not be blank")
    val childNric: String,
    @field:NotBlank(message = "parentNric must not be blank")
    val parentNric: String,
    @field:NotNull(message = "relationship must not be null")
    @field:NotBlank(message = "relationship must not be blank")
    val relationship: String
)
