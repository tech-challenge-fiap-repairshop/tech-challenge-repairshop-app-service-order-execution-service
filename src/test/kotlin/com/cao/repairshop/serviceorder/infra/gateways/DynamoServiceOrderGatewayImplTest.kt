package com.cao.repairshop.serviceorder.infra.gateways

import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.gateways.mapper.ServiceOrderDynamoMapper
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.ServiceOrderDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderMetadataDynamoItem
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import java.util.UUID

class DynamoServiceOrderGatewayImplTest {

    private lateinit var repository: DynamoServiceOrderRepository
    private lateinit var mapper: ServiceOrderDynamoMapper
    private lateinit var gateway: DynamoServiceOrderGatewayImpl

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        mapper = mockk(relaxed = true)
        gateway = DynamoServiceOrderGatewayImpl(repository, mapper)
    }

    @Test
    fun `save should build items and invoke saveTransactional`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_DIAGNOSIS
        )
        val meta = mockk<ServiceOrderMetadataDynamoItem>()
        every { mapper.buildMetadataItem(order, "ORDER#$orderId") } returns meta
        every { mapper.buildHistoryItems(order, "ORDER#$orderId") } returns emptyList()
        every { mapper.buildExecutionItems(order, "ORDER#$orderId") } returns emptyList()
        every { repository.getFullOrder(orderId) } returns FullServiceOrder()

        val result = gateway.save(order)

        assertEquals(order, result)
        verify(exactly = 1) {
            repository.saveTransactional(
                metadata = meta,
                histories = emptyList(),
                executions = emptyList(),
                executionsToDelete = emptyList()
            )
        }
    }

    @Test
    fun `save should detect executions to delete when removed from aggregate`() {
        val orderId = UUID.randomUUID()
        val remainingExecId = UUID.randomUUID()
        val removedExecId = UUID.randomUUID()

        val execution = Execution(
            id = remainingExecId,
            serviceOrderId = orderId,
            basicDescription = "Troca de óleo",
            price = java.math.BigDecimal.valueOf(50.0)
        )
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_DIAGNOSIS,
            executions = mutableSetOf(execution)
        )

        val existingFull = FullServiceOrder(
            executions = listOf(
                ExecutionDynamoItem(
                    pk = "ORDER#$orderId",
                    sk = "EXEC#$remainingExecId",
                    executionId = remainingExecId.toString(),
                    basicDescription = "Troca de óleo"
                ),
                ExecutionDynamoItem(
                    pk = "ORDER#$orderId",
                    sk = "EXEC#$removedExecId",
                    executionId = removedExecId.toString(),
                    basicDescription = "Alinhamento"
                )
            )
        )
        every { repository.getFullOrder(orderId) } returns existingFull

        gateway.save(order)

        verify(exactly = 1) {
            repository.saveTransactional(
                metadata = any(),
                histories = any(),
                executions = any(),
                executionsToDelete = listOf("EXEC#$removedExecId")
            )
        }
    }

    @Test
    fun `saveWithExtraExecution should invoke saveTransactional with extra item`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_DIAGNOSIS
        )
        val extraItem = ExtraExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXTRA#1",
            extraExecutionId = UUID.randomUUID().toString(),
            serviceOrderId = orderId.toString()
        )

        gateway.saveWithExtraExecution(order, extraItem)

        verify(exactly = 1) {
            repository.saveTransactional(
                metadata = any(),
                histories = any(),
                executions = any(),
                extraExecutions = listOf(extraItem)
            )
        }
    }

    @Test
    fun `saveWithExtraExecutions should invoke saveTransactional with extra items list`() {
        val orderId = UUID.randomUUID()
        val order = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_DIAGNOSIS
        )
        val extraItem = ExtraExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXTRA#1",
            extraExecutionId = UUID.randomUUID().toString(),
            serviceOrderId = orderId.toString()
        )

        gateway.saveWithExtraExecutions(order, listOf(extraItem))

        verify(exactly = 1) {
            repository.saveTransactional(
                metadata = any(),
                histories = any(),
                executions = any(),
                extraExecutions = listOf(extraItem)
            )
        }
    }

    @Test
    fun `findDetailedById should return domain order when found`() {
        val orderId = UUID.randomUUID()
        val fullOrder = FullServiceOrder()
        val expectedOrder = ServiceOrder(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = ServiceOrderStatus.IN_DIAGNOSIS
        )
        every { repository.getFullOrder(orderId) } returns fullOrder
        every { mapper.toDomainOrder(fullOrder) } returns expectedOrder

        val result = gateway.findDetailedById(orderId)

        assertEquals(expectedOrder, result)
    }

    @Test
    fun `findDetailedById should return null when not found`() {
        val orderId = UUID.randomUUID()
        every { repository.getFullOrder(orderId) } returns FullServiceOrder()
        every { mapper.toDomainOrder(any()) } returns null

        val result = gateway.findDetailedById(orderId)

        assertNull(result)
    }

    @Test
    fun `findAll should return paged service orders`() {
        val orderId = UUID.randomUUID()
        val pending = ServiceOrderDynamoItem(
            id = orderId,
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = "IN_DIAGNOSIS"
        )
        every { repository.findPendingNotification() } returns listOf(pending)
        val domainOrder = ServiceOrder(
            id = orderId,
            customerId = pending.customerId,
            vehicleId = pending.vehicleId,
            status = ServiceOrderStatus.IN_DIAGNOSIS
        )
        every { mapper.toDomainOrder(any()) } returns domainOrder

        val page = gateway.findAll(null, null, null, PageRequest.of(0, 10))

        assertEquals(1, page.content.size)
        assertEquals(orderId, page.content[0].id)
    }

    @Test
    fun `delegation methods should forward calls to repository`() {
        val orderId = UUID.randomUUID()
        val extraItem = ExtraExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXTRA#1",
            extraExecutionId = UUID.randomUUID().toString(),
            serviceOrderId = orderId.toString()
        )

        gateway.saveExtraExecution(extraItem)
        verify(exactly = 1) { repository.saveExtraExecution(extraItem) }

        gateway.getFullOrder(orderId)
        verify(exactly = 1) { repository.getFullOrder(orderId) }

        gateway.updateStatus(
            id = orderId,
            newStatus = "PAID",
            newDispatchStatus = "DISPATCHED",
            paymentStatus = "APPROVED",
            hasExtraServices = true,
            extraServicesAccepted = true,
            totalPrice = 150.0
        )
        verify(exactly = 1) {
            repository.updateStatus(
                id = orderId,
                newStatus = "PAID",
                newDispatchStatus = "DISPATCHED",
                paymentStatus = "APPROVED",
                hasExtraServices = true,
                extraServicesAccepted = true,
                totalPrice = 150.0
            )
        }
    }
}
