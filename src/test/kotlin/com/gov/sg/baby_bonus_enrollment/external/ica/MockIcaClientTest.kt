package com.gov.sg.baby_bonus_enrollment.external.ica

import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Citizenship
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
class MockIcaClientTest {

    @Autowired private lateinit var icaClient: IcaClient

    @Test
    fun `returns child record for known NRIC`() {
        val child = icaClient.findChild("T2400001A")

        assertNotNull(child)
        assertEquals("T2400001A", child.nric)
        assertEquals("Tan Baby", child.name)
        assertEquals(LocalDate.of(2025, 1, 15), child.dateOfBirth)
        assertEquals(Citizenship.SINGAPORE_CITIZEN, child.citizenship)
    }

    @Test
    fun `returns permanent resident child with correct citizenship`() {
        val child = icaClient.findChild("T2400002B")

        assertNotNull(child)
        assertEquals(Citizenship.PERMANENT_RESIDENT, child.citizenship)
    }

    @Test
    fun `returns null for unknown NRIC`() {
        assertNull(icaClient.findChild("X9999999Z"))
    }
}
