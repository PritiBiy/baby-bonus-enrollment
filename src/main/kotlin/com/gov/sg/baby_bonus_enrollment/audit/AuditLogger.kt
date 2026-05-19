package com.gov.sg.baby_bonus_enrollment.audit

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AuditLogger {
    private val log = LoggerFactory.getLogger(AuditLogger::class.java)
    fun log(message: String) = log.info(message)
}
