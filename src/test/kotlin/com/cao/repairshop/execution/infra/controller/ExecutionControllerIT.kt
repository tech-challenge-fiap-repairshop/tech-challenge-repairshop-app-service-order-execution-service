package com.cao.repairshop.execution.infra.controller

import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionRequest
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.persistence.models.ServiceOrderEntity
import com.cao.repairshop.serviceorder.infra.persistence.repositories.ServiceOrderRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.config.SpringDataJackson3Configuration
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExecutionControllerIT {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mapper: JsonMapper

    @Autowired
    private lateinit var serviceOrderRepository: ServiceOrderRepository

    private lateinit var serviceOrder: ServiceOrderEntity

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .addModule(SpringDataJackson3Configuration.PageModule(null))
            .build()
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()

        serviceOrder = serviceOrderRepository.save(
            ServiceOrderEntity(
                customerId = UUID.randomUUID(),
                vehicleId = UUID.randomUUID(),
                status = ServiceOrderStatus.IN_EXECUTION
            )
        )
    }

    @Test
    @WithMockUser(roles = ["ATTENDANT"])
    fun `should create, list and update execution status in a real database environment`() {
        val createRequest = CreateExecutionRequest(
            serviceOrderId = serviceOrder.id,
            basicDescription = "OIL_CHANGE",
            fullDescription = "Detailed oil change",
            price = BigDecimal("150.00")
        )

        // 1. Create
        val createResult = mockMvc.perform(
            post("/service-orders/${serviceOrder.id}/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("INITIATED"))
            .andExpect(jsonPath("$.totalPrice").value(150.0))
            .andReturn()

        val executionId = UUID.fromString(mapper.readTree(createResult.response.contentAsString).get("id").asText())

        // 2. Get
        mockMvc.perform(get("/service-orders/${serviceOrder.id}/executions/$executionId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.basicDescription").value("OIL_CHANGE"))

        // 3. Update execution info
        mockMvc.perform(
            put("/service-orders/${serviceOrder.id}/executions/$executionId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullDescription": "Updated details", "price": 180.00}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullDescription").value("Updated details"))
            .andExpect(jsonPath("$.price").value(180.0))

        // 4. Advance Status to PENDING
        mockMvc.perform(
            patch("/service-orders/${serviceOrder.id}/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "PENDING"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))

        // 5. Advance Status to IN_PROGRESS
        mockMvc.perform(
            patch("/service-orders/${serviceOrder.id}/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "IN_PROGRESS"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))

        // 6. Advance Status to FINALIZED
        mockMvc.perform(
            patch("/service-orders/${serviceOrder.id}/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "FINALIZED"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("FINALIZED"))

        // 7. Invalid transition check: FINALIZED cannot go back to PENDING
        mockMvc.perform(
            patch("/service-orders/${serviceOrder.id}/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "PENDING"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("Invalid state transition")))

        // 8. Delete execution
        mockMvc.perform(delete("/service-orders/${serviceOrder.id}/executions/$executionId"))
            .andExpect(status().isNoContent)

        // 9. Verify Not Found
        mockMvc.perform(get("/service-orders/${serviceOrder.id}/executions/$executionId"))
            .andExpect(status().isNotFound)
    }
}
