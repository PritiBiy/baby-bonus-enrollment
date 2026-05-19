package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Citizenship
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementClient
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementResult
import com.gov.sg.baby_bonus_enrollment.external.ica.ChildRecord
import com.gov.sg.baby_bonus_enrollment.external.ica.IcaClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.IroasClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.ParentRecord
import com.gov.sg.baby_bonus_enrollment.usecase.dto.CreateEnrollmentDto
import com.gov.sg.baby_bonus_enrollment.usecase.exception.DuplicateEnrollmentException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EligibilityException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EligibilityReason
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class EnrollChildUseCaseTest {

    @Mock lateinit var enrollmentRepository: EnrollmentEntityRepository
    @Mock lateinit var disbursementRepository: DisbursementEntityRepository
    @Mock lateinit var icaClient: IcaClient
    @Mock lateinit var iroasClient: IroasClient
    @Mock lateinit var disbursementClient: DisbursementClient

    private val fixedInstant = Instant.parse("2025-01-15T10:00:00Z")
    private val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private lateinit var useCase: EnrollChildUseCase

    @BeforeEach
    fun setUp() {
        useCase = EnrollChildUseCase(
            enrollmentRepository, disbursementRepository,
            icaClient, iroasClient, disbursementClient,
            fixedClock
        )
    }

    private val childNric = "T2400001A"
    private val parentNric = "S8001234A"
    private val request = CreateEnrollmentDto(childNric, parentNric, Relationship.FATHER)

    @Test
    fun `enrolling a Singapore Citizen child with no prior enrollment creates ENROLLED status and initiates CASH_GIFT disbursement`() {
        val child = ChildRecord(childNric, "Test Child", LocalDate.of(2024, 1, 1), Citizenship.SINGAPORE_CITIZEN)
        val parent = ParentRecord(parentNric, "Test Parent")
        val savedEnrollment = Enrollment(
            childNric = childNric, parentNric = parentNric,
            relationship = Relationship.FATHER, status = EnrollmentStatus.ENROLLED,
            enrolledAt = fixedInstant
        )
        val disbursementResult = DisbursementResult(UUID.randomUUID(), DisbursementStatus.PROCESSED, fixedInstant)

        whenever(icaClient.findChild(childNric)).thenReturn(child)
        whenever(iroasClient.findParent(parentNric)).thenReturn(parent)
        whenever(enrollmentRepository.findByChildNric(childNric)).thenReturn(emptyList())
        whenever(enrollmentRepository.save(any())).thenReturn(savedEnrollment)
        whenever(disbursementClient.initiate(any(), eq(DisbursementType.CASH_GIFT), eq(BigDecimal("3000.00")))).thenReturn(disbursementResult)
        whenever(disbursementRepository.save(any())).thenAnswer { invocation -> invocation.arguments[0] }

        val result = useCase.execute(request)

        result.childNric shouldBe "T240****A"
        result.parentNric shouldBe "S800****A"
        result.relationship shouldBe Relationship.FATHER
        result.status shouldBe EnrollmentStatus.ENROLLED
        result.enrolledAt shouldBe fixedInstant
        result.disbursement shouldNotBe null
        result.disbursement!!.type shouldBe DisbursementType.CASH_GIFT
        result.disbursement.amount.compareTo(BigDecimal("3000.00")) shouldBe 0
        result.disbursement.status shouldBe DisbursementStatus.PROCESSED
        result.disbursement.processedAt shouldBe fixedInstant
    }

    @Test
    fun `enrolling throws EligibilityException when child is not found in ICA`() {
        whenever(icaClient.findChild(childNric)).thenReturn(null)

        val exception = shouldThrow<EligibilityException> { useCase.execute(request) }
        exception.reason shouldBe EligibilityReason.CHILD_NOT_FOUND
    }

    @Test
    fun `enrolling throws EligibilityException when child is not a Singapore Citizen`() {
        val child = ChildRecord(childNric, "Test Child", LocalDate.of(2024, 1, 1), Citizenship.PERMANENT_RESIDENT)
        whenever(icaClient.findChild(childNric)).thenReturn(child)

        val exception = shouldThrow<EligibilityException> { useCase.execute(request) }
        exception.reason shouldBe EligibilityReason.NOT_SINGAPORE_CITIZEN
    }

    @Test
    fun `enrolling throws EligibilityException when parent is not found in IROAS`() {
        val child = ChildRecord(childNric, "Test Child", LocalDate.of(2024, 1, 1), Citizenship.SINGAPORE_CITIZEN)
        whenever(icaClient.findChild(childNric)).thenReturn(child)
        whenever(iroasClient.findParent(parentNric)).thenReturn(null)

        val exception = shouldThrow<EligibilityException> { useCase.execute(request) }
        exception.reason shouldBe EligibilityReason.PARENT_NOT_FOUND
    }

    @Test
    fun `enrolling throws DuplicateEnrollmentException when child already has an active enrollment`() {
        val child = ChildRecord(childNric, "Test Child", LocalDate.of(2024, 1, 1), Citizenship.SINGAPORE_CITIZEN)
        val parent = ParentRecord(parentNric, "Test Parent")
        val existingEnrollment = Enrollment(
            childNric = childNric, parentNric = parentNric,
            relationship = Relationship.FATHER, status = EnrollmentStatus.ENROLLED
        )
        whenever(icaClient.findChild(childNric)).thenReturn(child)
        whenever(iroasClient.findParent(parentNric)).thenReturn(parent)
        whenever(enrollmentRepository.findByChildNric(childNric)).thenReturn(listOf(existingEnrollment))

        val exception = shouldThrow<DuplicateEnrollmentException> { useCase.execute(request) }
        exception.message shouldBe "Child already has an active enrollment"
    }
}
