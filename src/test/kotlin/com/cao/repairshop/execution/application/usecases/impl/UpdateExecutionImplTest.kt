package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.infra.controller.dtos.UpdateExecutionRequest
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class UpdateExecutionImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var updateExecutionImpl: UpdateExecutionImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk()
        updateExecutionImpl = UpdateExecutionImpl(serviceOrderGateway)
    }

    @Test
    fun `should update execution successfully`() {
        val serviceOrderId = UUID.randomUUID()
        val executionId = UUID.randomUUID()
        
        val request = UpdateExecutionRequest(
            basicDescription = "BRAKE_INSPECTION",
            fullDescription = "Check front brakes",
            price = BigDecimal("150.00"),
            estimatedTime = BigDecimal("2.5")
        )

        val serviceOrder = ServiceOrder(
            id = serviceOrderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            enterTime = LocalDateTime.now()
        )
        
        val execution = Execution(
            id = executionId,
            serviceOrderId = serviceOrderId,
            basicDescription = "OIL_CHANGE",
            fullDescription = "Oil change",
            price = BigDecimal("100.00")
        )
        serviceOrder.executions.add(execution)

        every { serviceOrderGateway.findDetailedById(serviceOrderId) } returns serviceOrder
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = updateExecutionImpl.execute(serviceOrderId, executionId, request)

        assertEquals("BRAKE_INSPECTION", response.basicDescription)
        assertEquals("Check front brakes", response.fullDescription)
        assertEquals(BigDecimal("150.00"), response.laborPrice)
        assertEquals(BigDecimal("150.00"), serviceOrder.totalPrice)
        
        verify { serviceOrderGateway.save(serviceOrder) }
    }

    @Test
    fun `should throw error when order not found`() {
        every { serviceOrderGateway.findDetailedById(any()) } returns null

        assertThrows(EntityNotFoundException::class.java) {
            updateExecutionImpl.execute(UUID.randomUUID(), UUID.randomUUID(), mockk(relaxed = true))
        }
    }

    @Test
    fun `should throw error when execution not found in order`() {
        val serviceOrderId = UUID.randomUUID()
        val serviceOrder = ServiceOrder(
            id = serviceOrderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID()
        )

        every { serviceOrderGateway.findDetailedById(serviceOrderId) } returns serviceOrder

        assertThrows(EntityNotFoundException::class.java) {
            updateExecutionImpl.execute(serviceOrderId, UUID.randomUUID(), mockk(relaxed = true))
        }
    }
}
