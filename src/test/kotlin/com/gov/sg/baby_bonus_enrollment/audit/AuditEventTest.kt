package com.gov.sg.baby_bonus_enrollment.audit

import com.gov.sg.baby_bonus_enrollment.domain.Nric
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AuditEventTest {

    @Test
    fun `toLogString formats event, masked nric and outcome without extras`() {
        val event = AuditEvent(AuditEventType.ENROLLMENT_SUBMITTED, Nric("T2400001A"), AuditOutcome.SUCCESS)
        event.toLogString() shouldBe "event=ENROLLMENT_SUBMITTED nric=T240****A outcome=SUCCESS"
    }

    @Test
    fun `toLogString appends extras as key=value pairs`() {
        val event = AuditEvent(AuditEventType.ELIGIBILITY_FAILED, Nric("T2400001A"), AuditOutcome.FAILURE, mapOf("reason" to "Child not found"))
        event.toLogString() shouldBe "event=ELIGIBILITY_FAILED nric=T240****A outcome=FAILURE reason=Child not found"
    }

    @Test
    fun `toLogString masks nric in extras when Nric type is used`() {
        val event = AuditEvent(
            AuditEventType.ENROLLMENT_SUBMITTED,
            Nric("T2400001A"),
            AuditOutcome.SUCCESS,
            mapOf("parentNric" to Nric("S8001234A"))
        )
        event.toLogString() shouldBe "event=ENROLLMENT_SUBMITTED nric=T240****A outcome=SUCCESS parentNric=S800****A"
    }
}
