package com.gov.sg.baby_bonus_enrollment.audit

import com.gov.sg.baby_bonus_enrollment.domain.Nric
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AuditEventTest {

    @Test
    fun `toLogString formats event and masked nric without extras`() {
        val event = AuditEvent(AuditEventType.ENROLLMENT_SUBMITTED, Nric("T2400001A"))
        event.toLogString() shouldBe "event=ENROLLMENT_SUBMITTED nric=T240****A"
    }

    @Test
    fun `toLogString appends extras as key=value pairs`() {
        val event = AuditEvent(AuditEventType.ELIGIBILITY_FAILED, Nric("T2400001A"), mapOf("reason" to "Child not found"))
        event.toLogString() shouldBe "event=ELIGIBILITY_FAILED nric=T240****A reason=Child not found"
    }

    @Test
    fun `toLogString masks nric in extras when Nric type is used`() {
        val event = AuditEvent(
            AuditEventType.ENROLLMENT_SUBMITTED,
            Nric("T2400001A"),
            mapOf("parentNric" to Nric("S8001234A"))
        )
        event.toLogString() shouldBe "event=ENROLLMENT_SUBMITTED nric=T240****A parentNric=S800****A"
    }
}
