package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.controller.request.EnrollmentRequest
import com.gov.sg.baby_bonus_enrollment.controller.response.DisbursementResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.EnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import com.gov.sg.baby_bonus_enrollment.usecase.EnrollChildUseCase
import com.gov.sg.baby_bonus_enrollment.usecase.dto.CreateEnrollmentDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController(private val enrollChildUseCase: EnrollChildUseCase) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun enroll(@Valid @RequestBody request: EnrollmentRequest): EnrollmentResponse {
        val relationship = try {
            Relationship.valueOf(request.relationship)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid value for relationship")
        }
        val dto = CreateEnrollmentDto(
            childNric = request.childNric,
            parentNric = request.parentNric,
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
}
