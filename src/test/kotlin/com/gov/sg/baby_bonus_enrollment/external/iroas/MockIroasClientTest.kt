package com.gov.sg.baby_bonus_enrollment.external.iroas

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MockIroasClientTest {

    @Autowired private lateinit var iroasClient: IroasClient

    @Test
    fun `returns parent record for known NRIC`() {
        val parent = iroasClient.findParent("S8001234A")

        parent shouldNotBe null
        parent!!.nric shouldBe "S8001234A"
        parent.name shouldBe "Tan Ah Kow"
    }

    @Test
    fun `returns null for unknown NRIC`() {
        iroasClient.findParent("X9999999Z") shouldBe null
    }
}
