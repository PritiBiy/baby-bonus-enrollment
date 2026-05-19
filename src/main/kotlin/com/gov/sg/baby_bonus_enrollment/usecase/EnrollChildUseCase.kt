package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Citizenship
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementClient
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
    private val clock: Clock
) {
    fun execute(request: CreateEnrollmentDto): EnrollmentDto {
        val child = icaClient.findChild(request.childNric)
            ?: throw EligibilityException(EligibilityReason.CHILD_NOT_FOUND)

        if (child.citizenship != Citizenship.SINGAPORE_CITIZEN) {
            throw EligibilityException(EligibilityReason.NOT_SINGAPORE_CITIZEN)
        }

        iroasClient.findParent(request.parentNric)
            ?: throw EligibilityException(EligibilityReason.PARENT_NOT_FOUND)

        val existing = enrollmentRepository.findByChildNric(request.childNric)
        if (existing.any { it.status == EnrollmentStatus.ENROLLED }) {
            throw DuplicateEnrollmentException("Child already has an active enrollment")
        }

        val enrollment = enrollmentRepository.save(
            Enrollment(
                childNric = request.childNric,
                parentNric = request.parentNric,
                relationship = request.relationship,
                status = EnrollmentStatus.ENROLLED,
                enrolledAt = Instant.now(clock)
            )
        )

        val result = disbursementClient.initiate(enrollment.id, DisbursementType.CASH_GIFT, BigDecimal("3000.00"))

        val disbursement = disbursementRepository.save(
            Disbursement(
                enrollmentId = enrollment.id,
                type = DisbursementType.CASH_GIFT,
                amount = BigDecimal("3000.00"),
                status = result.status,
                processedAt = result.processedAt
            )
        )

        return EnrollmentDto(
            id = enrollment.id,
            childNric = mask(enrollment.childNric),
            parentNric = mask(enrollment.parentNric),
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

    private fun mask(nric: String) = nric.take(4) + "****" + nric.last()
}

