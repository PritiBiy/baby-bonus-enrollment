package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.Nric
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.usecase.dto.DisbursementDto
import com.gov.sg.baby_bonus_enrollment.usecase.dto.EnrollmentDto
import org.springframework.stereotype.Component

@Component
class GetEnrollmentsByChildNricUseCase(
    private val enrollmentRepository: EnrollmentEntityRepository,
    private val disbursementRepository: DisbursementEntityRepository
) {
    fun execute(childNric: String): List<EnrollmentDto> =
        enrollmentRepository.findByChildNric(childNric).map { enrollment ->
            toDto(enrollment, disbursementRepository.findByEnrollmentId(enrollment.id))
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
