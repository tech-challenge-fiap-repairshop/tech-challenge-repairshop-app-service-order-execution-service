package com.cao.repairshop.serviceorder.infra.persistence.dynamo.mapper

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.UUID

class DynamoItemParserTest {

    private val parser = DynamoItemParser()

    @Test
    fun `parseFullOrder should return empty FullServiceOrder when items list is empty`() {
        val orderId = UUID.randomUUID()
        val result = parser.parseFullOrder(orderId, emptyList())

        assertNull(result.metadata)
        assertTrue(result.histories.isEmpty())
        assertTrue(result.executions.isEmpty())
        assertTrue(result.extraExecutions.isEmpty())
    }

    @Test
    fun `parseFullOrder should parse metadata, history, execution and extra execution items`() {
        val orderId = UUID.randomUUID()
        val pk = "ORDER#$orderId"

        val metadataItem = mapOf(
            "PK" to AttributeValue.builder().s(pk).build(),
            "SK" to AttributeValue.builder().s("METADATA").build(),
            "service_order_id" to AttributeValue.builder().s(orderId.toString()).build(),
            "customer_id" to AttributeValue.builder().s(UUID.randomUUID().toString()).build(),
            "vehicle_id" to AttributeValue.builder().s(UUID.randomUUID().toString()).build(),
            "status" to AttributeValue.builder().s("IN_EXECUTION").build(),
            "total_service_order_price" to AttributeValue.builder().n("300.0").build(),
            "enter_time" to AttributeValue.builder().s("2026-10-27T00:00:00Z").build(),
            "payment_status" to AttributeValue.builder().s("PENDING").build(),
            "payment_dispatch_status" to AttributeValue.builder().s("PENDING").build()
        )

        val historyItem = mapOf(
            "PK" to AttributeValue.builder().s(pk).build(),
            "SK" to AttributeValue.builder().s("HIST#1700000000#1").build(),
            "history_id" to AttributeValue.builder().s("hist-1").build(),
            "status" to AttributeValue.builder().s("IN_EXECUTION").build(),
            "register_time" to AttributeValue.builder().s("2026-10-27T00:00:00Z").build()
        )

        val insumeMap = mapOf(
            "id_insume" to AttributeValue.builder().s("ins-1").build(),
            "name" to AttributeValue.builder().s("Filtro").build(),
            "quantity" to AttributeValue.builder().n("1").build(),
            "unity_insume_price" to AttributeValue.builder().n("50.0").build(),
            "insume_price" to AttributeValue.builder().n("50.0").build()
        )

        val executionItem = mapOf(
            "PK" to AttributeValue.builder().s(pk).build(),
            "SK" to AttributeValue.builder().s("EXEC#1700000000#1").build(),
            "execution_id" to AttributeValue.builder().s("exec-1").build(),
            "task_name" to AttributeValue.builder().s("Troca Filtro").build(),
            "basic_description" to AttributeValue.builder().s("Troca Filtro").build(),
            "execution_price" to AttributeValue.builder().n("150.0").build(),
            "status" to AttributeValue.builder().s("INITIATED").build(),
            "insumes" to AttributeValue.builder().l(AttributeValue.builder().m(insumeMap).build()).build()
        )

        val extraExecutionItem = mapOf(
            "PK" to AttributeValue.builder().s(pk).build(),
            "SK" to AttributeValue.builder().s("EXTRA_EXEC#1700000000#1").build(),
            "extra_execution_id" to AttributeValue.builder().s("extra-1").build(),
            "service_order_id" to AttributeValue.builder().s(orderId.toString()).build(),
            "status" to AttributeValue.builder().s("WAITING_APPROVAL").build(),
            "total_extra_price" to AttributeValue.builder().n("100.0").build(),
            "executions" to AttributeValue.builder().l(emptyList()).build()
        )

        val result = parser.parseFullOrder(orderId, listOf(metadataItem, historyItem, executionItem, extraExecutionItem))

        assertNotNull(result.metadata)
        assertEquals("IN_EXECUTION", result.metadata?.status)
        assertEquals(300.0, result.metadata?.totalServiceOrderPrice)

        assertEquals(1, result.histories.size)
        assertEquals("hist-1", result.histories[0].historyId)

        assertEquals(1, result.executions.size)
        assertEquals("exec-1", result.executions[0].executionId)
        assertEquals(1, result.executions[0].insumes.size)
        assertEquals("Filtro", result.executions[0].insumes[0].name)

        assertEquals(1, result.extraExecutions.size)
        assertEquals("extra-1", result.extraExecutions[0].extraExecutionId)
    }

    @Test
    fun `parsePendingNotifications should extract valid items and ignore invalid ones`() {
        val validId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()

        val validItem = mapOf(
            "id" to AttributeValue.builder().s(validId.toString()).build(),
            "customer_id" to AttributeValue.builder().s(customerId.toString()).build(),
            "vehicle_id" to AttributeValue.builder().s(vehicleId.toString()).build(),
            "status" to AttributeValue.builder().s("WAITING_APPROVAL").build(),
            "total_price" to AttributeValue.builder().n("200.0").build()
        )

        val invalidItem = mapOf(
            "status" to AttributeValue.builder().s("WAITING_APPROVAL").build()
        )

        val result = parser.parsePendingNotifications(listOf(validItem, invalidItem))

        assertEquals(1, result.size)
        assertEquals(validId, result[0].id)
        assertEquals(200.0, result[0].totalPrice)
    }
}
