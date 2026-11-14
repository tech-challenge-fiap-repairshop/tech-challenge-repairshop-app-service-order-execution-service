package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class RemoveExecutionImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var removeExecutionImpl: RemoveExecutionImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk()
        removeExecutionImpl = RemoveExecutionImpl(serviceOrderGateway)
    }

    @Test
    fun `should remove execution successfully`() {
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
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        removeExecutionImpl.execute(serviceOrderId, executionId)

        assert(serviceOrder.executions.isEmpty())
        verify { serviceOrderGateway.save(serviceOrder) }
    }

    @Test
    fun `should throw error when order not found`() {
        every { serviceOrderGateway.findDetailedById(any()) } returns null

        assertThrows(EntityNotFoundException::class.java) {
            removeExecutionImpl.execute(UUID.randomUUID(), UUID.randomUUID())
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
            removeExecutionImpl.execute(serviceOrderId, UUID.randomUUID())
        }
    }
}
