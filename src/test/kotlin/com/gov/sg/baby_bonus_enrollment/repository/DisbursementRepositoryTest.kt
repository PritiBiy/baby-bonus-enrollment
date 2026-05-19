package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@Transactional
class DisbursementRepositoryTest {

    @Autowired private lateinit var repository: DisbursementRepository

    @Test
    fun `saves and reloads disbursement with all fields intact`() {
        val enrollmentId = UUID.randomUUID()
        val entity = DisbursementEntity(
            enrollmentId = enrollmentId,
            type = DisbursementType.CASH_GIFT,
            amount = BigDecimal("3000.00"),
            status = DisbursementStatus.PROCESSED,
            processedAt = Instant.parse("2025-01-15T10:00:00Z")
        )

        repository.save(entity)
        val loaded = repository.findById(entity.id).orElseThrow()

        assertEquals(enrollmentId, loaded.enrollmentId)
        assertEquals(DisbursementType.CASH_GIFT, loaded.type)
        assertEquals(BigDecimal("3000.00"), loaded.amount)
        assertEquals(DisbursementStatus.PROCESSED, loaded.status)
        assertNotNull(loaded.processedAt)
    }
}
