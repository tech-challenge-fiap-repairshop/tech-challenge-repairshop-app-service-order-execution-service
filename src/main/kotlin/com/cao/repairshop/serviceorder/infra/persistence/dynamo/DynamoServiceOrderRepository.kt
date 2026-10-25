package com.cao.repairshop.serviceorder.infra.persistence.dynamo

import com.cao.repairshop.core.health.SqsListenerHealthIndicator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import java.time.Instant
import java.util.UUID

data class ServiceOrderDynamoItem(
    val id: UUID,
    val customerId: UUID,
    val vehicleId: UUID,
    val status: String,
    val totalPrice: Double = 0.0,
    val description: String? = null,
    val paymentDispatchStatus: String = "PENDING",
    val updateDate: String = Instant.now().toString(),
    val originTraceId: String? = null
)

@Repository
class DynamoServiceOrderRepository(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${dynamodb.table.service-order:tb-service_order-dev}")
    private val tableName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun save(order: ServiceOrderDynamoItem) {
        val item = mutableMapOf<String, AttributeValue>(
            "PK" to AttributeValue.builder().s("ORDER#${order.id}").build(),
            "SK" to AttributeValue.builder().s("METADATA").build(),
            "id" to AttributeValue.builder().s(order.id.toString()).build(),
            "customer_id" to AttributeValue.builder().s(order.customerId.toString()).build(),
            "vehicle_id" to AttributeValue.builder().s(order.vehicleId.toString()).build(),
            "status" to AttributeValue.builder().s(order.status).build(),
            "total_price" to AttributeValue.builder().n(order.totalPrice.toString()).build(),
            "payment_dispatch_status" to AttributeValue.builder().s(order.paymentDispatchStatus).build(),
            "update_date" to AttributeValue.builder().s(order.updateDate).build()
        )

        order.description?.let { item["description"] = AttributeValue.builder().s(it).build() }
        order.originTraceId?.let { item["origin_trace_id"] = AttributeValue.builder().s(it).build() }

        val request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build()

        dynamoDbClient.putItem(request)
        logger.info("Ordem de Serviço salva no DynamoDB: PK=ORDER#{}, Status={}", order.id, order.status)
    }

    fun findById(id: UUID): ServiceOrderDynamoItem? {
        val key = mapOf(
            "PK" to AttributeValue.builder().s("ORDER#$id").build(),
            "SK" to AttributeValue.builder().s("METADATA").build()
        )

        val request = GetItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .build()

        val response = dynamoDbClient.getItem(request)
        if (!response.hasItem() || response.item().isEmpty()) return null

        val item = response.item()
        return ServiceOrderDynamoItem(
            id = UUID.fromString(item["id"]?.s() ?: id.toString()),
            customerId = UUID.fromString(item["customer_id"]!!.s()),
            vehicleId = UUID.fromString(item["vehicle_id"]!!.s()),
            status = item["status"]!!.s(),
            totalPrice = item["total_price"]?.n()?.toDouble() ?: 0.0,
            description = item["description"]?.s(),
            paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
            updateDate = item["update_date"]?.s() ?: Instant.now().toString(),
            originTraceId = item["origin_trace_id"]?.s()
        )
    }

    fun updateStatus(id: UUID, newStatus: String, newDispatchStatus: String? = null) {
        val key = mapOf(
            "PK" to AttributeValue.builder().s("ORDER#$id").build(),
            "SK" to AttributeValue.builder().s("METADATA").build()
        )

        val now = Instant.now().toString()
        val updateExpr = StringBuilder("SET #status = :newStatus, #update_date = :now")
        val names = mutableMapOf("#status" to "status", "#update_date" to "update_date")
        val values = mutableMapOf<String, AttributeValue>(
            ":newStatus" to AttributeValue.builder().s(newStatus).build(),
            ":now" to AttributeValue.builder().s(now).build()
        )

        if (newDispatchStatus != null) {
            updateExpr.append(", #pds = :pds")
            names["#pds"] = "payment_dispatch_status"
            values[":pds"] = AttributeValue.builder().s(newDispatchStatus).build()
        }

        val request = UpdateItemRequest.builder()
            .tableName(tableName)
            .key(key)
            .updateExpression(updateExpr.toString())
            .expressionAttributeNames(names)
            .expressionAttributeValues(values)
            .build()

        dynamoDbClient.updateItem(request)
        logger.info("Status da OS atualizado no DynamoDB: ORDER#{} -> {}", id, newStatus)
    }

    fun findPendingNotification(): List<ServiceOrderDynamoItem> {
        val request = QueryRequest.builder()
            .tableName(tableName)
            .indexName("pending-dispatch-index")
            .keyConditionExpression("payment_dispatch_status = :pds")
            .expressionAttributeValues(mapOf(
                ":pds" to AttributeValue.builder().s("PENDING").build()
            ))
            .limit(25)
            .build()

        return runCatching {
            val response = dynamoDbClient.query(request)
            response.items().mapNotNull { item ->
                val idStr = item["id"]?.s() ?: return@mapNotNull null
                ServiceOrderDynamoItem(
                    id = UUID.fromString(idStr),
                    customerId = UUID.fromString(item["customer_id"]!!.s()),
                    vehicleId = UUID.fromString(item["vehicle_id"]!!.s()),
                    status = item["status"]!!.s(),
                    totalPrice = item["total_price"]?.n()?.toDouble() ?: 0.0,
                    description = item["description"]?.s(),
                    paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
                    updateDate = item["update_date"]?.s() ?: Instant.now().toString(),
                    originTraceId = item["origin_trace_id"]?.s()
                )
            }
        }.getOrDefault(emptyList())
    }
}
