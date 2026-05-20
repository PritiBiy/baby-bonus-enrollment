package com.gov.sg.baby_bonus_enrollment.audit

import com.gov.sg.baby_bonus_enrollment.domain.Nric

data class AuditEvent(
    val event: AuditEventType,
    val nric: Nric,
    val extras: Map<String, Any> = emptyMap()
) {
    fun toLogString(): String {
        val base = "event=$event nric=$nric"
        return if (extras.isEmpty()) base
        else "$base ${extras.entries.joinToString(" ") { "${it.key}=${it.value}" }}"
    }
}
