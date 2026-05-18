package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.domain.Citizenship
import com.gov.sg.baby_bonus_enrollment.domain.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.DisbursementType
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementClient
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementResult
import com.gov.sg.baby_bonus_enrollment.external.ica.ChildRecord
import com.gov.sg.baby_bonus_enrollment.external.ica.IcaClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.IroasClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.ParentRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// IcaClient and IroasClient are mocked at the interface level — not at HTTP level.
// The external boundary for this service is the IcaClient interface, not a URL.
// MockIcaClient owns JSON parsing; it is tested separately. The integration test
// provides typed records directly to keep the focus on the enrollment flow.
// If IcaClient were a real HTTP client, WireMock would stub the endpoint instead.
@SpringBootTest
@AutoConfigureMockMvc
@Disabled("enable once repository and service layers are tested")
class EnrollmentControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    private lateinit var webTestClient: WebTestClient

    @MockitoBean private lateinit var icaClient: IcaClient
    @MockitoBean private lateinit var iroasClient: IroasClient
    @MockitoBean private lateinit var disbursementClient: DisbursementClient

    @BeforeEach
    fun setUp() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc).build()
    }

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

        webTestClient.post()
            .uri("/api/v1/enrollments")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf(
                "childNric" to "T2400001A",
                "parentNric" to "S8001234A",
                "relationship" to "FATHER"
            ))
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isNotEmpty
            .jsonPath("$.childNric").isEqualTo("T240****A")
            .jsonPath("$.parentNric").isEqualTo("S800****A")
            .jsonPath("$.relationship").isEqualTo("FATHER")
            .jsonPath("$.status").isEqualTo("ENROLLED")
            .jsonPath("$.enrolledAt").isNotEmpty
            .jsonPath("$.disbursement.id").isEqualTo(disbursementId.toString())
            .jsonPath("$.disbursement.type").isEqualTo("CASH_GIFT")
            .jsonPath("$.disbursement.amount").isEqualTo(3000.00)
            .jsonPath("$.disbursement.status").isEqualTo("PROCESSED")
            .jsonPath("$.disbursement.processedAt").isNotEmpty
    }
}
