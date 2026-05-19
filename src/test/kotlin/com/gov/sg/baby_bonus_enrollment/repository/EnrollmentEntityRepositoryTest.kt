package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
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

    private fun enrollment(childNric: String = "T2400001A") = Enrollment(
        childNric = childNric,
        parentNric = "S8001234A",
        relationship = Relationship.FATHER,
        status = EnrollmentStatus.ENROLLED,
        enrolledAt = Instant.parse("2025-01-15T10:00:00Z")
    )
}
