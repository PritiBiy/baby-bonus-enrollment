package com.gov.sg.baby_bonus_enrollment.usecase.dto

import com.gov.sg.baby_bonus_enrollment.domain.Nric
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import java.time.Instant
import java.util.UUID

data class EnrollmentDto(
    val id: UUID,
    val childNric: String,
    val parentNric: String,
    val relationship: Relationship,
    val status: EnrollmentStatus,
    val enrolledAt: Instant?,
    val disbursement: DisbursementDto?
) {
    companion object {
        fun from(enrollment: Enrollment, disbursement: Disbursement?) = EnrollmentDto(
            id = enrollment.id,
            childNric = Nric(enrollment.childNric).masked(),
            parentNric = Nric(enrollment.parentNric).masked(),
            relationship = enrollment.relationship,
            status = enrollment.status,
            enrolledAt = enrollment.enrolledAt,
            disbursement = disbursement?.let {
                DisbursementDto(it.id, it.type, it.amount, it.status, it.processedAt)
            }
        )
    }
}
