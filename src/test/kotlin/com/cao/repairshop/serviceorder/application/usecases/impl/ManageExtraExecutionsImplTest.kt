package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.InvalidStateTransitionException
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.controller.dtos.AddExtraExecutionsRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ApprovalRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionDefinitionRequest
import com.cao.repairshop.serviceorder.infra.gateways.mapper.ServiceOrderDynamoMapper
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class ManageExtraExecutionsImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var mapper: ServiceOrderDynamoMapper
    private lateinit var manageExtraExecutions: ManageExtraExecutionsImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk(relaxed = true)
        mapper = ServiceOrderDynamoMapper()
        manageExtraExecutions = ManageExtraExecutionsImpl(serviceOrderGateway, mapper)
    }

    @Test
    fun `addExtraExecutions should add extra executions when status is APPROVED`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.APPROVED,
            enterTime = LocalDateTime.now()
        )

        val request = AddExtraExecutionsRequest(
            notes = "Adicional de amortecedores",
            executions = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "Troca Amortecedor",
                    price = BigDecimal("300.00")
                )
            )
        )

        every { serviceOrderGateway.findDetailedById(orderId) } returns order
        every { serviceOrderGateway.saveWithExtraExecution(any(), any()) } answers { firstArg() }

        val response = manageExtraExecutions.addExtraExecutions(orderId, request)

        assertEquals(ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS, response.status)
        assertTrue(response.hasExtraServices == true)
        assertEquals(1, response.extraExecutions.size)
        verify { serviceOrderGateway.saveWithExtraExecution(order, any()) }
    }

    @Test
    fun `addExtraExecutions should throw exception when order is in invalid status`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.FINALIZED,
            enterTime = LocalDateTime.now()
        )

        every { serviceOrderGateway.findDetailedById(orderId) } returns order

        assertThrows(InvalidStateTransitionException::class.java) {
            manageExtraExecutions.addExtraExecutions(orderId, AddExtraExecutionsRequest(executions = emptyList()))
        }
    }

    @Test
    fun `approveExtraExecutions should integrate executions when approved`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS,
            enterTime = LocalDateTime.now()
        )

        val extraItem = ExtraExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXTRA_EXEC#1",
            extraExecutionId = UUID.randomUUID().toString(),
            serviceOrderId = orderId.toString(),
            status = "WAITING_APPROVAL",
            executions = emptyList()
        )

        val fullOrder = FullServiceOrder(
            metadata = null,
            histories = emptyList(),
            executions = emptyList(),
            extraExecutions = listOf(extraItem)
        )

        every { serviceOrderGateway.findDetailedById(orderId) } returns order
        every { serviceOrderGateway.getFullOrder(orderId) } returns fullOrder
        every { serviceOrderGateway.saveWithExtraExecutions(any(), any()) } answers { firstArg() }

        val response = manageExtraExecutions.approveExtraExecutions(orderId, ApprovalRequest(approved = true))

        assertEquals(ServiceOrderStatus.IN_EXECUTION, response.status)
        assertEquals(true, response.extraServicesAccepted)
    }

    @Test
    fun `approveExtraExecutions should set REFUSED when rejected`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS,
            enterTime = LocalDateTime.now()
        )

        val extraItem = ExtraExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXTRA_EXEC#1",
            extraExecutionId = UUID.randomUUID().toString(),
            serviceOrderId = orderId.toString(),
            status = "WAITING_APPROVAL",
            executions = emptyList()
        )

        val fullOrder = FullServiceOrder(
            metadata = null,
            histories = emptyList(),
            executions = emptyList(),
            extraExecutions = listOf(extraItem)
        )

        every { serviceOrderGateway.findDetailedById(orderId) } returns order
        every { serviceOrderGateway.getFullOrder(orderId) } returns fullOrder
        every { serviceOrderGateway.saveWithExtraExecutions(any(), any()) } answers { firstArg() }

        val response = manageExtraExecutions.approveExtraExecutions(orderId, ApprovalRequest(approved = false, notes = "Muito caro"))

        assertEquals(ServiceOrderStatus.IN_EXECUTION, response.status)
        assertEquals(false, response.extraServicesAccepted)
        assertEquals("REFUSED", extraItem.status)
    }
}
