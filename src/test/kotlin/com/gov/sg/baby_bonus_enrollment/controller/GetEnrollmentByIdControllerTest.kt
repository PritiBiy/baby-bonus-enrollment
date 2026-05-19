package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.controller.response.DisbursementResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.EnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.ErrorResponse
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.util.UUID

class GetEnrollmentByIdControllerTest : BaseControllerTest() {

    @Test
    fun `returns 200 with masked NRICs and disbursement for a known enrollment ID`() {
        val disbursementId = UUID.randomUUID()
        stubEligibleEnrollment("T2400005E", disbursementId)

        val postResult = mockMvc.perform(enrollmentRequest("T2400005E"))
            .andExpect(status().isCreated)
            .andReturn()
        val created = objectMapper.readValue<EnrollmentResponse>(postResult.response.contentAsString)

        val result = mockMvc.perform(
            get("/api/v1/enrollments/${created.id}").header("X-API-Key", API_KEY)
        ).andExpect(status().isOk).andReturn()

        val response = objectMapper.readValue<EnrollmentResponse>(result.response.contentAsString)
        response shouldBe EnrollmentResponse(
            id = created.id,
            childNric = "T240****E",
            parentNric = "S800****A",
            relationship = Relationship.FATHER,
            status = EnrollmentStatus.ENROLLED,
            enrolledAt = response.enrolledAt,
            disbursement = DisbursementResponse(
                id = disbursementId,
                type = DisbursementType.CASH_GIFT,
                amount = BigDecimal("3000.00"),
                status = DisbursementStatus.PROCESSED,
                processedAt = response.disbursement!!.processedAt
            )
        )
    }

    @Test
    fun `returns 404 for an unknown enrollment ID`() {
        val result = mockMvc.perform(
            get("/api/v1/enrollments/${UUID.randomUUID()}").header("X-API-Key", API_KEY)
        ).andExpect(status().isNotFound).andReturn()

        objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
            ErrorResponse("Enrollment not found")
    }
}
