package com.gov.sg.baby_bonus_enrollment.usecase

import com.gov.sg.baby_bonus_enrollment.audit.AuditLogger
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MarkEnrollmentIneligibleUseCaseTest {

    @Mock private lateinit var enrollmentRepository: EnrollmentEntityRepository
    @Mock private lateinit var auditLogger: AuditLogger

    private lateinit var useCase: MarkEnrollmentIneligibleUseCase

    @BeforeEach
    fun setUp() {
        useCase = MarkEnrollmentIneligibleUseCase(enrollmentRepository, auditLogger)
    }

    @Test
    fun `marks enrollment ineligible and returns updated enrollment`() {
        val id = UUID.randomUUID()
        val reason = "Child citizenship data incorrect"
        val enrollment = Enrollment(
            id = id,
            childNric = "T2400001A",
            parentNric = "S8001234A",
            relationship = Relationship.FATHER,
            status = EnrollmentStatus.INELIGIBLE,
            reason = reason,
            enrolledAt = Instant.parse("2025-01-15T10:00:00Z")
        )
        whenever(enrollmentRepository.updateStatus(id, reason)).thenReturn(enrollment)

        val result = useCase.execute(id, reason)

        result.id shouldBe id
        result.status shouldBe EnrollmentStatus.INELIGIBLE
        result.reason shouldBe reason
    }
}
