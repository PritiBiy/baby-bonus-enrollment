package com.gov.sg.baby_bonus_enrollment.controller

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.gov.sg.baby_bonus_enrollment.audit.AuditLogger
import com.gov.sg.baby_bonus_enrollment.controller.response.DisbursementResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.EnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.ErrorResponse
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Citizenship
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.util.UUID

class PostEnrollmentControllerTest : BaseControllerTest() {

    @Test
    fun `returns 201 with masked NRICs and disbursement details for an eligible child`() {
        val disbursementId = UUID.randomUUID()
        stubEligibleEnrollment("T2400001A", disbursementId)

        val result = mockMvc.perform(enrollmentRequest("T2400001A"))
            .andExpect(status().isCreated)
            .andReturn()

        val response = objectMapper.readValue<EnrollmentResponse>(result.response.contentAsString)
        response shouldBe EnrollmentResponse(
            id = response.id,
            childNric = "T240****A",
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

    @Nested
    inner class EligibilityFailures {

        @Test
        fun `returns 422 when child is not found in ICA`() {
            stubChildNotInIca()

            val result = mockMvc.perform(enrollmentRequest())
                .andExpect(status().isUnprocessableEntity)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Child not found in ICA records")
        }

        @Test
        fun `returns 422 when child is not a Singapore Citizen`() {
            stubChildInIca(DEFAULT_CHILD_NRIC, Citizenship.PERMANENT_RESIDENT)

            val result = mockMvc.perform(enrollmentRequest())
                .andExpect(status().isUnprocessableEntity)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Child is not a Singapore Citizen")
        }

        @Test
        fun `returns 422 when parent is not found in IROAS`() {
            stubChildInIca(DEFAULT_CHILD_NRIC)
            stubParentNotInIroas()

            val result = mockMvc.perform(enrollmentRequest())
                .andExpect(status().isUnprocessableEntity)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Parent not found in IROAS records")
        }
    }

    @Nested
    inner class DuplicateEnrollment {

        @Test
        fun `returns 409 when child already has an active enrollment`() {
            stubEligibleEnrollment("T2400003C")

            mockMvc.perform(enrollmentRequest("T2400003C")).andExpect(status().isCreated)

            val result = mockMvc.perform(enrollmentRequest("T2400003C"))
                .andExpect(status().isConflict)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Child already has an active enrollment")
        }
    }

    @Nested
    inner class BadRequest {

        @Test
        fun `returns 400 for invalid relationship value`() {
            val result = mockMvc.perform(enrollmentRequest(relationship = "INVALID"))
                .andExpect(status().isBadRequest)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Invalid value for relationship")
        }

        @Test
        fun `returns 400 when childNric is blank`() {
            val result = mockMvc.perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", API_KEY)
                    .content("""{"childNric": "", "parentNric": "S8001234A", "relationship": "FATHER"}""")
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("childNric must not be blank")
        }

        @Test
        fun `returns 400 when parentNric is blank`() {
            val result = mockMvc.perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", API_KEY)
                    .content("""{"childNric": "T2400001A", "parentNric": "", "relationship": "FATHER"}""")
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("parentNric must not be blank")
        }

        @Test
        fun `returns 400 when relationship is blank`() {
            val result = mockMvc.perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", API_KEY)
                    .content("""{"childNric": "T2400001A", "parentNric": "S8001234A", "relationship": ""}""")
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("relationship must not be blank")
        }

        @Test
        fun `returns 400 when relationship is missing`() {
            val result = mockMvc.perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", API_KEY)
                    .content("""{"childNric": "T2400001A", "parentNric": "S8001234A"}""")
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Bad request")
        }
    }

    @Nested
    inner class AuditLogging {

        private lateinit var logCapture: ListAppender<ILoggingEvent>

        @BeforeEach
        fun attachLogCapture() {
            logCapture = ListAppender<ILoggingEvent>().also {
                (LoggerFactory.getLogger(AuditLogger::class.java) as ch.qos.logback.classic.Logger)
                    .addAppender(it)
                it.start()
            }
        }

        @AfterEach
        fun detachLogCapture() {
            (LoggerFactory.getLogger(AuditLogger::class.java) as ch.qos.logback.classic.Logger)
                .detachAppender(logCapture)
        }

        @Test
        fun `logs ENROLLMENT_SUBMITTED, ELIGIBILITY_PASSED and DISBURSEMENT_INITIATED with masked NRICs on success`() {
            stubEligibleEnrollment("T2400002B")

            mockMvc.perform(enrollmentRequest("T2400002B")).andExpect(status().isCreated)

            val messages = logCapture.list.map { it.formattedMessage }
            messages.any { "ENROLLMENT_SUBMITTED" in it && "T240****B" in it && "S800****A" in it } shouldBe true
            messages.any { "ELIGIBILITY_PASSED" in it } shouldBe true
            messages.any { "DISBURSEMENT_INITIATED" in it } shouldBe true
            messages.none { "T2400002B" in it } shouldBe true
            messages.none { "S8001234A" in it } shouldBe true
        }

        @Test
        fun `logs ELIGIBILITY_FAILED with reason and masked NRIC when child is not found`() {
            stubChildNotInIca()

            mockMvc.perform(enrollmentRequest()).andExpect(status().isUnprocessableEntity)

            val messages = logCapture.list.map { it.formattedMessage }
            messages.any { "ELIGIBILITY_FAILED" in it && "Child not found in ICA records" in it } shouldBe true
            messages.none { "T2400001A" in it } shouldBe true
        }
    }

    @Nested
    inner class Unauthorized {

        @Test
        fun `returns 401 when X-API-Key header is missing`() {
            val result = mockMvc.perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"childNric": "T2400001A", "parentNric": "S8001234A", "relationship": "FATHER"}""")
            )
                .andExpect(status().isUnauthorized)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Unauthorised")
        }

        @Test
        fun `returns 401 when X-API-Key header is invalid`() {
            val result = mockMvc.perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", "wrong-key")
                    .content("""{"childNric": "T2400001A", "parentNric": "S8001234A", "relationship": "FATHER"}""")
            )
                .andExpect(status().isUnauthorized)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Unauthorised")
        }
    }
}
