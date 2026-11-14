package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class FindExecutionImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var findExecutionImpl: FindExecutionImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk()
        findExecutionImpl = FindExecutionImpl(serviceOrderGateway)
    }

    @Test
    fun `should find execution successfully`() {
        val serviceOrderId = UUID.randomUUID()
        val executionId = UUID.randomUUID()

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

        val response = findExecutionImpl.findById(serviceOrderId, executionId)

        assertEquals(executionId, response.id)
        assertEquals("OIL_CHANGE", response.basicDescription)
    }

    @Test
    fun `should throw error when order not found`() {
        every { serviceOrderGateway.findDetailedById(any()) } returns null

        assertThrows(EntityNotFoundException::class.java) {
            findExecutionImpl.findById(UUID.randomUUID(), UUID.randomUUID())
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
            findExecutionImpl.findById(serviceOrderId, UUID.randomUUID())
        }
    }
}
