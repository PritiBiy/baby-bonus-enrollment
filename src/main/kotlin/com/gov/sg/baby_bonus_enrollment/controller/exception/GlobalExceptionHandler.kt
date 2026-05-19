package com.gov.sg.baby_bonus_enrollment.controller.exception

import com.gov.sg.baby_bonus_enrollment.audit.AuditLogger
import com.gov.sg.baby_bonus_enrollment.controller.response.ErrorResponse
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EnrollmentAlreadyIneligibleException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.DuplicateEnrollmentException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.EligibilityException
import com.gov.sg.baby_bonus_enrollment.usecase.exception.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler(private val auditLogger: AuditLogger) {

    @ExceptionHandler(EligibilityException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleEligibility(e: EligibilityException): ErrorResponse =
        ErrorResponse(e.message!!)

    @ExceptionHandler(EnrollmentAlreadyIneligibleException::class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    fun handleAlreadyIneligible(e: EnrollmentAlreadyIneligibleException): ErrorResponse =
        ErrorResponse(e.message!!)

    @ExceptionHandler(DuplicateEnrollmentException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDuplicate(e: DuplicateEnrollmentException): ErrorResponse =
        ErrorResponse(e.message!!)

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: NotFoundException): ErrorResponse = ErrorResponse(e.message!!)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(e: MethodArgumentNotValidException): ErrorResponse {
        val message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Bad request"
        return ErrorResponse(message)
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMissingParam(e: MissingServletRequestParameterException): ErrorResponse =
        ErrorResponse("${e.parameterName} query parameter is required")

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(e: IllegalArgumentException): ErrorResponse =
        ErrorResponse(e.message ?: "Bad request")

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpected(e: Exception): ErrorResponse {
        auditLogger.error("UNEXPECTED_ERROR type=${e.javaClass.simpleName} message=${e.message}")
        return ErrorResponse("An unexpected error occurred")
    }
}
