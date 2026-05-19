package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.controller.response.DisbursementResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.EnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.ErrorResponse
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.util.UUID

class GetEnrollmentsByChildNricControllerTest : BaseControllerTest() {

    @Test
    fun `returns 200 with list of enrollments for a known childNric`() {
        val disbursementId = UUID.randomUUID()
        stubEligibleEnrollment("T2400007G", disbursementId)

        val postResult = mockMvc.perform(enrollmentRequest("T2400007G"))
            .andExpect(status().isCreated).andReturn()
        val created = objectMapper.readValue<EnrollmentResponse>(postResult.response.contentAsString)

        val result = mockMvc.perform(
            get("/api/v1/enrollments")
                .param("childNric", "T2400007G")
                .header("X-API-Key", API_KEY)
        ).andExpect(status().isOk).andReturn()

        val response = objectMapper.readValue<List<EnrollmentResponse>>(result.response.contentAsString)
        response shouldHaveSize 1
        response[0] shouldBe EnrollmentResponse(
            id = created.id,
            childNric = "T240****G",
            parentNric = "S800****A",
            relationship = Relationship.FATHER,
            status = EnrollmentStatus.ENROLLED,
            enrolledAt = response[0].enrolledAt,
            disbursement = DisbursementResponse(
                id = disbursementId,
                type = DisbursementType.CASH_GIFT,
                amount = BigDecimal("3000.00"),
                status = DisbursementStatus.PROCESSED,
                processedAt = response[0].disbursement!!.processedAt
            )
        )
    }

    @Test
    fun `returns empty list for unknown childNric`() {
        val result = mockMvc.perform(
            get("/api/v1/enrollments")
                .param("childNric", "Z9999999Z")
                .header("X-API-Key", API_KEY)
        ).andExpect(status().isOk).andReturn()

        objectMapper.readValue<List<EnrollmentResponse>>(result.response.contentAsString) shouldBe emptyList()
    }

    @Test
    fun `returns 400 when childNric param is missing`() {
        val result = mockMvc.perform(
            get("/api/v1/enrollments").header("X-API-Key", API_KEY)
        ).andExpect(status().isBadRequest).andReturn()

        objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
            ErrorResponse("childNric query parameter is required")
    }
}
