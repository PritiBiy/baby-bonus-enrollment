package com.gov.sg.baby_bonus_enrollment.usecase

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
        auditLogger.enrollmentSubmitted(request.childNric, request.parentNric)

        try {
            checkEligibility(request)
        } catch (e: EligibilityException) {
            auditLogger.eligibilityFailed(request.childNric, e.reason.message)
            throw e
        } catch (e: DuplicateEnrollmentException) {
            auditLogger.eligibilityFailed(request.childNric, e.message ?: "Duplicate enrollment")
            throw e
        }

        auditLogger.eligibilityPassed(request.childNric)

        val enrollment = saveEnrollment(request)
        val disbursement = initiateDisbursement(enrollment)

        auditLogger.disbursementInitiated(enrollment.id, disbursement.amount)

        return toDto(enrollment, disbursement)
    }

    private fun checkEligibility(request: CreateEnrollmentDto) {
        val child = icaClient.findChild(request.childNric.value)
            ?: throw EligibilityException(EligibilityReason.CHILD_NOT_FOUND)

        if (child.citizenship != Citizenship.SINGAPORE_CITIZEN) {
            throw EligibilityException(EligibilityReason.NOT_SINGAPORE_CITIZEN)
        }

        iroasClient.findParent(request.parentNric.value)
            ?: throw EligibilityException(EligibilityReason.PARENT_NOT_FOUND)

        val existing = enrollmentRepository.findByChildNric(request.childNric.value)
        if (existing.any { it.status == EnrollmentStatus.ENROLLED }) {
            throw DuplicateEnrollmentException("Child already has an active enrollment")
        }
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
            disbursementClient.initiate(enrollment.id, DisbursementType.CASH_GIFT, BigDecimal("3000.00"))
        return disbursementRepository.save(
            Disbursement(
                id = result.disbursementId,
                enrollmentId = enrollment.id,
                type = DisbursementType.CASH_GIFT,
                amount = BigDecimal("3000.00"),
                status = result.status,
                processedAt = result.processedAt
            )
        )
    }

    private fun toDto(enrollment: Enrollment, disbursement: Disbursement): EnrollmentDto =
        EnrollmentDto(
            id = enrollment.id,
            childNric = Nric(enrollment.childNric).masked(),
            parentNric = Nric(enrollment.parentNric).masked(),
            relationship = enrollment.relationship,
            status = enrollment.status,
            enrolledAt = enrollment.enrolledAt,
            disbursement = DisbursementDto(
                id = disbursement.id,
                type = disbursement.type,
                amount = disbursement.amount,
                status = disbursement.status,
                processedAt = disbursement.processedAt
            )
        )
}
