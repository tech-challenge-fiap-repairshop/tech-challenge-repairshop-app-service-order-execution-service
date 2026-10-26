package com.cao.repairshop.serviceorder.infra.persistence.dynamo

import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.InsumeDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderHistoryDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderMetadataDynamoItem
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import java.time.Instant
import java.util.UUID

// Compatibilidade com DTO existente
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
    private val enhancedClient: DynamoDbEnhancedClient,
    @Value("\${dynamodb.table.service-order:tb-service_order-dev}")
    private val tableName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val metadataTable: DynamoDbTable<ServiceOrderMetadataDynamoItem> by lazy {
        enhancedClient.table(tableName, TableSchema.fromBean(ServiceOrderMetadataDynamoItem::class.java))
    }

    private val historyTable: DynamoDbTable<ServiceOrderHistoryDynamoItem> by lazy {
        enhancedClient.table(tableName, TableSchema.fromBean(ServiceOrderHistoryDynamoItem::class.java))
    }

    private val executionTable: DynamoDbTable<ExecutionDynamoItem> by lazy {
        enhancedClient.table(tableName, TableSchema.fromBean(ExecutionDynamoItem::class.java))
    }

    /**
     * Salva ou atualiza os metadados da Ordem de Serviço (PK=ORDER#id, SK=METADATA)
     */
    fun saveMetadata(metadata: ServiceOrderMetadataDynamoItem) {
        if (metadata.pk.isBlank()) metadata.pk = "ORDER#${metadata.serviceOrderId}"
        if (metadata.sk.isBlank()) metadata.sk = "METADATA"
        metadataTable.putItem(metadata)
        logger.info("Metadata da OS salvo no DynamoDB: PK={}, Status={}", metadata.pk, metadata.status)
    }

    /**
     * Busca os metadados da OS pela chave primária
     */
    fun findMetadataById(orderId: UUID): ServiceOrderMetadataDynamoItem? {
        val key = Key.builder().partitionValue("ORDER#$orderId").sortValue("METADATA").build()
        return runCatching { metadataTable.getItem(key) }.getOrNull()
    }

    /**
     * Registra histórico de mudança de status (PK=ORDER#id, SK=HIST#timestamp#historyId)
     */
    fun saveHistory(history: ServiceOrderHistoryDynamoItem) {
        historyTable.putItem(history)
        logger.info("Histórico registrado no DynamoDB: PK={}, Status={}", history.pk, history.status)
    }

    /**
     * Registra tarefa de execução técnica com insumos embutidos (PK=ORDER#id, SK=EXEC#timestamp#executionId)
     */
    fun saveExecution(execution: ExecutionDynamoItem) {
        executionTable.putItem(execution)
        logger.info("Execução registrada no DynamoDB: PK={}, Task={}, Status={}", execution.pk, execution.taskName, execution.status)
    }

    /**
     * Recupera a Ordem de Serviço COMPLETA (Metadata + Histórico + Execuções) via Single-Table Query
     * Uma única chamada na partição PK = ORDER#<id> recupera tudo!
     */
    fun getFullOrder(orderId: UUID): Triple<ServiceOrderMetadataDynamoItem?, List<ServiceOrderHistoryDynamoItem>, List<ExecutionDynamoItem>> {
        val pk = "ORDER#$orderId"
        val request = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("PK = :pk")
            .expressionAttributeValues(mapOf(":pk" to AttributeValue.builder().s(pk).build()))
            .build()

        val response = runCatching { dynamoDbClient.query(request) }.getOrNull() ?: return Triple(null, emptyList(), emptyList())

        var metadata: ServiceOrderMetadataDynamoItem? = null
        val histories = mutableListOf<ServiceOrderHistoryDynamoItem>()
        val executions = mutableListOf<ExecutionDynamoItem>()

        for (item in response.items()) {
            val sk = item["SK"]?.s() ?: ""
            when {
                sk == "METADATA" -> {
                    metadata = ServiceOrderMetadataDynamoItem(
                        pk = pk,
                        sk = sk,
                        entityType = item["entityType"]?.s() ?: "SERVICE_ORDER_METADATA",
                        serviceOrderId = item["service_order_id"]?.s() ?: orderId.toString(),
                        customerId = item["customer_id"]?.s() ?: "",
                        vehicleId = item["vehicle_id"]?.s() ?: "",
                        totalServiceOrderPrice = item["total_service_order_price"]?.n()?.toDouble() ?: item["total_price"]?.n()?.toDouble() ?: 0.0,
                        enterTime = item["enter_time"]?.s() ?: "",
                        endTime = item["end_time"]?.s(),
                        createDate = item["create_date"]?.s() ?: "",
                        updateDate = item["update_date"]?.s() ?: "",
                        status = item["status"]?.s() ?: "RECEIVED",
                        validDate = item["valid_date"]?.s(),
                        paymentStatus = item["payment_status"]?.s() ?: "PENDING",
                        paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
                        description = item["description"]?.s(),
                        originTraceId = item["origin_trace_id"]?.s()
                    )
                }
                sk.startsWith("HIST#") -> {
                    histories.add(
                        ServiceOrderHistoryDynamoItem(
                            pk = pk,
                            sk = sk,
                            entityType = item["entityType"]?.s() ?: "SERVICE_ORDER_HISTORY",
                            historyId = item["history_id"]?.s() ?: UUID.randomUUID().toString(),
                            status = item["status"]?.s() ?: "",
                            registerTime = item["register_time"]?.s() ?: "",
                            intervalTime = item["interval_time"]?.n()?.toLong(),
                            description = item["description"]?.s(),
                            paymentStatus = item["payment_status"]?.s()
                        )
                    )
                }
                sk.startsWith("EXEC#") -> {
                    val insumesList = item["insumes"]?.l()?.mapNotNull { iv ->
                        val m = iv.m() ?: return@mapNotNull null
                        InsumeDynamoItem(
                            idInsume = m["id_insume"]?.s() ?: "",
                            name = m["name"]?.s() ?: "",
                            brand = m["brand"]?.s() ?: "",
                            quantity = m["quantity"]?.n()?.toDouble() ?: 1.0,
                            insumePrice = m["insume_price"]?.n()?.toDouble() ?: 0.0,
                            skuId = m["sku_id"]?.s() ?: "",
                            unityInsumePrice = m["unity_insume_price"]?.n()?.toDouble() ?: 0.0
                        )
                    } ?: emptyList()

                    executions.add(
                        ExecutionDynamoItem(
                            pk = pk,
                            sk = sk,
                            entityType = item["entityType"]?.s() ?: "EXECUTION",
                            executionId = item["execution_id"]?.s() ?: UUID.randomUUID().toString(),
                            taskName = item["task_name"]?.s() ?: "",
                            executionPrice = item["execution_price"]?.n()?.toDouble() ?: 0.0,
                            status = item["status"]?.s() ?: "INITIATED",
                            createDate = item["create_date"]?.s() ?: "",
                            updateDate = item["update_date"]?.s() ?: "",
                            basicDescription = item["basic_description"]?.s() ?: "",
                            fullDescription = item["full_description"]?.s(),
                            estimatedTime = item["estimated_time"]?.n()?.toDouble(),
                            intervalTime = item["interval_time"]?.n()?.toLong(),
                            insumes = insumesList
                        )
                    )
                }
            }
        }

        return Triple(metadata, histories, executions)
    }

    /**
     * Busca ordens pendentes de envio ao serviço de pagamento através do GSI pending-dispatch-index
     */
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
                val idStr = item["id"]?.s() ?: item["service_order_id"]?.s() ?: return@mapNotNull null
                ServiceOrderDynamoItem(
                    id = UUID.fromString(idStr),
                    customerId = UUID.fromString(item["customer_id"]!!.s()),
                    vehicleId = UUID.fromString(item["vehicle_id"]!!.s()),
                    status = item["status"]!!.s(),
                    totalPrice = item["total_service_order_price"]?.n()?.toDouble() ?: item["total_price"]?.n()?.toDouble() ?: 0.0,
                    description = item["description"]?.s(),
                    paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
                    updateDate = item["update_date"]?.s() ?: Instant.now().toString(),
                    originTraceId = item["origin_trace_id"]?.s()
                )
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Atualiza o status e/ou payment_dispatch_status no DynamoDB
     */
    fun updateStatus(id: UUID, newStatus: String, newDispatchStatus: String? = null, paymentStatus: String? = null) {
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

        if (paymentStatus != null) {
            updateExpr.append(", #ps = :ps")
            names["#ps"] = "payment_status"
            values[":ps"] = AttributeValue.builder().s(paymentStatus).build()
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

    // Métodos legados/compatibilidade com ServiceOrderDynamoItem
    fun save(order: ServiceOrderDynamoItem) {
        val item = mutableMapOf<String, AttributeValue>(
            "PK" to AttributeValue.builder().s("ORDER#${order.id}").build(),
            "SK" to AttributeValue.builder().s("METADATA").build(),
            "id" to AttributeValue.builder().s(order.id.toString()).build(),
            "service_order_id" to AttributeValue.builder().s(order.id.toString()).build(),
            "customer_id" to AttributeValue.builder().s(order.customerId.toString()).build(),
            "vehicle_id" to AttributeValue.builder().s(order.vehicleId.toString()).build(),
            "status" to AttributeValue.builder().s(order.status).build(),
            "total_price" to AttributeValue.builder().n(order.totalPrice.toString()).build(),
            "total_service_order_price" to AttributeValue.builder().n(order.totalPrice.toString()).build(),
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
        logger.info("ServiceOrder salvo no DynamoDB: ORDER#{}", order.id)
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

        val response = runCatching { dynamoDbClient.getItem(request) }.getOrNull() ?: return null
        if (!response.hasItem() || response.item().isEmpty()) return null

        val item = response.item()
        return ServiceOrderDynamoItem(
            id = id,
            customerId = UUID.fromString(item["customer_id"]?.s() ?: UUID.randomUUID().toString()),
            vehicleId = UUID.fromString(item["vehicle_id"]?.s() ?: UUID.randomUUID().toString()),
            status = item["status"]?.s() ?: "",
            totalPrice = item["total_price"]?.n()?.toDouble() ?: item["total_service_order_price"]?.n()?.toDouble() ?: 0.0,
            description = item["description"]?.s(),
            paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
            updateDate = item["update_date"]?.s() ?: "",
            originTraceId = item["origin_trace_id"]?.s()
        )
    }
}
