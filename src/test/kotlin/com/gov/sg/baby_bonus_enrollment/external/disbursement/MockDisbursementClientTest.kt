package com.gov.sg.baby_bonus_enrollment.external.disbursement

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
class MockDisbursementClientTest {

    @Autowired private lateinit var disbursementClient: DisbursementClient

    @Test
    fun `returns a processed result with a unique disbursement ID`() {
        val result = disbursementClient.initiate(UUID.randomUUID(), DisbursementType.CASH_GIFT, BigDecimal("3000.00"))

        assertNotNull(result.disbursementId)
        assertEquals(DisbursementStatus.PROCESSED, result.status)
        assertNotNull(result.processedAt)
    }

    @Test
    fun `each invocation returns a different disbursement ID`() {
        val enrollmentId = UUID.randomUUID()
        val first = disbursementClient.initiate(enrollmentId, DisbursementType.CASH_GIFT, BigDecimal("3000.00"))
        val second = disbursementClient.initiate(enrollmentId, DisbursementType.CASH_GIFT, BigDecimal("3000.00"))

        assert(first.disbursementId != second.disbursementId)
    }
}
