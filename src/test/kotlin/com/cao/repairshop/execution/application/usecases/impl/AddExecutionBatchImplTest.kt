package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionBatchRequest
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionDefinitionRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeItemRequest
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

class AddExecutionBatchImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var addExecutionBatchImpl: AddExecutionBatchImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk()
        addExecutionBatchImpl = AddExecutionBatchImpl(serviceOrderGateway)
    }

    @Test
    fun `should add execution batch successfully`() {
        val serviceOrderId = UUID.randomUUID()
        val insumeId = UUID.randomUUID()

        val request = CreateExecutionBatchRequest(
            serviceOrderId = serviceOrderId,
            executions = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "OIL_CHANGE",
                    fullDescription = "Oil change",
                    price = BigDecimal("100.00"),
                    insumes = listOf(
                        InsumeItemRequest(insumeId = insumeId, name = "Oil", quantity = 2, unityPrice = BigDecimal.TEN)
                    )
                )
            )
        )

        val serviceOrder = ServiceOrder(
            id = serviceOrderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            enterTime = LocalDateTime.now()
        )

        every { serviceOrderGateway.findDetailedById(serviceOrderId) } returns serviceOrder
        every { serviceOrderGateway.save(any()) } answers { firstArg() }

        val response = addExecutionBatchImpl.execute(serviceOrderId, request)

        assertEquals(1, response.size)
        assertEquals("OIL_CHANGE", response[0].basicDescription)
        assertEquals(BigDecimal("120.00"), response[0].totalPrice)
        
        verify { serviceOrderGateway.save(serviceOrder) }
    }

    @Test
    fun `should throw error when order not found`() {
        every { serviceOrderGateway.findDetailedById(any()) } returns null

        assertThrows(EntityNotFoundException::class.java) {
            addExecutionBatchImpl.execute(UUID.randomUUID(), mockk(relaxed = true))
        }
    }

    @Test
    fun `should throw error when order is in terminal state`() {
        val serviceOrderId = UUID.randomUUID()
        val request = CreateExecutionBatchRequest(
            serviceOrderId = serviceOrderId,
            executions = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "OIL_CHANGE",
                    price = BigDecimal("100.00")
                )
            )
        )

        val serviceOrder = ServiceOrder(
            id = serviceOrderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = com.cao.repairshop.serviceorder.domain.ServiceOrderStatus.FINALIZED
        )

        every { serviceOrderGateway.findDetailedById(serviceOrderId) } returns serviceOrder

        assertThrows(com.cao.repairshop.core.exception.InvalidStateTransitionException::class.java) {
            addExecutionBatchImpl.execute(serviceOrderId, request)
        }
    }
}
