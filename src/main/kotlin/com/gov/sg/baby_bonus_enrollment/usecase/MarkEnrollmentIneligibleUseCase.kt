package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.Nric
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.usecase.dto.DisbursementDto
import com.gov.sg.baby_bonus_enrollment.usecase.dto.EnrollmentDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MarkEnrollmentIneligibleUseCase(
    private val enrollmentRepository: EnrollmentEntityRepository,
    private val disbursementRepository: DisbursementEntityRepository
) {
    fun execute(id: UUID, reason: String): EnrollmentDto {
        val updated = enrollmentRepository.updateStatus(id, reason)
        val disbursement = disbursementRepository.findByEnrollmentId(id)
        return toDto(updated, disbursement)
    }

    private fun toDto(enrollment: Enrollment, disbursement: Disbursement?): EnrollmentDto =
        EnrollmentDto(
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
