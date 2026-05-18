package com.gov.sg.baby_bonus_enrollment.external.ica

import com.gov.sg.baby_bonus_enrollment.domain.Citizenship
import java.time.LocalDate

data class ChildRecord(
    val nric: String,
    val name: String,
    val dateOfBirth: LocalDate,
    val citizenship: Citizenship
)
