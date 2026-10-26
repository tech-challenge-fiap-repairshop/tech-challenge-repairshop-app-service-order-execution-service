package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.inventory.application.gateways.InsumeGateway
import com.cao.repairshop.inventory.domain.entities.Insume
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.NotifyCustomer
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
    private lateinit var insumeGateway: InsumeGateway
    private lateinit var notifyCustomer: NotifyCustomer
    private lateinit var createServiceOrderImpl: CreateServiceOrderImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk()
        insumeGateway = mockk()
        notifyCustomer = mockk(relaxed = true)
        createServiceOrderImpl = CreateServiceOrderImpl(
            serviceOrderGateway, insumeGateway, notifyCustomer
        )
    }

    @Test
    fun `should create service order successfully with UUIDs`() {
        val custId = UUID.randomUUID()
        val vehId = UUID.randomUUID()
        val insumeId = UUID.randomUUID()

        val request = CreateServiceOrderRequest(
            customerId = custId,
            vehicleId = vehId,
            customerEmail = "test@example.com",
            vehiclePlate = "ABC-1234",
            services = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "OIL_CHANGE",
                    fullDescription = "Oil change 5W40",
                    price = BigDecimal("100.00"),
                    insumes = listOf(
                        InsumeItemRequest(insumeId = insumeId, quantity = 2)
                    )
                )
            )
        )

        val insume = Insume(id = insumeId, name = "Oil", price = BigDecimal.TEN, unityPrice = BigDecimal.TEN)

        every { insumeGateway.findById(insumeId) } returns insume
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = createServiceOrderImpl.execute(request)

        assertEquals(custId, response.customerId)
        assertEquals(vehId, response.vehicleId)
        assertEquals(ServiceOrderStatus.RECEIVED, response.status)
        assertEquals(BigDecimal("120.00"), response.totalPrice)

        verify { serviceOrderGateway.save(any()) }
        verify { notifyCustomer.execute(any(), ServiceOrderStatus.RECEIVED) }
    }
}
