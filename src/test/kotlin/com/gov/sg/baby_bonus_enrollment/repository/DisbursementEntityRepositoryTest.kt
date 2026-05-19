package com.gov.sg.baby_bonus_enrollment.repository

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.Disbursement
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementEntityRepository
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import io.kotest.matchers.shouldBe
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@SpringBootTest
@Transactional
class DisbursementEntityRepositoryTest {

    @Autowired private lateinit var repository: DisbursementEntityRepository
    @Autowired private lateinit var jpaRepository: DisbursementJpaRepository

    @Test
    fun `save persists disbursement fields to database`() {
        val disbursement = disbursement()

        repository.save(disbursement)

        val entity = jpaRepository.findById(disbursement.id).orElseThrow()
        entity.id shouldBe disbursement.id
        entity.enrollmentId shouldBe disbursement.enrollmentId
        entity.type shouldBe disbursement.type
        entity.amount shouldBe disbursement.amount
        entity.status shouldBe disbursement.status
        entity.processedAt shouldBe disbursement.processedAt
    }

    @Test
    fun `findById returns null for unknown id`() {
        repository.findById(UUID.randomUUID()) shouldBe null
    }

    private fun disbursement() = Disbursement(
        enrollmentId = UUID.randomUUID(),
        type = DisbursementType.CASH_GIFT,
        amount = BigDecimal("3000.00"),
        status = DisbursementStatus.PROCESSED,
        processedAt = Instant.parse("2025-01-15T10:00:00Z")
    )
}
