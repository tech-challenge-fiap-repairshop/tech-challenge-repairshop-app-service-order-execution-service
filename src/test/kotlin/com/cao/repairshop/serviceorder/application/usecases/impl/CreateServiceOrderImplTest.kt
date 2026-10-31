package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.controller.dtos.CreateServiceOrderRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionDefinitionRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeItemRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class CreateServiceOrderImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var createServiceOrderImpl: CreateServiceOrderImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk()
        createServiceOrderImpl = CreateServiceOrderImpl(serviceOrderGateway)
    }

    @Test
    fun `should create service order successfully with UUIDs in IN_DIAGNOSIS status`() {
        val custId = UUID.randomUUID()
        val vehId = UUID.randomUUID()

        val request = CreateServiceOrderRequest(
            customerId = custId,
            vehicleId = vehId,
            services = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "Troca de Óleo",
                    fullDescription = "Troca com óleo 5W40",
                    price = BigDecimal("100.00"),
                    insumes = listOf(
                        InsumeItemRequest(name = "Óleo Sintético", quantity = 2, unityPrice = BigDecimal.TEN)
                    )
                )
            )
        )

        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = createServiceOrderImpl.execute(request)

        assertEquals(custId, response.customerId)
        assertEquals(vehId, response.vehicleId)
        assertEquals(ServiceOrderStatus.IN_DIAGNOSIS, response.status)
        assertEquals(BigDecimal("120.00"), response.totalPrice)

        verify { serviceOrderGateway.save(any()) }
    }
}
