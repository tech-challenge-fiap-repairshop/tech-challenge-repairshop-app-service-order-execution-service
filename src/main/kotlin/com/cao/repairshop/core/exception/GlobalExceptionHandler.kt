package com.cao.repairshop.core.exception

import jakarta.servlet.http.HttpServletRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant

private val logger = KotlinLogging.logger {}

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.warn { "EntityNotFound: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.message, request)
    }

    @ExceptionHandler(DuplicateEntityException::class)
    fun handleDuplicateEntity(ex: DuplicateEntityException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.warn { "DuplicateEntity: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.CONFLICT, "Duplicate entity", ex.message, request)
    }

    @ExceptionHandler(InvalidStateTransitionException::class)
    fun handleInvalidStateTransition(ex: InvalidStateTransitionException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.warn { "InvalidStateTransition: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid state transition", ex.message, request)
    }

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.error { "InsufficientStock: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient stock", ex.message, request)
    }

    @ExceptionHandler(BusinessRuleViolationException::class)
    fun handleBusinessRuleViolation(ex: BusinessRuleViolationException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.warn { "BusinessRuleViolation: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", ex.message, request)
    }

    @ExceptionHandler(InvalidDocumentException::class)
    fun handleInvalidDocument(ex: InvalidDocumentException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.warn { "InvalidDocument: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid document", ex.message, request)
    }

    @ExceptionHandler(InvalidPlateException::class)
    fun handleInvalidPlate(ex: InvalidPlateException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.warn { "InvalidPlate: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid plate", ex.message, request)
    }

    @ExceptionHandler(PasswordEncodingException::class)
    fun handlePasswordEncoding(ex: PasswordEncodingException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.error { "PasswordEncodingException: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Password encoding failed", ex.message, request)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.warn { "IllegalArgument: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad request", ex.message, request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        val detail = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        logger.warn { "ValidationError: $detail [${request.requestURI}]" }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation error", detail, request)
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: org.springframework.http.converter.HttpMessageNotReadableException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.warn { "HttpMessageNotReadable: ${ex.message} [${request.requestURI}]" }
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad request", "Malformed or incomplete request body", request)
    }

    @ExceptionHandler(software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException::class)
    fun handleTransactionCanceled(ex: software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.error(ex) { "DynamoDB Transaction Canceled: ${ex.cancellationReasons()} [${request.requestURI}]" }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Database transaction failed", "A operação atômica no banco de dados falhou: ${ex.message}", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<DefaultError> {
        logger.error(ex) { "UnexpectedException: ${ex.javaClass.simpleName} [${request.requestURI}]" }
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred", request)
    }

    private fun buildResponse(
        status: HttpStatus,
        error: String,
        message: String?,
        request: HttpServletRequest
    ): ResponseEntity<DefaultError> = ResponseEntity.status(status).body(
        DefaultError(
            timestamp = Instant.now(),
            status = status.value(),
            error = error,
            message = message,
            path = request.requestURI
        )
    )
}
