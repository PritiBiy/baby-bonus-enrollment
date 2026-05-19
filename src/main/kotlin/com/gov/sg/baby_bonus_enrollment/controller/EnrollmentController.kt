package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.controller.request.EnrollmentRequest
import com.gov.sg.baby_bonus_enrollment.controller.response.DisbursementResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.EnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.domain.Nric
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import com.gov.sg.baby_bonus_enrollment.usecase.EnrollChildUseCase
import com.gov.sg.baby_bonus_enrollment.usecase.GetEnrollmentByIdUseCase
import com.gov.sg.baby_bonus_enrollment.usecase.GetEnrollmentsByChildNricUseCase
import com.gov.sg.baby_bonus_enrollment.usecase.dto.CreateEnrollmentDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController(
    private val enrollChildUseCase: EnrollChildUseCase,
    private val getEnrollmentByIdUseCase: GetEnrollmentByIdUseCase,
    private val getEnrollmentsByChildNricUseCase: GetEnrollmentsByChildNricUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun enroll(@Valid @RequestBody request: EnrollmentRequest): EnrollmentResponse {
        val relationship = try {
            Relationship.valueOf(request.relationship)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid value for relationship")
        }
        val dto = CreateEnrollmentDto(
            childNric = Nric(request.childNric),
            parentNric = Nric(request.parentNric),
            relationship = relationship
        )
        val result = enrollChildUseCase.execute(dto)
        return EnrollmentResponse(
            id = result.id,
            childNric = result.childNric,
            parentNric = result.parentNric,
            relationship = result.relationship,
            status = result.status,
            enrolledAt = result.enrolledAt,
            disbursement = result.disbursement?.let {
                DisbursementResponse(it.id, it.type, it.amount, it.status, it.processedAt)
            }
        )
    }

    @GetMapping
    fun listByChildNric(@RequestParam childNric: String): List<EnrollmentResponse> {
        return getEnrollmentsByChildNricUseCase.execute(childNric).map { result ->
            EnrollmentResponse(
                id = result.id,
                childNric = result.childNric,
                parentNric = result.parentNric,
                relationship = result.relationship,
                status = result.status,
                enrolledAt = result.enrolledAt,
                disbursement = result.disbursement?.let {
                    DisbursementResponse(it.id, it.type, it.amount, it.status, it.processedAt)
                }
            )
        }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): EnrollmentResponse {
        val result = getEnrollmentByIdUseCase.execute(id)
        return EnrollmentResponse(
            id = result.id,
            childNric = result.childNric,
            parentNric = result.parentNric,
            relationship = result.relationship,
            status = result.status,
            enrolledAt = result.enrolledAt,
            disbursement = result.disbursement?.let {
                DisbursementResponse(it.id, it.type, it.amount, it.status, it.processedAt)
            }
        )
    }
}
