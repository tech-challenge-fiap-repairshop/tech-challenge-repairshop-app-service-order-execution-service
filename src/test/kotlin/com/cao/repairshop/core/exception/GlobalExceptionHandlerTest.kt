package com.cao.repairshop.core.exception

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.core.MethodParameter

class GlobalExceptionHandlerTest {

    private lateinit var globalExceptionHandler: GlobalExceptionHandler
    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setup() {
        globalExceptionHandler = GlobalExceptionHandler()
        request = mock(HttpServletRequest::class.java)
        `when`(request.requestURI).thenReturn("/test-uri")
    }

    @Test
    fun `should handle EntityNotFoundException`() {
        val ex = EntityNotFoundException("Entity not found")
        val response = globalExceptionHandler.handleEntityNotFound(ex, request)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Resource not found", response.body?.error)
        assertEquals("Entity not found", response.body?.message)
        assertEquals("/test-uri", response.body?.path)
        assertNotNull(response.body?.timestamp)
    }

    @Test
    fun `should handle DuplicateEntityException`() {
        val ex = DuplicateEntityException("Duplicate entity")
        val response = globalExceptionHandler.handleDuplicateEntity(ex, request)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("Duplicate entity", response.body?.error)
        assertEquals("Duplicate entity", response.body?.message)
    }

    @Test
    fun `should handle InvalidStateTransitionException`() {
        val ex = InvalidStateTransitionException("Invalid transition")
        val response = globalExceptionHandler.handleInvalidStateTransition(ex, request)

        assertEquals(HttpStatus.valueOf(422), response.statusCode)
        assertEquals("Invalid state transition", response.body?.error)
        assertEquals("Invalid transition", response.body?.message)
    }

    @Test
    fun `should handle InsufficientStockException`() {
        val ex = InsufficientStockException("Insufficient stock")
        val response = globalExceptionHandler.handleInsufficientStock(ex, request)

        assertEquals(HttpStatus.valueOf(422), response.statusCode)
        assertEquals("Insufficient stock", response.body?.error)
        assertEquals("Insufficient stock", response.body?.message)
    }

    @Test
    fun `should handle BusinessRuleViolationException`() {
        val ex = BusinessRuleViolationException("Business rule violated")
        val response = globalExceptionHandler.handleBusinessRuleViolation(ex, request)

        assertEquals(HttpStatus.valueOf(422), response.statusCode)
        assertEquals("Business rule violation", response.body?.error)
        assertEquals("Business rule violated", response.body?.message)
    }

    @Test
    fun `should handle InvalidDocumentException`() {
        val ex = InvalidDocumentException("Invalid document")
        val response = globalExceptionHandler.handleInvalidDocument(ex, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid document", response.body?.error)
        assertEquals("Invalid document", response.body?.message)
    }

    @Test
    fun `should handle InvalidPlateException`() {
        val ex = InvalidPlateException("Invalid plate")
        val response = globalExceptionHandler.handleInvalidPlate(ex, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid plate", response.body?.error)
        assertEquals("Invalid plate", response.body?.message)
    }

    @Test
    fun `should handle PasswordEncodingException`() {
        val ex = PasswordEncodingException("Encoding failed")
        val response = globalExceptionHandler.handlePasswordEncoding(ex, request)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Password encoding failed", response.body?.error)
        assertEquals("Encoding failed", response.body?.message)
    }

    @Test
    fun `should handle IllegalArgumentException`() {
        val ex = IllegalArgumentException("Illegal argument")
        val response = globalExceptionHandler.handleIllegalArgument(ex, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Bad request", response.body?.error)
        assertEquals("Illegal argument", response.body?.message)
    }

    @Test
    fun `should handle MethodArgumentNotValidException`() {
        val bindingResult = mock(BindingResult::class.java)
        val fieldError = FieldError("objectName", "field", "defaultMessage")
        `when`(bindingResult.fieldErrors).thenReturn(listOf(fieldError))
        val methodParameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        val response = globalExceptionHandler.handleValidation(ex, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Validation error", response.body?.error)
        assertEquals("field: defaultMessage", response.body?.message)
    }

    @Test
    fun `should handle HttpMessageNotReadableException`() {
        val inputMessage = mock(org.springframework.http.HttpInputMessage::class.java)
        val ex = HttpMessageNotReadableException("Not readable", inputMessage)
        val response = globalExceptionHandler.handleHttpMessageNotReadable(ex, request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Bad request", response.body?.error)
        assertEquals("Malformed or incomplete request body", response.body?.message)
    }

    @Test
    fun `should handle Exception`() {
        val ex = Exception("Generic error")
        val response = globalExceptionHandler.handleUnexpected(ex, request)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Internal server error", response.body?.error)
        assertEquals("An unexpected error occurred", response.body?.message)
    }
}
