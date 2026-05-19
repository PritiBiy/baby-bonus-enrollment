package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementRepository
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
import kotlin.test.assertNull

@SpringBootTest
@Transactional
class DisbursementRepositoryTest {

    @Autowired private lateinit var repository: DisbursementRepository

    @Test
    fun `save and findById returns saved disbursement`() {
        val disbursement = disbursement()

        repository.save(disbursement)

        assertEquals(disbursement, repository.findById(disbursement.id))
    }

    @Test
    fun `findById returns null for unknown id`() {
        assertNull(repository.findById(UUID.randomUUID()))
    }

    private fun disbursement() = Disbursement(
        enrollmentId = UUID.randomUUID(),
        type = DisbursementType.CASH_GIFT,
        amount = BigDecimal("3000.00"),
        status = DisbursementStatus.PROCESSED,
        processedAt = Instant.parse("2025-01-15T10:00:00Z")
    )
}
