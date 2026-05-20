package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EnrollmentAlreadyIneligibleException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID

@SpringBootTest
@Transactional
class EnrollmentEntityRepositoryTest {

    @Autowired private lateinit var repository: EnrollmentEntityRepository
    @Autowired private lateinit var jpaRepository: EnrollmentJpaRepository

    @Test
    fun `save persists enrollment fields to database`() {
        val enrollment = enrollment()

        repository.save(enrollment)

        val entity = jpaRepository.findById(enrollment.id).orElseThrow()
        entity.id shouldBe enrollment.id
        entity.childNric shouldBe enrollment.childNric
        entity.parentNric shouldBe enrollment.parentNric
        entity.relationship shouldBe enrollment.relationship
        entity.status shouldBe enrollment.status
        entity.enrolledAt shouldBe enrollment.enrolledAt
    }

    @Test
    fun `findById returns null for unknown id`() {
        repository.findById(UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `findByChildNric returns all enrollments for a child`() {
        val first = enrollment(childNric = "T2400001A")
        val second = enrollment(childNric = "T2400001A")
        val other = enrollment(childNric = "T2400002B")
        listOf(first, second, other).forEach { repository.save(it) }

        val results = repository.findByChildNric("T2400001A")

        results shouldBe listOf(first, second)
    }

    @Test
    fun `findByChildNric returns empty list when no enrollment exists`() {
        repository.findByChildNric("X9999999Z") shouldBe emptyList()
    }

    @Test
    fun `updateStatus sets status to INELIGIBLE and stores reason without resetting createdAt`() {
        val enrollment = enrollment()
        repository.save(enrollment)
        val createdAt = jpaRepository.findById(enrollment.id).orElseThrow().createdAt

        repository.updateStatus(enrollment.id, "data incorrect")

        val entity = jpaRepository.findById(enrollment.id).orElseThrow()
        entity.status shouldBe EnrollmentStatus.INELIGIBLE
        entity.reason shouldBe "data incorrect"
        entity.createdAt shouldBe createdAt
    }

    @Test
    fun `updateStatus throws NotFoundException for unknown enrollment id`() {
        shouldThrow<NotFoundException> {
            repository.updateStatus(UUID.randomUUID(), "data incorrect")
        }
    }

    @Test
    fun `updateStatus throws EnrollmentAlreadyIneligibleException when enrollment is already INELIGIBLE`() {
        val enrollment = enrollment(status = EnrollmentStatus.INELIGIBLE)
        repository.save(enrollment)

        shouldThrow<EnrollmentAlreadyIneligibleException> {
            repository.updateStatus(enrollment.id, "data incorrect")
        }
    }

    private fun enrollment(
        childNric: String = "T2400001A",
        status: EnrollmentStatus = EnrollmentStatus.ENROLLED
    ) = Enrollment(
        childNric = childNric,
        parentNric = "S8001234A",
        relationship = Relationship.FATHER,
        status = status,
        enrolledAt = Instant.parse("2025-01-15T10:00:00Z")
    )
}
