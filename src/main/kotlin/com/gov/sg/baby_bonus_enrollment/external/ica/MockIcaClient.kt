package com.gov.sg.baby_bonus_enrollment.external.ica

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class MockIcaClient(private val objectMapper: ObjectMapper) : IcaClient {

    private val children: List<ChildRecord> by lazy {
        objectMapper.readValue<List<ChildRecord>>(ClassPathResource("mock-data/ica_children.json").inputStream)
    }

    override fun findChild(nric: String): ChildRecord? = children.find { it.nric == nric }
}
