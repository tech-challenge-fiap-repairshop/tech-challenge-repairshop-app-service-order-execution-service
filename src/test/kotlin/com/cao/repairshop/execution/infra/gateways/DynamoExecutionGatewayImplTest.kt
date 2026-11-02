package com.cao.repairshop.execution.infra.gateways

import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.serviceorder.infra.gateways.mapper.ServiceOrderDynamoMapper
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DynamoExecutionGatewayImplTest {

    private lateinit var repository: DynamoServiceOrderRepository
    private lateinit var mapper: ServiceOrderDynamoMapper
    private lateinit var gateway: DynamoExecutionGatewayImpl

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        mapper = mockk(relaxed = true)
        gateway = DynamoExecutionGatewayImpl(repository, mapper)
    }

    @Test
    fun `save should map execution and persist in repository`() {
        val execId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val execution = Execution(
            id = execId,
            serviceOrderId = orderId,
            basicDescription = "Troca de vela",
            price = java.math.BigDecimal.valueOf(50.0)
        )
        val dynamoItem = ExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXEC#$execId",
            executionId = execId.toString(),
            basicDescription = "Troca de vela"
        )
        every { mapper.buildExecutionItem(execution, "ORDER#$orderId") } returns dynamoItem

        val result = gateway.save(execution)

        assertEquals(execution, result)
        verify(exactly = 1) { repository.saveExecution(dynamoItem) }
    }

    @Test
    fun `findByServiceOrderId should retrieve and map executions`() {
        val orderId = UUID.randomUUID()
        val execId = UUID.randomUUID()
        val dynamoItem = ExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXEC#$execId",
            executionId = execId.toString(),
            basicDescription = "Balanceamento"
        )
        val expectedExec = Execution(
            id = execId,
            serviceOrderId = orderId,
            basicDescription = "Balanceamento",
            price = java.math.BigDecimal.valueOf(50.0)
        )
        every { repository.getFullOrder(orderId) } returns FullServiceOrder(executions = listOf(dynamoItem))
        every { mapper.toDomainExecution(dynamoItem, orderId) } returns expectedExec

        val results = gateway.findByServiceOrderId(orderId)

        assertEquals(1, results.size)
        assertEquals(expectedExec, results[0])
    }

    @Test
    fun `default operations should return expected values`() {
        assertNull(gateway.findById(UUID.randomUUID()))
        assertDoesNotThrow { gateway.delete(UUID.randomUUID()) }
        assertEquals(30.0, gateway.getAverageExecutionTimeMinutes())
    }
}
