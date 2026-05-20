package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import com.gov.sg.baby_bonus_enrollment.usecase.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetEnrollmentByIdUseCaseTest {

    @Mock private lateinit var enrollmentRepository: EnrollmentEntityRepository
    @Mock private lateinit var disbursementRepository: DisbursementEntityRepository

    @InjectMocks private lateinit var useCase: GetEnrollmentByIdUseCase

    @Test
    fun `returns enrollment dto with masked nrics and disbursement`() {
        val enrollmentId = UUID.randomUUID()
        val disbursementId = UUID.randomUUID()
        val expectedEnrolledAt = Instant.parse("2025-01-15T10:00:00Z")
        val expectedProcessedAt = Instant.parse("2025-01-15T10:00:01Z")
        whenever(enrollmentRepository.findById(enrollmentId)).thenReturn(
            Enrollment(enrollmentId, "T2400001A", "S8001234A", Relationship.FATHER, EnrollmentStatus.ENROLLED, enrolledAt = expectedEnrolledAt)
        )
        whenever(disbursementRepository.findByEnrollmentId(enrollmentId)).thenReturn(
            Disbursement(disbursementId, enrollmentId, DisbursementType.CASH_GIFT, BigDecimal("3000.00"), DisbursementStatus.PROCESSED, expectedProcessedAt)
        )

        val result = useCase.execute(enrollmentId)

        with(result) {
            id shouldBe enrollmentId
            childNric shouldBe "T240****A"
            parentNric shouldBe "S800****A"
            status shouldBe EnrollmentStatus.ENROLLED
            enrolledAt shouldBe expectedEnrolledAt
            with(disbursement!!) {
                id shouldBe disbursementId
                type shouldBe DisbursementType.CASH_GIFT
                amount shouldBe BigDecimal("3000.00")
                status shouldBe DisbursementStatus.PROCESSED
                processedAt shouldBe expectedProcessedAt
            }
        }
    }

    @Test
    fun `returns enrollment dto with null disbursement when no disbursement exists`() {
        val enrollmentId = UUID.randomUUID()
        whenever(enrollmentRepository.findById(enrollmentId)).thenReturn(
            Enrollment(enrollmentId, "T2400001A", "S8001234A", Relationship.FATHER, EnrollmentStatus.INELIGIBLE)
        )
        whenever(disbursementRepository.findByEnrollmentId(enrollmentId)).thenReturn(null)

        val result = useCase.execute(enrollmentId)

        with(result) {
            status shouldBe EnrollmentStatus.INELIGIBLE
            disbursement shouldBe null
        }
    }

    @Test
    fun `throws NotFoundException when enrollment does not exist`() {
        val id = UUID.randomUUID()
        whenever(enrollmentRepository.findById(id)).thenReturn(null)

        val exception = shouldThrow<NotFoundException> { useCase.execute(id) }

        exception.message shouldBe "Enrollment not found"
    }
}
