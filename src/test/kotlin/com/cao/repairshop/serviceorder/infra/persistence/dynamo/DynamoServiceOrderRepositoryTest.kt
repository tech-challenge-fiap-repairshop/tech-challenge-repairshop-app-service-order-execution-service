package com.cao.repairshop.serviceorder.infra.persistence.dynamo

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import java.util.UUID

class DynamoServiceOrderRepositoryTest {

    private lateinit var dynamoDbClient: DynamoDbClient
    private lateinit var enhancedClient: DynamoDbEnhancedClient
    private lateinit var repository: DynamoServiceOrderRepository
    private val tableName = "tb-service_order-test"

    @BeforeEach
    fun setup() {
        dynamoDbClient = mockk(relaxed = true)
        enhancedClient = mockk(relaxed = true)
        repository = DynamoServiceOrderRepository(dynamoDbClient, enhancedClient, tableName)
    }

    @Test
    fun `save should put item with description and originTraceId`() {
        val order = ServiceOrderDynamoItem(
            id = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = "RECEIVED",
            totalPrice = 150.0,
            description = "Troca de óleo",
            paymentDispatchStatus = "PENDING",
            updateDate = "2026-10-26T12:00:00Z",
            originTraceId = "trace-123"
        )

        val slot = slot<PutItemRequest>()
        every { dynamoDbClient.putItem(capture(slot)) } returns mockk()

        repository.save(order)

        verify(exactly = 1) { dynamoDbClient.putItem(any<PutItemRequest>()) }
        val captured = slot.captured
        assertEquals(tableName, captured.tableName())
        assertEquals("ORDER#${order.id}", captured.item()["PK"]?.s())
        assertEquals("METADATA", captured.item()["SK"]?.s())
        assertEquals("Troca de óleo", captured.item()["description"]?.s())
        assertEquals("trace-123", captured.item()["origin_trace_id"]?.s())
        assertEquals("150.0", captured.item()["total_price"]?.n())
    }

    @Test
    fun `save should put item when description and originTraceId are null`() {
        val order = ServiceOrderDynamoItem(
            id = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = "RECEIVED",
            totalPrice = 0.0,
            description = null,
            originTraceId = null
        )

        val slot = slot<PutItemRequest>()
        every { dynamoDbClient.putItem(capture(slot)) } returns mockk()

        repository.save(order)

        verify(exactly = 1) { dynamoDbClient.putItem(any<PutItemRequest>()) }
        val captured = slot.captured
        assertNull(captured.item()["description"])
        assertNull(captured.item()["origin_trace_id"])
    }

    @Test
    fun `findById should return null when response has no item`() {
        val id = UUID.randomUUID()
        val response = GetItemResponse.builder().build()
        every { dynamoDbClient.getItem(any<GetItemRequest>()) } returns response

        val result = repository.findById(id)

        assertNull(result)
    }

    @Test
    fun `findById should return null when item map is empty`() {
        val id = UUID.randomUUID()
        val response = GetItemResponse.builder().item(emptyMap()).build()
        every { dynamoDbClient.getItem(any<GetItemRequest>()) } returns response

        val result = repository.findById(id)

        assertNull(result)
    }

    @Test
    fun `findById should return item when found with all attributes`() {
        val id = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val itemMap = mapOf(
            "id" to AttributeValue.builder().s(id.toString()).build(),
            "customer_id" to AttributeValue.builder().s(customerId.toString()).build(),
            "vehicle_id" to AttributeValue.builder().s(vehicleId.toString()).build(),
            "status" to AttributeValue.builder().s("IN_EXECUTION").build(),
            "total_price" to AttributeValue.builder().n("250.5").build(),
            "description" to AttributeValue.builder().s("Reparo freio").build(),
            "payment_dispatch_status" to AttributeValue.builder().s("DISPATCHED").build(),
            "update_date" to AttributeValue.builder().s("2026-10-26T10:00:00Z").build(),
            "origin_trace_id" to AttributeValue.builder().s("trace-abc").build()
        )

        val response = GetItemResponse.builder().item(itemMap).build()
        every { dynamoDbClient.getItem(any<GetItemRequest>()) } returns response

        val result = repository.findById(id)

        assertNotNull(result)
        assertEquals(id, result?.id)
        assertEquals(customerId, result?.customerId)
        assertEquals(vehicleId, result?.vehicleId)
        assertEquals("IN_EXECUTION", result?.status)
        assertEquals(250.5, result?.totalPrice)
        assertEquals("Reparo freio", result?.description)
        assertEquals("DISPATCHED", result?.paymentDispatchStatus)
        assertEquals("2026-10-26T10:00:00Z", result?.updateDate)
        assertEquals("trace-abc", result?.originTraceId)
    }

