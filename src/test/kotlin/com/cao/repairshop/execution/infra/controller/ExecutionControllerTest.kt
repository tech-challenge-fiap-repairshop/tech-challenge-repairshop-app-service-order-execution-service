package com.cao.repairshop.execution.infra.controller

import com.cao.repairshop.core.exception.GlobalExceptionHandler
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.infra.controller.dtos.*
import com.cao.repairshop.execution.infra.controller.ExecutionController
import com.cao.repairshop.execution.application.usecases.*
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionDefinitionRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.data.web.config.SpringDataJackson3Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class ExecutionControllerTest {

    private val addExecution: AddExecution = mockk()
    private val addExecutionBatch: AddExecutionBatch = mockk()
    private val findExecution: FindExecution = mockk()
    private val updateExecution: UpdateExecution = mockk()
    private val removeExecution: RemoveExecution = mockk()
    private val advanceExecutionStatus: AdvanceExecutionStatus = mockk()

    private lateinit var mockMvc: MockMvc
    private lateinit var mapper: JsonMapper

    private val executionId = UUID.randomUUID()
    private val serviceOrderId = UUID.randomUUID()
    private val basePath = "/service-orders/$serviceOrderId/executions"

    private val sampleResponse = ExecutionResponse(
        id = executionId,
        serviceOrderId = serviceOrderId,
        basicDescription = "OIL_CHANGE",
        fullDescription = "Oil change",
        laborPrice = BigDecimal("150.00"),
        totalPrice = BigDecimal("150.00"),
        estimatedTime = null,
        status = ExecutionStatus.INITIATED,
        created = LocalDateTime.now(),
        updated = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .addModule(SpringDataJackson3Configuration.PageModule(null))
            .build()

        mockMvc = MockMvcBuilders
            .standaloneSetup(
                ExecutionController(
                    addExecution,
                    addExecutionBatch,
                    findExecution,
                    updateExecution,
                    removeExecution,
                    advanceExecutionStatus
                )
            )
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
            .setMessageConverters(JacksonJsonHttpMessageConverter(mapper))
            .build()
    }

    @Test
    fun `POST executions should return 201`() {
        val request = CreateExecutionRequest(
            serviceOrderId = serviceOrderId,
            basicDescription = "OIL_CHANGE",
            fullDescription = "Oil change",
            price = BigDecimal("150.00")
        )
        every { addExecution.execute(serviceOrderId, any()) } returns sampleResponse

        mockMvc.perform(
            post(basePath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.basicDescription").value("OIL_CHANGE"))
            .andExpect(jsonPath("$.status").value("INITIATED"))
    }

    @Test
    fun `GET execution by id should return 200`() {
        every { findExecution.findById(serviceOrderId, executionId) } returns sampleResponse

        mockMvc.perform(get("$basePath/$executionId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(executionId.toString()))
    }

    @Test
    fun `PUT execution should return 200`() {
        val updateRequest = UpdateExecutionRequest(
            basicDescription = "SUSPENSION_REPLACEMENT",
            fullDescription = "Full service",
            price = BigDecimal("300.00")
        )
        every { updateExecution.execute(serviceOrderId, executionId, any()) } returns sampleResponse.copy(
            basicDescription = "SUSPENSION_REPLACEMENT",
            fullDescription = "Full service"
        )

        mockMvc.perform(
            put("$basePath/$executionId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.basicDescription").value("SUSPENSION_REPLACEMENT"))
    }

    @Test
    fun `DELETE execution should return 204`() {
        every { removeExecution.execute(serviceOrderId, executionId) } just runs

        mockMvc.perform(delete("$basePath/$executionId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `PATCH execution status should return 200`() {
        val statusRequest = ExecutionStatusUpdateRequest(status = ExecutionStatus.PENDING)
        every { advanceExecutionStatus.execute(serviceOrderId, executionId, ExecutionStatus.PENDING) } returns
            sampleResponse.copy(status = ExecutionStatus.PENDING)

        mockMvc.perform(
            patch("$basePath/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(statusRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `POST executions batch should return 201 with list`() {
        val batchRequest = CreateExecutionBatchRequest(
            serviceOrderId = serviceOrderId,
            executions = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "OIL_CHANGE",
                    price = BigDecimal("150.00")
                ),
                ExecutionDefinitionRequest(
                    basicDescription = "BRAKE_INSPECTION",
                    price = BigDecimal("200.00")
                )
            )
        )
        val batchResponse = listOf(
            sampleResponse,
            sampleResponse.copy(id = UUID.randomUUID(), basicDescription = "BRAKE_INSPECTION")
        )
        every { addExecutionBatch.execute(serviceOrderId, any()) } returns batchResponse

        mockMvc.perform(
            post("$basePath/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(batchRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].basicDescription").value("OIL_CHANGE"))
            .andExpect(jsonPath("$[1].basicDescription").value("BRAKE_INSPECTION"))
    }
}
