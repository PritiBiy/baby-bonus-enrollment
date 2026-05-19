package com.gov.sg.baby_bonus_enrollment.repository

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
    fun `saves and reloads enrollment with all fields intact`() {
        val entity = EnrollmentEntity(
            childNric = "T2400001A",
            parentNric = "S8001234A",
            relationship = Relationship.FATHER,
            status = EnrollmentStatus.ENROLLED,
            reason = null,
            enrolledAt = Instant.parse("2025-01-15T10:00:00Z"),
            createdAt = Instant.parse("2025-01-15T09:00:00Z")
        )

        repository.save(entity)
        val loaded = repository.findById(entity.id).orElseThrow()

        assertEquals("T2400001A", loaded.childNric)
        assertEquals("S8001234A", loaded.parentNric)
        assertEquals(Relationship.FATHER, loaded.relationship)
        assertEquals(EnrollmentStatus.ENROLLED, loaded.status)
        assertNull(loaded.reason)
        assertEquals(Instant.parse("2025-01-15T10:00:00Z"), loaded.enrolledAt)
    }
}
