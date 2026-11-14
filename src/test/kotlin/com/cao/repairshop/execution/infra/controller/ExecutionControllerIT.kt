package com.cao.repairshop.execution.infra.controller

import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionRequest
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.controller.dtos.CreateServiceOrderRequest
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
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class ExecutionControllerIT {

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mapper: JsonMapper

    private val customerId = UUID.randomUUID()
    private val vehicleId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .addModule(SpringDataJackson3Configuration.PageModule(null))
            .build()
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    @Test
    @WithMockUser(roles = ["ATTENDANT", "MECHANIC", "ADMIN"])
    fun `should create, list and update execution status in an end-to-end environment`() {
        // 1. Create a service order
        val createOrderRequest = CreateServiceOrderRequest(
            customerId = customerId,
            vehicleId = vehicleId,
            services = emptyList()
        )

        val orderResult = mockMvc.perform(
            post("/service-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(createOrderRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val serviceOrderId = UUID.fromString(mapper.readTree(orderResult.response.contentAsString).get("id").asText())

        val createRequest = CreateExecutionRequest(
            serviceOrderId = serviceOrderId,
            basicDescription = "OIL_CHANGE",
            fullDescription = "Detailed oil change",
            price = BigDecimal("150.00")
        )

        // 2. Create Execution
        val createResult = mockMvc.perform(
            post("/service-orders/$serviceOrderId/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("INITIATED"))
            .andExpect(jsonPath("$.totalPrice").value(150.0))
            .andReturn()

        val executionId = UUID.fromString(mapper.readTree(createResult.response.contentAsString).get("id").asText())

        // 3. Get Execution
        mockMvc.perform(get("/service-orders/$serviceOrderId/executions/$executionId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.basicDescription").value("OIL_CHANGE"))

        // 4. Update execution info
        mockMvc.perform(
            put("/service-orders/$serviceOrderId/executions/$executionId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullDescription": "Updated details", "price": 180.00}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullDescription").value("Updated details"))
            .andExpect(jsonPath("$.price").value(180.0))

        // 5. Advance Status to PENDING
        mockMvc.perform(
            patch("/service-orders/$serviceOrderId/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "PENDING"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))

        // 6. Advance Status to IN_PROGRESS
        mockMvc.perform(
            patch("/service-orders/$serviceOrderId/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "IN_PROGRESS"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))

        // 7. Advance Status to FINALIZED
        mockMvc.perform(
            patch("/service-orders/$serviceOrderId/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "FINALIZED"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("FINALIZED"))

        // 8. Invalid transition check: FINALIZED cannot go back to PENDING
        mockMvc.perform(
            patch("/service-orders/$serviceOrderId/executions/$executionId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "PENDING"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message", containsString("Invalid state transition")))

        // 9. Delete execution
        mockMvc.perform(delete("/service-orders/$serviceOrderId/executions/$executionId"))
            .andExpect(status().isNoContent)

        // 10. Verify Not Found
        mockMvc.perform(get("/service-orders/$serviceOrderId/executions/$executionId"))
            .andExpect(status().isNotFound)
    }
}
