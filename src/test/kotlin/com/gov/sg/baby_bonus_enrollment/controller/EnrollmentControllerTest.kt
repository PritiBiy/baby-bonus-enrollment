package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.controller.response.DisbursementResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.EnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.ErrorResponse
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Citizenship
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.EnrollmentStatus
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementClient
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementResult
import com.gov.sg.baby_bonus_enrollment.external.ica.ChildRecord
import com.gov.sg.baby_bonus_enrollment.external.ica.IcaClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.IroasClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.ParentRecord
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.gov.sg.baby_bonus_enrollment.audit.AuditLogger
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// IcaClient, IroasClient, DisbursementClient are mocked at the interface level — they represent
// external HTTP boundaries. Everything internal (use cases, repositories, H2) runs for real.
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockitoBean private lateinit var icaClient: IcaClient
    @MockitoBean private lateinit var iroasClient: IroasClient
    @MockitoBean private lateinit var disbursementClient: DisbursementClient

    @Test
    fun `POST enrollments returns 201 with masked NRICs and disbursement details for an eligible child`() {
        val disbursementId = UUID.randomUUID()

        given(icaClient.findChild(eq("T2400001A"))).willReturn(
            ChildRecord("T2400001A", "Tan Baby", LocalDate.of(2025, 1, 15), Citizenship.SINGAPORE_CITIZEN)
        )
        given(iroasClient.findParent(eq("S8001234A"))).willReturn(
            ParentRecord("S8001234A", "Tan Ah Kow")
        )
        given(disbursementClient.initiate(any(), eq(DisbursementType.CASH_GIFT), eq(BigDecimal("3000.00")))).willReturn(
            DisbursementResult(disbursementId, DisbursementStatus.PROCESSED, Instant.now())
        )

        val result = mockMvc.perform(
            post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", API_KEY)
                .content("""{"childNric": "T2400001A", "parentNric": "S8001234A", "relationship": "FATHER"}""")
        )
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
            given(icaClient.findChild(eq("T2400001A"))).willReturn(null)

            val result = mockMvc.perform(enrollmentRequest())
                .andExpect(status().isUnprocessableEntity)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Child not found in ICA records")
        }

        @Test
        fun `returns 422 when child is not a Singapore Citizen`() {
            given(icaClient.findChild(eq("T2400001A"))).willReturn(
                ChildRecord("T2400001A", "Tan Baby", LocalDate.of(2025, 1, 15), Citizenship.PERMANENT_RESIDENT)
            )

            val result = mockMvc.perform(enrollmentRequest())
                .andExpect(status().isUnprocessableEntity)
                .andReturn()

            objectMapper.readValue<ErrorResponse>(result.response.contentAsString) shouldBe
                ErrorResponse("Child is not a Singapore Citizen")
        }

        @Test
        fun `returns 422 when parent is not found in IROAS`() {
            given(icaClient.findChild(eq("T2400001A"))).willReturn(
                ChildRecord("T2400001A", "Tan Baby", LocalDate.of(2025, 1, 15), Citizenship.SINGAPORE_CITIZEN)
            )
            given(iroasClient.findParent(eq("S8001234A"))).willReturn(null)

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
            given(icaClient.findChild(eq("T2400003C"))).willReturn(
                ChildRecord("T2400003C", "Tan Baby Two", LocalDate.of(2025, 3, 1), Citizenship.SINGAPORE_CITIZEN)
            )
            given(iroasClient.findParent(eq("S8001234A"))).willReturn(
                ParentRecord("S8001234A", "Tan Ah Kow")
            )
            given(disbursementClient.initiate(any(), eq(DisbursementType.CASH_GIFT), eq(BigDecimal("3000.00")))).willReturn(
                DisbursementResult(UUID.randomUUID(), DisbursementStatus.PROCESSED, Instant.now())
            )

            val request = """{"childNric": "T2400003C", "parentNric": "S8001234A", "relationship": "FATHER"}"""

            mockMvc.perform(post("/api/v1/enrollments").contentType(MediaType.APPLICATION_JSON).header("X-API-Key", API_KEY).content(request))
                .andExpect(status().isCreated)

            val result = mockMvc.perform(post("/api/v1/enrollments").contentType(MediaType.APPLICATION_JSON).header("X-API-Key", API_KEY).content(request))
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
            val result = mockMvc.perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", API_KEY)
                    .content("""{"childNric": "T2400001A", "parentNric": "S8001234A", "relationship": "INVALID"}""")
            )
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
            given(icaClient.findChild(eq("T2400002B"))).willReturn(
                ChildRecord("T2400002B", "Tan Baby Log", LocalDate.of(2025, 2, 1), Citizenship.SINGAPORE_CITIZEN)
            )
            given(iroasClient.findParent(eq("S8001234A"))).willReturn(
                ParentRecord("S8001234A", "Tan Ah Kow")
            )
            given(disbursementClient.initiate(any(), eq(DisbursementType.CASH_GIFT), eq(BigDecimal("3000.00")))).willReturn(
                DisbursementResult(UUID.randomUUID(), DisbursementStatus.PROCESSED, Instant.now())
            )

            mockMvc.perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", API_KEY)
                    .content("""{"childNric": "T2400002B", "parentNric": "S8001234A", "relationship": "FATHER"}""")
            ).andExpect(status().isCreated)

            val messages = logCapture.list.map { it.formattedMessage }
            messages.any { "ENROLLMENT_SUBMITTED" in it && "T240****B" in it && "S800****A" in it } shouldBe true
            messages.any { "ELIGIBILITY_PASSED" in it } shouldBe true
            messages.any { "DISBURSEMENT_INITIATED" in it } shouldBe true
            messages.none { "T2400002B" in it } shouldBe true
            messages.none { "S8001234A" in it } shouldBe true
        }

        @Test
        fun `logs ELIGIBILITY_FAILED with reason and masked NRIC when child is not found`() {
            given(icaClient.findChild(eq("T2400001A"))).willReturn(null)

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

    private fun enrollmentRequest() = post("/api/v1/enrollments")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-API-Key", API_KEY)
        .content("""{"childNric": "T2400001A", "parentNric": "S8001234A", "relationship": "FATHER"}""")

    companion object {
        private const val API_KEY = "test-api-key"
    }
}
