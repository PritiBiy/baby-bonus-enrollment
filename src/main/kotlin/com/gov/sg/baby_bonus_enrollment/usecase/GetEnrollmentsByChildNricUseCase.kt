package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.usecase.dto.EnrollmentDto
import org.springframework.stereotype.Component

@Component
class GetEnrollmentsByChildNricUseCase(
    private val enrollmentRepository: EnrollmentEntityRepository,
    private val disbursementRepository: DisbursementEntityRepository
) {
    fun execute(childNric: String): List<EnrollmentDto> =
        enrollmentRepository.findByChildNric(childNric).map { enrollment ->
            EnrollmentDto.from(enrollment, disbursementRepository.findByEnrollmentId(enrollment.id))
        }
}
