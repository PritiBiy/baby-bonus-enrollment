package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.audit.AuditEvent
import com.gov.sg.baby_bonus_enrollment.audit.AuditLogger
import com.gov.sg.baby_bonus_enrollment.domain.Nric
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Citizenship
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementClient
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementResult
import com.gov.sg.baby_bonus_enrollment.external.ica.IcaClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.IroasClient
import com.gov.sg.baby_bonus_enrollment.usecase.dto.CreateEnrollmentDto
import com.gov.sg.baby_bonus_enrollment.usecase.dto.DisbursementDto
import com.gov.sg.baby_bonus_enrollment.usecase.dto.EnrollmentDto
import com.gov.sg.baby_bonus_enrollment.usecase.exception.DuplicateEnrollmentException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EligibilityException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EligibilityReason
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

internal val CASH_GIFT_AMOUNT: BigDecimal = BigDecimal("3000.00")

@Component
class EnrollChildUseCase(
    private val enrollmentRepository: EnrollmentEntityRepository,
    private val disbursementRepository: DisbursementEntityRepository,
    private val icaClient: IcaClient,
    private val iroasClient: IroasClient,
    private val disbursementClient: DisbursementClient,
    private val clock: Clock,
    private val auditLogger: AuditLogger
) {
    fun execute(request: CreateEnrollmentDto): EnrollmentDto {
        auditEnrollmentSubmitted(request.childNric, request.parentNric)

        checkEligibility(request)
        val enrollment = saveEnrollment(request)
        val disbursement = initiateDisbursement(enrollment)
        return toDto(enrollment, disbursement)
    }

    private fun checkEligibility(request: CreateEnrollmentDto) {
        val child = icaClient.findChild(request.childNric.value)
            ?: failEligibility(request.childNric, EligibilityReason.CHILD_NOT_FOUND)

        if (child.citizenship != Citizenship.SINGAPORE_CITIZEN)
            failEligibility(request.childNric, EligibilityReason.NOT_SINGAPORE_CITIZEN)

        iroasClient.findParent(request.parentNric.value)
            ?: failEligibility(request.childNric, EligibilityReason.PARENT_NOT_FOUND)

        if (enrollmentRepository.findByChildNric(request.childNric.value).any { it.status == EnrollmentStatus.ENROLLED }) {
            auditEligibilityFailed(request.childNric, "Child already has an active enrollment")
            throw DuplicateEnrollmentException("Child already has an active enrollment")
        }

        auditEligibilityPassed(request.childNric)
    }

    private fun failEligibility(childNric: Nric, reason: EligibilityReason): Nothing {
        auditEligibilityFailed(childNric, reason.message)
        throw EligibilityException(reason)
    }

    private fun saveEnrollment(request: CreateEnrollmentDto): Enrollment =
        enrollmentRepository.save(
            Enrollment(
                childNric = request.childNric.value,
                parentNric = request.parentNric.value,
                relationship = request.relationship,
                status = EnrollmentStatus.ENROLLED,
                enrolledAt = Instant.now(clock)
            )
        )

    private fun initiateDisbursement(enrollment: Enrollment): Disbursement {
        val result: DisbursementResult =
            disbursementClient.initiate(enrollment.id, DisbursementType.CASH_GIFT, CASH_GIFT_AMOUNT)
        val disbursement = disbursementRepository.save(
            Disbursement(
                id = result.disbursementId,
                enrollmentId = enrollment.id,
                type = DisbursementType.CASH_GIFT,
                amount = CASH_GIFT_AMOUNT,
                status = result.status,
                processedAt = result.processedAt
            )
        )
        auditDisbursementInitiated(enrollment, disbursement.amount)
        return disbursement
    }

    private fun toDto(enrollment: Enrollment, disbursement: Disbursement): EnrollmentDto =
        EnrollmentDto.from(enrollment, disbursement)

    private fun auditEnrollmentSubmitted(childNric: Nric, parentNric: Nric) =
        auditLogger.info(AuditEvent("ENROLLMENT_SUBMITTED", childNric, mapOf("parentNric" to parentNric)))

    private fun auditEligibilityPassed(childNric: Nric) =
        auditLogger.info(AuditEvent("ELIGIBILITY_PASSED", childNric))

    private fun auditEligibilityFailed(childNric: Nric, reason: String) =
        auditLogger.warn(AuditEvent("ELIGIBILITY_FAILED", childNric, mapOf("reason" to reason)))

    private fun auditDisbursementInitiated(enrollment: Enrollment, amount: BigDecimal) =
        auditLogger.info(AuditEvent("DISBURSEMENT_INITIATED", Nric(enrollment.childNric), mapOf("enrollmentId" to enrollment.id, "amount" to amount)))
}
