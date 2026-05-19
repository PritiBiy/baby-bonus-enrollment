package com.gov.sg.baby_bonus_enrollment.audit

import com.gov.sg.baby_bonus_enrollment.domain.Nric
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class AuditLogger {
    private val log = LoggerFactory.getLogger(AuditLogger::class.java)

    fun enrollmentSubmitted(childNric: Nric, parentNric: Nric) {
        log.info("ENROLLMENT_SUBMITTED childNric={} parentNric={}", childNric, parentNric)
    }

    fun eligibilityPassed(childNric: Nric) {
        log.info("ELIGIBILITY_PASSED childNric={}", childNric)
    }

    fun eligibilityFailed(childNric: Nric, reason: String) {
        log.info("ELIGIBILITY_FAILED childNric={} reason={}", childNric, reason)
    }

    fun disbursementInitiated(enrollmentId: UUID, amount: BigDecimal) {
        log.info("DISBURSEMENT_INITIATED enrollmentId={} amount={}", enrollmentId, amount)
    }
}
