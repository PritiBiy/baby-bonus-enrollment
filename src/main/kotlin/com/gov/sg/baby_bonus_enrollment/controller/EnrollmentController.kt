package com.gov.sg.baby_bonus_enrollment.controller

import com.gov.sg.baby_bonus_enrollment.controller.request.EnrollmentRequest
import com.gov.sg.baby_bonus_enrollment.controller.response.DisbursementResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.EnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.controller.response.MarkIneligibleEnrollmentResponse
import com.gov.sg.baby_bonus_enrollment.domain.Nric
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Relationship
import com.gov.sg.baby_bonus_enrollment.controller.request.MarkIneligibleEnrollmentStatusRequest
import com.gov.sg.baby_bonus_enrollment.domain.enrollment.Enrollment
import com.gov.sg.baby_bonus_enrollment.usecase.EnrollChildUseCase
import com.gov.sg.baby_bonus_enrollment.usecase.GetEnrollmentByIdUseCase
import com.gov.sg.baby_bonus_enrollment.usecase.GetEnrollmentsByChildNricUseCase
import com.gov.sg.baby_bonus_enrollment.usecase.MarkEnrollmentIneligibleUseCase
import com.gov.sg.baby_bonus_enrollment.usecase.dto.CreateEnrollmentDto
import com.gov.sg.baby_bonus_enrollment.usecase.dto.EnrollmentDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Enrollments")
@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController(
    private val enrollChildUseCase: EnrollChildUseCase,
    private val getEnrollmentByIdUseCase: GetEnrollmentByIdUseCase,
    private val getEnrollmentsByChildNricUseCase: GetEnrollmentsByChildNricUseCase,
    private val markEnrollmentIneligibleUseCase: MarkEnrollmentIneligibleUseCase
) {

    @Operation(summary = "Submit an enrollment application")
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
        return enrollChildUseCase.execute(dto).toResponse()
    }

    @Operation(summary = "List all enrollments for a child")
    @GetMapping
    fun listByChildNric(@RequestParam childNric: String): List<EnrollmentResponse> =
        getEnrollmentsByChildNricUseCase.execute(childNric).map { it.toResponse() }

    @Operation(summary = "Retrieve an enrollment by enrollment ID")
    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): EnrollmentResponse =
        getEnrollmentByIdUseCase.execute(id).toResponse()

    @Operation(summary = "Override enrollment status to INELIGIBLE by enrollment ID")
    @PatchMapping("/{id}/ineligible")
    fun markIneligible(
        @PathVariable id: UUID,
        @Valid @RequestBody request: MarkIneligibleEnrollmentStatusRequest
    ): MarkIneligibleEnrollmentResponse {
        val enrollment = markEnrollmentIneligibleUseCase.execute(id, request.reason)
        return enrollment.toMarkIneligibleResponse()
    }

    private fun Enrollment.toMarkIneligibleResponse() = MarkIneligibleEnrollmentResponse(
        id = id,
        childNric = Nric(childNric).masked(),
        parentNric = Nric(parentNric).masked(),
        status = status,
        reason = requireNotNull(reason)
    )

    private fun EnrollmentDto.toResponse() = EnrollmentResponse(
        id = id,
        childNric = childNric,
        parentNric = parentNric,
        relationship = relationship,
        status = status,
        enrolledAt = enrolledAt,
        disbursement = disbursement?.let {
            DisbursementResponse(it.id, it.type, it.amount, it.status, it.processedAt)
        }
    )
}
