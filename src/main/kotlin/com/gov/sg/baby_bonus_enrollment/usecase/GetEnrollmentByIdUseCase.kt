package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.usecase.dto.EnrollmentDto
import com.gov.sg.baby_bonus_enrollment.usecase.exception.NotFoundException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetEnrollmentByIdUseCase(
    private val enrollmentRepository: EnrollmentEntityRepository,
    private val disbursementRepository: DisbursementEntityRepository
) {
    fun execute(id: UUID): EnrollmentDto {
        val enrollment = enrollmentRepository.findById(id)
            ?: throw NotFoundException("Enrollment not found")
        return EnrollmentDto.from(enrollment, disbursementRepository.findByEnrollmentId(id))
    }
}
