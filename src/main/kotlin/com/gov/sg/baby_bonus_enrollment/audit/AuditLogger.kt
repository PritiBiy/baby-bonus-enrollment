package com.gov.sg.baby_bonus_enrollment.audit

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AuditLogger {
    private val log = LoggerFactory.getLogger(AuditLogger::class.java)
    fun info(event: AuditEvent) = log.info(event.toLogString())
    fun warn(event: AuditEvent) = log.warn(event.toLogString())
    fun error(event: AuditEvent) = log.error(event.toLogString())
    fun error(message: String) = log.error(message)
}
