package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
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
class GetEnrollmentsByChildNricUseCaseTest {

    @Mock private lateinit var enrollmentRepository: EnrollmentEntityRepository
    @Mock private lateinit var disbursementRepository: DisbursementEntityRepository

    @InjectMocks private lateinit var useCase: GetEnrollmentsByChildNricUseCase

    @Test
    fun `returns enrollment dtos with masked nrics and disbursement per enrollment`() {
        val nric = "T2400001A"
        val enrolledId = UUID.randomUUID()
        val ineligibleId = UUID.randomUUID()
        val disbursementId = UUID.randomUUID()
        val enrolledAt = Instant.parse("2025-01-15T10:00:00Z")

        whenever(enrollmentRepository.findByChildNric(nric)).thenReturn(
            listOf(
                Enrollment(enrolledId, nric, "S8001234A", Relationship.FATHER, EnrollmentStatus.ENROLLED, enrolledAt = enrolledAt),
                Enrollment(ineligibleId, nric, "S8201234B", Relationship.MOTHER, EnrollmentStatus.INELIGIBLE, reason = "data incorrect")
            )
        )
        whenever(disbursementRepository.findByEnrollmentId(enrolledId)).thenReturn(
            Disbursement(disbursementId, enrolledId, DisbursementType.CASH_GIFT, BigDecimal("3000.00"), DisbursementStatus.PROCESSED, Instant.parse("2025-01-15T10:00:01Z"))
        )
        whenever(disbursementRepository.findByEnrollmentId(ineligibleId)).thenReturn(null)

        val results = useCase.execute(nric)

        results.size shouldBe 2

        with(results[0]) {
            id shouldBe enrolledId
            childNric shouldBe "T240****A"
            parentNric shouldBe "S800****A"
            status shouldBe EnrollmentStatus.ENROLLED
            disbursement?.id shouldBe disbursementId
        }

        with(results[1]) {
            id shouldBe ineligibleId
            parentNric shouldBe "S820****B"
            status shouldBe EnrollmentStatus.INELIGIBLE
            disbursement shouldBe null
        }
    }

    @Test
    fun `returns empty list when no enrollments exist for child nric`() {
        whenever(enrollmentRepository.findByChildNric("X9999999Z")).thenReturn(emptyList())

        useCase.execute("X9999999Z") shouldBe emptyList()
    }
}
