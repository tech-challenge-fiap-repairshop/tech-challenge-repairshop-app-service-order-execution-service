package com.cao.repairshop.serviceorder.infra.controller

import com.cao.repairshop.serviceorder.infra.controller.dtos.CreateServiceOrderRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionDefinitionRequest
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
class ServiceOrderControllerIT {

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
    @WithMockUser(roles = ["ATTENDANT"])
    fun `should create and transition service order lifecycle`() {
        val createRequest = CreateServiceOrderRequest(
            customerId = customerId,
            vehicleId = vehicleId,
            customerEmail = "so_it@example.com",
            vehiclePlate = "ABC1234",
            services = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "BRAKE_INSPECTION",
                    fullDescription = "Brake repair",
                    price = BigDecimal("500.00")
                )
            )
        )

        // 1. Create
        val createResult = mockMvc.perform(
            post("/service-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("RECEIVED"))
            .andExpect(jsonPath("$.totalPrice").value(500.0))
            .andReturn()

        val orderId = UUID.fromString(mapper.readTree(createResult.response.contentAsString).get("id").asText())

        // 2. Advance to IN_DIAGNOSIS
        mockMvc.perform(
            patch("/service-orders/$orderId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "IN_DIAGNOSIS"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"))

        // 3. List with pagination
        mockMvc.perform(get("/service-orders?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[?(@.id == '$orderId')]").exists())
    }
}
