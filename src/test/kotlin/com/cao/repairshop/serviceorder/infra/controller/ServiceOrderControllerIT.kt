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
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
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
            services = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "Inspeção de Freios",
                    fullDescription = "Brake repair",
                    price = BigDecimal("500.00")
                )
            )
        )

        // 1. Create (initial state is IN_DIAGNOSIS)
        val createResult = mockMvc.perform(
            post("/service-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"))
            .andExpect(jsonPath("$.totalPrice").value(500.0))
            .andReturn()

        val orderId = UUID.fromString(mapper.readTree(createResult.response.contentAsString).get("id").asText())

        // 2. Finish diagnosis to move to WAITING_APPROVAL
        mockMvc.perform(
            post("/service-orders/$orderId/finish-diagnosis")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("WAITING_APPROVAL"))

        // 3. List with pagination
        mockMvc.perform(get("/service-orders?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[?(@.id == '$orderId')]").exists())
    }
}
