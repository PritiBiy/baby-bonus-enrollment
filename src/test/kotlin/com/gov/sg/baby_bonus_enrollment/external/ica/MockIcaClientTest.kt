package com.gov.sg.baby_bonus_enrollment.external.ica

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Citizenship
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class MockIcaClientTest {

    @Autowired private lateinit var icaClient: IcaClient

    @Test
    fun `returns child record for known NRIC`() {
        val child = icaClient.findChild("T2400001A")

        child shouldNotBe null
        child!!.nric shouldBe "T2400001A"
        child.name shouldBe "Tan Baby"
        child.dateOfBirth shouldBe LocalDate.of(2025, 1, 15)
        child.citizenship shouldBe Citizenship.SINGAPORE_CITIZEN
    }

    @Test
    fun `returns permanent resident child with correct citizenship`() {
        val child = icaClient.findChild("T2400002B")

        child shouldNotBe null
        child!!.citizenship shouldBe Citizenship.PERMANENT_RESIDENT
    }

    @Test
    fun `returns null for unknown NRIC`() {
        icaClient.findChild("X9999999Z") shouldBe null
    }
}
