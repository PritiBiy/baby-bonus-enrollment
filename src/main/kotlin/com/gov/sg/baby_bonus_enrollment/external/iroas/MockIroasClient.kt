package com.gov.sg.baby_bonus_enrollment.external.iroas

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class MockIroasClient(private val objectMapper: ObjectMapper) : IroasClient {

    private val parents: List<ParentRecord> by lazy {
        objectMapper.readValue<List<ParentRecord>>(ClassPathResource("mock-data/iroas_parents.json").inputStream)
    }

    override fun findParent(nric: String): ParentRecord? = parents.find { it.nric == nric }
}
