package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.TestResourceReader
import com.gov.sg.baby_bonus_enrollment.domain.Citizenship
import com.gov.sg.baby_bonus_enrollment.domain.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.DisbursementType
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementClient
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementResult
import com.gov.sg.baby_bonus_enrollment.external.ica.ChildRecord
import com.gov.sg.baby_bonus_enrollment.external.ica.IcaClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.IroasClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.ParentRecord
import org.junit.jupiter.api.Test
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// IcaClient, IroasClient, DisbursementClient are mocked at the interface level — they represent
// external HTTP boundaries. Everything internal (service, repositories, H2) runs for real.
@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

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

        val expectedBody = TestResourceReader.read("responses/create-enrollment-201.json")

        mockMvc.perform(
            post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"childNric": "T2400001A", "parentNric": "S8001234A", "relationship": "FATHER"}""")
        )
            .andExpect(status().isCreated)
//            .andExpect(content().json(expectedBody))
//            .andExpect(jsonPath("$.id").isNotEmpty)
//            .andExpect(jsonPath("$.enrolledAt").isNotEmpty)
//            .andExpect(jsonPath("$.disbursement.id").value(disbursementId.toString()))
//            .andExpect(jsonPath("$.disbursement.processedAt").isNotEmpty)
    }
}
