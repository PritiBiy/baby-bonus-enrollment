package com.gov.sg.baby_bonus_enrollment.controller.request

data class EnrollmentRequest(
    val childNric: String,
    val parentNric: String,
    // TODO: add @Valid and constrain relationship to Relationship enum values (return 400 on invalid value)
    val relationship: String
)
