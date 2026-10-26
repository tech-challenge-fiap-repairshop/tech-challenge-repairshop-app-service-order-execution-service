package com.cao.repairshop.execution.infra.controller

import com.cao.repairshop.execution.application.usecases.AddExecution
import com.cao.repairshop.execution.application.usecases.AdvanceExecutionStatus
import com.cao.repairshop.execution.domain.BasicExecution
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionRequest
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class TaskExecutionControllerTest {

    private val addExecution: AddExecution = mockk()
    private val advanceExecutionStatus: AdvanceExecutionStatus = mockk()
    private lateinit var mockMvc: MockMvc
    private val mapper: JsonMapper = JsonMapper.builder().build()

    private val executionId = UUID.randomUUID()
    private val serviceOrderId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val controller = TaskExecutionController(addExecution, advanceExecutionStatus)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `POST execute_task should advance execution status and return 200`() {
        val response = ExecutionResponse(
            id = executionId,
            serviceOrderId = serviceOrderId,
            basicDescription = BasicExecution.OIL_CHANGE,
            fullDescription = "Oil change",
            laborPrice = BigDecimal("150.00"),
            totalPrice = BigDecimal("150.00"),
            estimatedTime = null,
            status = ExecutionStatus.FINALIZED,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
        )

        every { advanceExecutionStatus.execute(serviceOrderId, executionId, ExecutionStatus.FINALIZED) } returns response

        mockMvc.perform(
            post("/execute_task/$executionId")
                .param("serviceOrderId", serviceOrderId.toString())
                .param("status", "FINALIZED")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("FINALIZED"))
    }

    @Test
    fun `POST increment_execution_task should add execution to order and return 201`() {
        val response = ExecutionResponse(
            id = executionId,
            serviceOrderId = serviceOrderId,
            basicDescription = BasicExecution.BRAKE_INSPECTION,
            fullDescription = "Inspect brake pads",
            laborPrice = BigDecimal("80.00"),
            totalPrice = BigDecimal("80.00"),
            estimatedTime = BigDecimal("1.0"),
            status = ExecutionStatus.INITIATED,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
        )

        val request = CreateExecutionRequest(
            serviceOrderId = serviceOrderId,
            basicDescription = "BRAKE_INSPECTION",
            fullDescription = "Inspect brake pads",
            price = BigDecimal("80.00"),
            estimatedTime = BigDecimal("1.0")
        )

        every { addExecution.execute(serviceOrderId, any()) } returns response

        mockMvc.perform(
            post("/increment_execution_task/$serviceOrderId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(executionId.toString()))
            .andExpect(jsonPath("$.basicDescription").value("BRAKE_INSPECTION"))
    }
}
