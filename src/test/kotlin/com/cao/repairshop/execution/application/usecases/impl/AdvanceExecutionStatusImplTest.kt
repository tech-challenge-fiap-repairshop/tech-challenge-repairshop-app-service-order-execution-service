package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
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

class AdvanceExecutionStatusImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var advanceExecutionStatusImpl: AdvanceExecutionStatusImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk()
        advanceExecutionStatusImpl = AdvanceExecutionStatusImpl(serviceOrderGateway)
    }

    @Test
    fun `should advance execution status successfully`() {
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
            price = BigDecimal("100.00"),
            status = ExecutionStatus.INITIATED
        )
        serviceOrder.executions.add(execution)

        every { serviceOrderGateway.findDetailedById(serviceOrderId) } returns serviceOrder
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = advanceExecutionStatusImpl.execute(serviceOrderId, executionId, ExecutionStatus.PENDING)

        assertEquals(ExecutionStatus.PENDING, response.status)
        verify { serviceOrderGateway.save(serviceOrder) }
    }

    @Test
    fun `should advance execution status and finalize order when all executions are finalized`() {
        val serviceOrderId = UUID.randomUUID()
        val executionId = UUID.randomUUID()

        val serviceOrder = ServiceOrder(
            id = serviceOrderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_EXECUTION,
            enterTime = LocalDateTime.now()
        )
        
        val execution = Execution(
            id = executionId,
            serviceOrderId = serviceOrderId,
            basicDescription = "OIL_CHANGE",
            fullDescription = "Oil change",
            price = BigDecimal("100.00"),
            status = ExecutionStatus.PENDING
        )
        serviceOrder.executions.add(execution)

        every { serviceOrderGateway.findDetailedById(serviceOrderId) } returns serviceOrder
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = advanceExecutionStatusImpl.execute(serviceOrderId, executionId, ExecutionStatus.FINALIZED)

        assertEquals(ExecutionStatus.FINALIZED, response.status)
        assertEquals(ServiceOrderStatus.FINALIZED, serviceOrder.status)
        verify { serviceOrderGateway.save(serviceOrder) }
    }

    @Test
    fun `should throw error when order not found`() {
        every { serviceOrderGateway.findDetailedById(any()) } returns null

        assertThrows(EntityNotFoundException::class.java) {
            advanceExecutionStatusImpl.execute(UUID.randomUUID(), UUID.randomUUID(), ExecutionStatus.INITIATED)
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
            advanceExecutionStatusImpl.execute(serviceOrderId, UUID.randomUUID(), ExecutionStatus.INITIATED)
        }
    }
}
