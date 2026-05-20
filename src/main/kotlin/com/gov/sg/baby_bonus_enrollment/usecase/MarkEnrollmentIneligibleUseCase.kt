package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MarkEnrollmentIneligibleUseCase(
    private val enrollmentRepository: EnrollmentEntityRepository
) {
    fun execute(id: UUID, reason: String): Enrollment =
        enrollmentRepository.updateStatus(id, reason)
}
