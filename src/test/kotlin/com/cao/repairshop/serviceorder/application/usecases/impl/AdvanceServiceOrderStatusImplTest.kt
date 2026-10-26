package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.InvalidStateTransitionException
import com.cao.repairshop.execution.domain.BasicExecution
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.inventory.domain.entities.Insume
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.NotifyCustomer
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class AdvanceServiceOrderStatusImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var notifyCustomer: NotifyCustomer
    private lateinit var advanceServiceOrderStatusImpl: AdvanceServiceOrderStatusImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk()
        notifyCustomer = mockk(relaxed = true)
        advanceServiceOrderStatusImpl = AdvanceServiceOrderStatusImpl(
            serviceOrderGateway,
            notifyCustomer
        )
    }

    @Test
    fun `should advance status and notify customer when WAITING_APPROVAL`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()
        
        val order = ServiceOrder(
            id = orderId,
            customerId = customerId,
            vehicleId = vehicleId,
            status = ServiceOrderStatus.IN_DIAGNOSIS,
            enterTime = LocalDateTime.now()
        )
        // Add an execution so WAITING_APPROVAL validation passes
        val insume = Insume(name = "Test", price = BigDecimal.TEN, unityPrice = BigDecimal.TEN, quantity = 10)
        order.addExecution(BasicExecution.OIL_CHANGE, null, BigDecimal("100"), null, listOf(insume to 1))

        every { serviceOrderGateway.findDetailedById(orderId) } returns order
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = advanceServiceOrderStatusImpl.execute(orderId, ServiceOrderStatus.WAITING_APPROVAL)

        assertEquals(ServiceOrderStatus.WAITING_APPROVAL, response.status)
        verify { serviceOrderGateway.save(order) }
        verify { notifyCustomer.execute(any(), ServiceOrderStatus.WAITING_APPROVAL) }
    }

    @Test
    fun `should advance from RECEIVED to IN_DIAGNOSIS`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.RECEIVED,
            enterTime = LocalDateTime.now()
        )

        every { serviceOrderGateway.findDetailedById(orderId) } returns order
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = advanceServiceOrderStatusImpl.execute(orderId, ServiceOrderStatus.IN_DIAGNOSIS)

        assertEquals(ServiceOrderStatus.IN_DIAGNOSIS, response.status)
        verify { serviceOrderGateway.save(order) }
        verify { notifyCustomer.execute(any(), ServiceOrderStatus.IN_DIAGNOSIS) }
    }

    @Test
    fun `should throw InvalidStateTransitionException when transition is not allowed`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.RECEIVED,
            enterTime = LocalDateTime.now()
        )

        every { serviceOrderGateway.findDetailedById(orderId) } returns order

        assertThrows(InvalidStateTransitionException::class.java) {
            advanceServiceOrderStatusImpl.execute(orderId, ServiceOrderStatus.PAID)
        }
    }

    @Test
    fun `should throw InvalidStateTransitionException when advancing to WAITING_APPROVAL without executions`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_DIAGNOSIS,
            enterTime = LocalDateTime.now()
        )

        every { serviceOrderGateway.findDetailedById(orderId) } returns order

        assertThrows(InvalidStateTransitionException::class.java) {
            advanceServiceOrderStatusImpl.execute(orderId, ServiceOrderStatus.WAITING_APPROVAL)
        }
    }

    @Test
    fun `should throw InvalidStateTransitionException when advancing to FINALIZED but executions not finalized`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_EXECUTION,
            enterTime = LocalDateTime.now()
        )
        val execution = order.addExecution(BasicExecution.OIL_CHANGE, null, BigDecimal("100"), null, emptyList())
        execution.status = ExecutionStatus.PENDING

        every { serviceOrderGateway.findDetailedById(orderId) } returns order

        assertThrows(InvalidStateTransitionException::class.java) {
            advanceServiceOrderStatusImpl.execute(orderId, ServiceOrderStatus.FINALIZED)
        }
    }

    @Test
    fun `should advance to FINALIZED and set endTime when all executions are finalized`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_EXECUTION,
            enterTime = LocalDateTime.now()
        )
        val execution = order.addExecution(BasicExecution.OIL_CHANGE, null, BigDecimal("100"), null, emptyList())
        execution.status = ExecutionStatus.FINALIZED

        every { serviceOrderGateway.findDetailedById(orderId) } returns order
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = advanceServiceOrderStatusImpl.execute(orderId, ServiceOrderStatus.FINALIZED)

        assertEquals(ServiceOrderStatus.FINALIZED, response.status)
        assertNotNull(order.endTime)
    }

    @Test
    fun `should throw error when order not found`() {
        val orderId = UUID.randomUUID()
        every { serviceOrderGateway.findDetailedById(orderId) } returns null

        assertThrows(EntityNotFoundException::class.java) {
            advanceServiceOrderStatusImpl.execute(orderId, ServiceOrderStatus.IN_DIAGNOSIS)
        }
    }
}
