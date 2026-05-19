package com.gov.sg.baby_bonus_enrollment.external.disbursement

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
class MockDisbursementClientTest {

    @Autowired private lateinit var disbursementClient: DisbursementClient

    @Test
    fun `returns a processed result with a unique disbursement ID`() {
        val result = disbursementClient.initiate(UUID.randomUUID(), DisbursementType.CASH_GIFT, BigDecimal("3000.00"))

        result.disbursementId shouldNotBe null
        result.status shouldBe DisbursementStatus.PROCESSED
        result.processedAt shouldNotBe null
    }

    @Test
    fun `each invocation returns a different disbursement ID`() {
        val enrollmentId = UUID.randomUUID()
        val first = disbursementClient.initiate(enrollmentId, DisbursementType.CASH_GIFT, BigDecimal("3000.00"))
        val second = disbursementClient.initiate(enrollmentId, DisbursementType.CASH_GIFT, BigDecimal("3000.00"))

        first.disbursementId shouldNotBe second.disbursementId
    }
}
