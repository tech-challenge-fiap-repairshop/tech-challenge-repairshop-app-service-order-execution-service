package com.cao.repairshop.serviceorder.infra.gateways.mapper

import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.controller.dtos.AddExtraExecutionsRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionDefinitionRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeItemRequest
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.InsumeDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderHistoryDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderMetadataDynamoItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class ServiceOrderDynamoMapperTest {

    private val mapper = ServiceOrderDynamoMapper()

    @Test
    fun `buildMetadataItem should map ServiceOrder properties correctly`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val order = ServiceOrder(
            id = orderId,
            customerId = customerId,
            vehicleId = vehicleId,
            status = ServiceOrderStatus.IN_DIAGNOSIS,
            totalPrice = BigDecimal("250.00"),
            enterTime = LocalDateTime.now(),
            hasExtraServices = true,
            extraServicesAccepted = false
        )

        val metadata = mapper.buildMetadataItem(order, "ORDER#$orderId")

        assertEquals("ORDER#$orderId", metadata.pk)
        assertEquals("METADATA", metadata.sk)
        assertEquals(orderId.toString(), metadata.serviceOrderId)
        assertEquals(customerId.toString(), metadata.customerId)
        assertEquals(vehicleId.toString(), metadata.vehicleId)
        assertEquals(250.0, metadata.totalServiceOrderPrice)
        assertEquals("IN_DIAGNOSIS", metadata.status)
        assertTrue(metadata.hasExtraServices)
        assertEquals(false, metadata.extraServicesAccepted)
    }

    @Test
    fun `buildExecutionItem should map Execution and embedded insumes`() {
        val orderId = UUID.randomUUID()
        val exec = Execution(
            serviceOrderId = orderId,
            basicDescription = "Troca de Óleo",
            fullDescription = "Sintético 5W30",
            price = BigDecimal("150.00")
        )
        val insume = Insume(
            name = "Óleo 5W30",
            quantity = 4,
            unityPrice = BigDecimal("30.00")
        )
        exec.addInsume(insume, 4)

        val item = mapper.buildExecutionItem(exec, "ORDER#$orderId")

        assertEquals("ORDER#$orderId", item.pk)
        assertTrue(item.sk.startsWith("EXEC#"))
        assertEquals("Troca de Óleo", item.taskName)
        assertEquals(150.0, item.executionPrice)
        assertEquals(1, item.insumes.size)
        assertEquals("Óleo 5W30", item.insumes[0].name)
        assertEquals(4.0, item.insumes[0].quantity)
    }

    @Test
    fun `toDomainOrder should reconstruct ServiceOrder from FullServiceOrder`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val metadata = ServiceOrderMetadataDynamoItem(
            pk = "ORDER#$orderId",
            sk = "METADATA",
            serviceOrderId = orderId.toString(),
            customerId = customerId.toString(),
            vehicleId = vehicleId.toString(),
            totalServiceOrderPrice = 200.0,
            status = "IN_EXECUTION"
        )

        val executionItem = ExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXEC#123",
            executionId = UUID.randomUUID().toString(),
            taskName = "Alinhamento",
            basicDescription = "Alinhamento",
            executionPrice = 200.0,
            status = "INITIATED",
            insumes = emptyList()
        )

        val fullOrder = FullServiceOrder(
            metadata = metadata,
            histories = emptyList(),
            executions = listOf(executionItem),
            extraExecutions = emptyList()
        )

        val order = mapper.toDomainOrder(fullOrder)

        assertNotNull(order)
        assertEquals(orderId, order?.id)
        assertEquals(ServiceOrderStatus.IN_EXECUTION, order?.status)
        assertEquals(1, order?.executions?.size)
        assertEquals("Alinhamento", order?.executions?.firstOrNull()?.basicDescription)
    }

    @Test
    fun `buildExtraExecutionItem should construct extra batch and embedded executions`() {
        val orderId = UUID.randomUUID()
        val request = AddExtraExecutionsRequest(
            notes = "Revisão complementar",
            executions = listOf(
                ExecutionDefinitionRequest(
                    basicDescription = "Pastilhas",
                    price = BigDecimal("100.00"),
                    insumes = listOf(
                        InsumeItemRequest(name = "Pastilha Dianteira", quantity = 1, unityPrice = BigDecimal("80.00"))
                    )
                )
            )
        )

        val extraItem = mapper.buildExtraExecutionItem(orderId, request)

        assertEquals("ORDER#$orderId", extraItem.pk)
        assertTrue(extraItem.sk.startsWith("EXTRA_EXEC#"))
        assertEquals("WAITING_APPROVAL", extraItem.status)
        assertEquals(180.0, extraItem.totalExtraPrice)
        assertEquals("Revisão complementar", extraItem.notes)
        assertEquals(1, extraItem.executions.size)
    }

    @Test
    fun `toDomainExecution should preserve created timestamp and reuse exact SK on rebuild`() {
        val orderId = UUID.randomUUID()
        val execId = UUID.randomUUID()
        val originalTs = 1794696743145L
        val originalSk = "EXEC#$originalTs#$execId"

        val dynamoItem = ExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = originalSk,
            executionId = execId.toString(),
            taskName = "Revisao",
            basicDescription = "Revisao",
            executionPrice = 350.0,
            status = "PENDING",
            createDate = Instant.ofEpochMilli(originalTs).toString(),
            updateDate = Instant.ofEpochMilli(originalTs + 5000).toString(),
            insumes = emptyList()
        )

        val domainExec = mapper.toDomainExecution(dynamoItem, orderId)
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(originalTs), ZoneOffset.UTC), domainExec.created)
        assertEquals(ExecutionStatus.PENDING, domainExec.status)

        domainExec.advanceStatus(ExecutionStatus.FINALIZED)
        val rebuiltItem = mapper.buildExecutionItem(domainExec, "ORDER#$orderId")
        assertEquals(originalSk, rebuiltItem.sk)
        assertEquals("FINALIZED", rebuiltItem.status)
    }

    @Test
    fun `toDomainOrder should keep latest duplicate execution status and preserve history registerTime`() {
        val orderId = UUID.randomUUID()
        val execId = UUID.randomUUID()
        val histId = UUID.randomUUID()
        val histTs = 1794696743142L

        val metadata = ServiceOrderMetadataDynamoItem(
            pk = "ORDER#$orderId",
            sk = "METADATA",
            serviceOrderId = orderId.toString(),
            customerId = UUID.randomUUID().toString(),
            vehicleId = UUID.randomUUID().toString(),
            totalServiceOrderPrice = 350.0,
            status = "IN_EXECUTION"
        )

        val olderExec = ExecutionDynamoItem(
            pk = "ORDER#$orderId",
            sk = "EXEC#1794696743145#$execId",
            executionId = execId.toString(),
            taskName = "Revisao",
            basicDescription = "Revisao",
            executionPrice = 350.0,
            status = "INITIATED",
            insumes = emptyList()
        )
        val newerExec = olderExec.copy(
            sk = "EXEC#1794696756124#$execId",
            status = "PENDING"
        )

        val histItem = ServiceOrderHistoryDynamoItem(
            pk = "ORDER#$orderId",
            sk = "HIST#$histTs#$histId",
            historyId = histId.toString(),
            status = "IN_DIAGNOSIS",
            registerTime = Instant.ofEpochMilli(histTs).toString()
        )

        val fullOrder = FullServiceOrder(
            metadata = metadata,
            histories = listOf(histItem),
            executions = listOf(olderExec, newerExec),
            extraExecutions = emptyList()
        )

        val domainOrder = mapper.toDomainOrder(fullOrder)
        assertNotNull(domainOrder)
        assertEquals(1, domainOrder?.executions?.size)
        assertEquals(ExecutionStatus.PENDING, domainOrder?.executions?.first()?.status)
        assertEquals(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(histTs), ZoneOffset.UTC),
            domainOrder?.histories?.first()?.registerTime
        )
    }
}