    @Test
    fun `findById should use fallbacks when optional fields are missing in item`() {
        val id = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val itemMap = mapOf(
            "customer_id" to AttributeValue.builder().s(customerId.toString()).build(),
            "vehicle_id" to AttributeValue.builder().s(vehicleId.toString()).build(),
            "status" to AttributeValue.builder().s("APPROVED").build()
        )

        val response = GetItemResponse.builder().item(itemMap).build()
        every { dynamoDbClient.getItem(any<GetItemRequest>()) } returns response

        val result = repository.findById(id)

        assertNotNull(result)
        assertEquals(id, result?.id)
        assertEquals(0.0, result?.totalPrice)
        assertEquals("PENDING", result?.paymentDispatchStatus)
        assertNull(result?.description)
        assertNull(result?.originTraceId)
    }

    @Test
    fun `updateStatus should update status without dispatch status`() {
        val id = UUID.randomUUID()
        val slot = slot<TransactWriteItemsRequest>()
        every { dynamoDbClient.transactWriteItems(capture(slot)) } returns mockk()

        repository.updateStatus(id, "FINALIZED")

        verify(exactly = 1) { dynamoDbClient.transactWriteItems(any<TransactWriteItemsRequest>()) }
        val captured = slot.captured
        assertEquals(2, captured.transactItems().size)
        val update = captured.transactItems()[0].update()
        assertEquals(tableName, update.tableName())
        assertEquals("ORDER#$id", update.key()["PK"]?.s())
        assertEquals("FINALIZED", update.expressionAttributeValues()[":newStatus"]?.s())
        assertFalse(update.updateExpression().contains("#pds"))

        val putHist = captured.transactItems()[1].put()
        assertEquals(tableName, putHist.tableName())
        assertEquals("ORDER#$id", putHist.item()["PK"]?.s())
        assertTrue(putHist.item()["SK"]?.s()?.startsWith("HIST#") == true)
        assertEquals("FINALIZED", putHist.item()["status"]?.s())
    }

    @Test
    fun `updateStatus should update status with new dispatch status`() {
        val id = UUID.randomUUID()
        val slot = slot<TransactWriteItemsRequest>()
        every { dynamoDbClient.transactWriteItems(capture(slot)) } returns mockk()

        repository.updateStatus(id, "PAID", "DISPATCHED")

        verify(exactly = 1) { dynamoDbClient.transactWriteItems(any<TransactWriteItemsRequest>()) }
        val captured = slot.captured
        assertEquals(2, captured.transactItems().size)
        val update = captured.transactItems()[0].update()
        assertTrue(update.updateExpression().contains("#pds = :pds"))
        assertEquals("DISPATCHED", update.expressionAttributeValues()[":pds"]?.s())

        val putHist = captured.transactItems()[1].put()
        assertEquals("PAID", putHist.item()["status"]?.s())
    }

    @Test
    fun `findPendingNotification should return items from query`() {
        val id = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val validItem = mapOf(
            "id" to AttributeValue.builder().s(id.toString()).build(),
            "customer_id" to AttributeValue.builder().s(customerId.toString()).build(),
            "vehicle_id" to AttributeValue.builder().s(vehicleId.toString()).build(),
            "status" to AttributeValue.builder().s("WAITING_APPROVAL").build()
        )
        val invalidItemWithoutId = mapOf(
            "customer_id" to AttributeValue.builder().s(customerId.toString()).build()
        )

        val response = QueryResponse.builder()
            .items(listOf(validItem, invalidItemWithoutId))
            .build()
        every { dynamoDbClient.query(any<QueryRequest>()) } returns response

        val results = repository.findPendingNotification()

        assertEquals(1, results.size)
        assertEquals(id, results[0].id)
    }

    @Test
    fun `findPendingNotification should return empty list on exception`() {
        every { dynamoDbClient.query(any<QueryRequest>()) } throws RuntimeException("DynamoDB error")

        val results = repository.findPendingNotification()

        assertTrue(results.isEmpty())
    }
}
