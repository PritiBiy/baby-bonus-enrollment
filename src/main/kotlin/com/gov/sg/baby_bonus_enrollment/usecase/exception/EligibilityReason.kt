package com.gov.sg.baby_bonus_enrollment.usecase.exception

enum class EligibilityReason(val message: String) {
    CHILD_NOT_FOUND("Child not found in ICA records"),
    NOT_SINGAPORE_CITIZEN("Child is not a Singapore Citizen"),
    PARENT_NOT_FOUND("Parent not found in IROAS records")
}
