package com.gov.sg.baby_bonus_enrollment.usecase.exception

class EligibilityException(val reason: EligibilityReason) : RuntimeException(reason.message)
