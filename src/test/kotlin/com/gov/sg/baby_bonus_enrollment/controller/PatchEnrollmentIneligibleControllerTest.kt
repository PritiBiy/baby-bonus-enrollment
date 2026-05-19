package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.controller.response.EnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.ErrorResponse
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.module.kotlin.readValue
import java.util.UUID

class PatchEnrollmentIneligibleControllerTest : BaseControllerTest() {

    @Test
    fun `returns 200 with status INELIGIBLE after override`() {
        stubEligibleEnrollment("T2400008H")
        val postResult = mockMvc.perform(enrollmentRequest("T2400008H"))
            .andExpect(status().isCreated).andReturn()
        val created = objectMapper.readValue<EnrollmentResponse>(postResult.response.contentAsString)

        val result = mockMvc.perform(
            patch("/api/v1/enrollments/${created.id}/ineligible")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", API_KEY)
                .content("""{"reason": "Child citizenship data incorrect"}""")
        ).andExpect(status().isOk).andReturn()

        val response = objectMapper.readValue<EnrollmentResponse>(result.response.contentAsString)
        response.status shouldBe EnrollmentStatus.INELIGIBLE
        response.id shouldBe created.id
        response.childNric shouldBe "T240****H"
    }

    @Test
    fun `returns 422 when enrollment is already INELIGIBLE`() {
        stubEligibleEnrollment("T2400009I")
        val postResult = mockMvc.perform(enrollmentRequest("T2400009I"))
            .andExpect(status().isCreated).andReturn()
        val created = objectMapper.readValue<EnrollmentResponse>(postResult.response.contentAsString)

        val patchRequest = patch("/api/v1/enrollments/${created.id}/ineligible")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-API-Key", API_KEY)
            .content("""{"reason": "First override"}""")

        mockMvc.perform(patchRequest).andExpect(status().isOk)

        val result = mockMvc.perform(
            patch("/api/v1/enrollments/${created.id}/ineligible")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", API_KEY)
                .content("""{"reason": "Second override"}""")
        ).andExpect(status().isUnprocessableEntity).andReturn()

        objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
            ErrorResponse("Enrollment is already ineligible")
    }

    @Test
    fun `returns 404 for unknown enrollment ID`() {
        val result = mockMvc.perform(
            patch("/api/v1/enrollments/${UUID.randomUUID()}/ineligible")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", API_KEY)
                .content("""{"reason": "Some reason"}""")
        ).andExpect(status().isNotFound).andReturn()

        objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
            ErrorResponse("Enrollment not found")
    }

    @Test
    fun `returns 400 when reason is blank`() {
        val result = mockMvc.perform(
            patch("/api/v1/enrollments/${UUID.randomUUID()}/ineligible")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", API_KEY)
                .content("""{"reason": ""}""")
        ).andExpect(status().isBadRequest).andReturn()

        objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
            ErrorResponse("reason must not be blank")
    }
}
