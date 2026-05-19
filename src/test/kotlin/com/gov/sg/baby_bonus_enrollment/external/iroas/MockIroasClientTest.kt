package com.gov.sg.baby_bonus_enrollment.external.iroas

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
class MockIroasClientTest {

    @Autowired private lateinit var iroasClient: IroasClient

    @Test
    fun `returns parent record for known NRIC`() {
        val parent = iroasClient.findParent("S8001234A")

        assertNotNull(parent)
        assertEquals("S8001234A", parent.nric)
        assertEquals("Tan Ah Kow", parent.name)
    }

    @Test
    fun `returns null for unknown NRIC`() {
        assertNull(iroasClient.findParent("X9999999Z"))
    }
}
