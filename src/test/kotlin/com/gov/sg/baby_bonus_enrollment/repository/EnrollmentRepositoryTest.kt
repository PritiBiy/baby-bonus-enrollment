package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentRepository
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class EnrollmentRepositoryTest {

    @Autowired private lateinit var repository: EnrollmentRepository

    @Test
    fun `save and findById returns saved enrollment`() {
        val enrollment = enrollment()

        repository.save(enrollment)

        assertEquals(enrollment, repository.findById(enrollment.id))
    }

    @Test
    fun `findById returns null for unknown id`() {
        assertNull(repository.findById(java.util.UUID.randomUUID()))
    }

    @Test
    fun `findByChildNric returns all enrollments for a child`() {
        val first = enrollment(childNric = "T2400001A")
        val second = enrollment(childNric = "T2400001A")
        val other = enrollment(childNric = "T2400002B")
        listOf(first, second, other).forEach { repository.save(it) }

        val results = repository.findByChildNric("T2400001A")

        assertEquals(listOf(first, second), results)
    }

    @Test
    fun `findByChildNric returns empty list when no enrollment exists`() {
        assertEquals(emptyList(), repository.findByChildNric("X9999999Z"))
    }

    private fun enrollment(childNric: String = "T2400001A") = Enrollment(
        childNric = childNric,
        parentNric = "S8001234A",
        relationship = Relationship.FATHER,
        status = EnrollmentStatus.ENROLLED,
        enrolledAt = Instant.parse("2025-01-15T10:00:00Z")
    )
}
