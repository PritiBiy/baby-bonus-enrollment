package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.audit.AuditEvent
import com.gov.sg.baby_bonus_enrollment.audit.AuditEventType
import com.gov.sg.baby_bonus_enrollment.audit.AuditLogger
import com.gov.sg.baby_bonus_enrollment.audit.AuditOutcome
import com.gov.sg.baby_bonus_enrollment.domain.Nric
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MarkEnrollmentIneligibleUseCase(
    private val enrollmentRepository: EnrollmentEntityRepository,
    private val auditLogger: AuditLogger
) {
    fun execute(id: UUID, reason: String): Enrollment {
        val enrollment = enrollmentRepository.updateStatus(id, reason)
        auditMarkedIneligible(enrollment, reason)
        return enrollment
    }

    private fun auditMarkedIneligible(enrollment: Enrollment, reason: String) =
        auditLogger.warn(AuditEvent(
            event = AuditEventType.MARKED_INELIGIBLE,
            nric = Nric(enrollment.childNric),
            outcome = AuditOutcome.SUCCESS,
            extras = mapOf("enrollmentId" to enrollment.id, "reason" to reason)
        ))
}
