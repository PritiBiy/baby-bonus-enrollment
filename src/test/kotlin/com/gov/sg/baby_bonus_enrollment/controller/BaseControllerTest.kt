package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementStatus
import com.gov.sg.baby_bonus_enrollment.domain.disbursement.DisbursementType
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Citizenship
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementClient
import com.gov.sg.baby_bonus_enrollment.external.disbursement.DisbursementResult
import com.gov.sg.baby_bonus_enrollment.external.ica.ChildRecord
import com.gov.sg.baby_bonus_enrollment.external.ica.IcaClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.IroasClient
import com.gov.sg.baby_bonus_enrollment.external.iroas.ParentRecord
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
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// IcaClient, IroasClient, DisbursementClient are mocked at the interface level — they represent
// external HTTP boundaries. Everything internal (use cases, repositories, H2) runs for real.
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseControllerTest {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var objectMapper: ObjectMapper

    @MockitoBean protected lateinit var icaClient: IcaClient
    @MockitoBean protected lateinit var iroasClient: IroasClient
    @MockitoBean protected lateinit var disbursementClient: DisbursementClient

    companion object {
        const val API_KEY = "test-api-key"
        const val DEFAULT_CHILD_NRIC = "T2400001A"
        const val DEFAULT_PARENT_NRIC = "S8001234A"
    }

    protected fun stubChildInIca(childNric: String, citizenship: Citizenship = Citizenship.SINGAPORE_CITIZEN) {
        given(icaClient.findChild(eq(childNric))).willReturn(
            ChildRecord(childNric, "Tan Baby", LocalDate.of(2025, 1, 15), citizenship)
        )
    }

    protected fun stubChildNotInIca(childNric: String = DEFAULT_CHILD_NRIC) {
        given(icaClient.findChild(eq(childNric))).willReturn(null)
    }

    protected fun stubParentInIroas(parentNric: String = DEFAULT_PARENT_NRIC) {
        given(iroasClient.findParent(eq(parentNric))).willReturn(
            ParentRecord(parentNric, "Tan Ah Kow")
        )
    }

    protected fun stubParentNotInIroas(parentNric: String = DEFAULT_PARENT_NRIC) {
        given(iroasClient.findParent(eq(parentNric))).willReturn(null)
    }

    protected fun stubDisbursement(disbursementId: UUID = UUID.randomUUID()): UUID {
        given(disbursementClient.initiate(any(), eq(DisbursementType.CASH_GIFT), eq(BigDecimal("3000.00")))).willReturn(
            DisbursementResult(disbursementId, DisbursementStatus.PROCESSED, Instant.now())
        )
        return disbursementId
    }

    protected fun stubEligibleEnrollment(childNric: String, disbursementId: UUID = UUID.randomUUID()): UUID {
        stubChildInIca(childNric)
        stubParentInIroas()
        return stubDisbursement(disbursementId)
    }

    protected fun enrollmentRequest(
        childNric: String = DEFAULT_CHILD_NRIC,
        parentNric: String = DEFAULT_PARENT_NRIC,
        relationship: String = "FATHER"
    ) = post("/api/v1/enrollments")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-API-Key", API_KEY)
        .content("""{"childNric": "$childNric", "parentNric": "$parentNric", "relationship": "$relationship"}""")
}
