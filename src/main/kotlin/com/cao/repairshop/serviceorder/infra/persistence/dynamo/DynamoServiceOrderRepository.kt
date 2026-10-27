package com.cao.repairshop.serviceorder.infra.persistence.dynamo

import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.Put
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException
import software.amazon.awssdk.services.dynamodb.model.Update
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

data class FullServiceOrder(
    val metadata: ServiceOrderMetadataDynamoItem?,
    val histories: List<ServiceOrderHistoryDynamoItem>,
    val executions: List<ExecutionDynamoItem>,
    val extraExecutions: List<ExtraExecutionDynamoItem>
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

    private val extraExecutionTable: DynamoDbTable<ExtraExecutionDynamoItem> by lazy {
        enhancedClient.table(tableName, TableSchema.fromBean(ExtraExecutionDynamoItem::class.java))
    }

    /**
     * Executa operação de gravação transacional atômica no DynamoDB (ACID):
     * Persiste ou atualiza Metadata, Históricos, Execuções, Execuções Extras e Deleta Execuções
     * removidas em um único TransactWriteItems. Ou acontecem todas as operações, ou nenhuma!
     */
    fun saveTransactional(
        metadata: ServiceOrderMetadataDynamoItem,
        histories: List<ServiceOrderHistoryDynamoItem> = emptyList(),
        executions: List<ExecutionDynamoItem> = emptyList(),
        extraExecutions: List<ExtraExecutionDynamoItem> = emptyList(),
        executionsToDelete: List<String> = emptyList()
    ) {
        val totalOperations = 1 + histories.size + executions.size + extraExecutions.size + executionsToDelete.size
        logger.info(
            "Iniciando transação atômica no DynamoDB para OS [{}]: {} operações totais (1 metadata, {} históricos, {} execuções, {} extras, {} deleções)",
            metadata.serviceOrderId, totalOperations, histories.size, executions.size, extraExecutions.size, executionsToDelete.size
        )

        try {
            enhancedClient.transactWriteItems { builder ->
                if (metadata.pk.isBlank()) metadata.pk = "ORDER#${metadata.serviceOrderId}"
                if (metadata.sk.isBlank()) metadata.sk = "METADATA"
                builder.addPutItem(metadataTable, metadata)

                histories.forEach { h ->
                    if (h.pk.isBlank()) h.pk = metadata.pk
                    if (h.sk.isBlank()) h.sk = "HIST#${Instant.now().toEpochMilli()}#${h.historyId}"
                    builder.addPutItem(historyTable, h)
                }

                executions.forEach { e ->
                    if (e.pk.isBlank()) e.pk = metadata.pk
                    if (e.sk.isBlank()) e.sk = "EXEC#${Instant.now().toEpochMilli()}#${e.executionId}"
                    builder.addPutItem(executionTable, e)
                }

                extraExecutions.forEach { ex ->
                    if (ex.pk.isBlank()) ex.pk = metadata.pk
                    if (ex.sk.isBlank()) ex.sk = "EXTRA_EXEC#${Instant.now().toEpochMilli()}#${ex.extraExecutionId}"
                    builder.addPutItem(extraExecutionTable, ex)
                }

                executionsToDelete.forEach { skToDelete ->
                    builder.addDeleteItem(executionTable, Key.builder().partitionValue(metadata.pk).sortValue(skToDelete).build())
                }
            }
            logger.info(
                "Transação atômica no DynamoDB concluída com SUCESSO para OS [{}] (todas as {} operações persistidas)",
                metadata.serviceOrderId, totalOperations
            )
        } catch (e: TransactionCanceledException) {
            logger.error(
                "Transação DynamoDB CANCELADA para OS [{}]: Motivos de cancelamento: {}",
                metadata.serviceOrderId, e.cancellationReasons(), e
            )
            throw e
        } catch (e: Exception) {
            logger.error(
                "Falha ao executar transação atômica no DynamoDB para OS [{}]: {}",
                metadata.serviceOrderId, e.message, e
            )
            throw e
        }
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
        val result = runCatching { metadataTable.getItem(key) }.getOrNull()
        if (result != null) {
            logger.debug("Metadata da OS encontrado no DynamoDB: ORDER#{}", orderId)
        } else {
            logger.debug("Metadata da OS não encontrado no DynamoDB: ORDER#{}", orderId)
        }
        return result
    }

    /**
     * Registra histórico de mudança de status (PK=ORDER#id, SK=HIST#timestamp#historyId)
     */
    fun saveHistory(history: ServiceOrderHistoryDynamoItem) {
        if (history.pk.isBlank()) history.pk = "ORDER#${history.historyId}"
        if (history.sk.isBlank()) history.sk = "HIST#${Instant.now().toEpochMilli()}#${history.historyId}"
        historyTable.putItem(history)
        logger.info("Histórico registrado no DynamoDB: PK={}, Status={}", history.pk, history.status)
    }

    /**
     * Registra tarefa de execução técnica com insumos embutidos (PK=ORDER#id, SK=EXEC#timestamp#executionId)
     */
    fun saveExecution(execution: ExecutionDynamoItem) {
        if (execution.pk.isBlank()) execution.pk = "ORDER#${execution.executionId}"
        if (execution.sk.isBlank()) execution.sk = "EXEC#${Instant.now().toEpochMilli()}#${execution.executionId}"
        executionTable.putItem(execution)
        logger.info("Execução registrada no DynamoDB: PK={}, Task={}, Status={}", execution.pk, execution.taskName, execution.status)
    }

    /**
     * Registra item de execuções extras (PK=ORDER#id, SK=EXTRA_EXEC#timestamp#extraExecutionId)
     */
    fun saveExtraExecution(extraItem: ExtraExecutionDynamoItem) {
        if (extraItem.pk.isBlank()) extraItem.pk = "ORDER#${extraItem.serviceOrderId}"
        if (extraItem.sk.isBlank()) extraItem.sk = "EXTRA_EXEC#${Instant.now().toEpochMilli()}#${extraItem.extraExecutionId}"
        extraExecutionTable.putItem(extraItem)
        logger.info("Item de Execução Extra registrado no DynamoDB: PK={}, ExtraId={}, Status={}", extraItem.pk, extraItem.extraExecutionId, extraItem.status)
    }

    /**
     * Recupera a Ordem de Serviço COMPLETA (Metadata + Histórico + Execuções + Execuções Extras) via Single-Table Query
     */
    fun getFullOrder(orderId: UUID): FullServiceOrder {
        val pk = "ORDER#$orderId"
        logger.debug("Consultando Ordem de Serviço completa no DynamoDB: {}", pk)

        val request = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("PK = :pk")
            .expressionAttributeValues(mapOf(":pk" to AttributeValue.builder().s(pk).build()))
            .build()

        val response = runCatching { dynamoDbClient.query(request) }.getOrNull()
        if (response == null || response.items().isEmpty()) {
            logger.debug("Nenhum item retornado para {}", pk)
            return FullServiceOrder(null, emptyList(), emptyList(), emptyList())
        }

        var metadata: ServiceOrderMetadataDynamoItem? = null
        val histories = mutableListOf<ServiceOrderHistoryDynamoItem>()
        val executions = mutableListOf<ExecutionDynamoItem>()
        val extraExecutions = mutableListOf<ExtraExecutionDynamoItem>()

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
                        totalServiceOrderPrice = item["total_service_order_price"]?.n()?.toDouble()
                            ?: item["total_price"]?.n()?.toDouble() ?: 0.0,
                        enterTime = item["enter_time"]?.s() ?: "",
                        endTime = item["end_time"]?.s(),
                        createDate = item["create_date"]?.s() ?: "",
                        updateDate = item["update_date"]?.s() ?: "",
                        status = item["status"]?.s() ?: "IN_DIAGNOSIS",
                        validDate = item["valid_date"]?.s(),
                        paymentStatus = item["payment_status"]?.s() ?: "PENDING",
                        paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
                        hasExtraServices = item["has_extra_services"]?.bool() ?: false,
                        extraServicesAccepted = item["extra_services_accepted"]?.bool(),
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
                    val insumesList = parseInsumes(item["insumes"])
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
                sk.startsWith("EXTRA_EXEC#") -> {
                    val rawExecs = item["executions"]?.l() ?: emptyList()
                    val extraExecList = rawExecs.mapNotNull { ev ->
                        val m = ev.m() ?: return@mapNotNull null
                        ExecutionDynamoItem(
                            pk = pk,
                            sk = m["SK"]?.s() ?: "",
                            entityType = m["entityType"]?.s() ?: "EXECUTION",
                            executionId = m["execution_id"]?.s() ?: UUID.randomUUID().toString(),
                            taskName = m["task_name"]?.s() ?: "",
                            executionPrice = m["execution_price"]?.n()?.toDouble() ?: 0.0,
                            status = m["status"]?.s() ?: "INITIATED",
                            createDate = m["create_date"]?.s() ?: "",
                            updateDate = m["update_date"]?.s() ?: "",
                            basicDescription = m["basic_description"]?.s() ?: "",
                            fullDescription = m["full_description"]?.s(),
                            estimatedTime = m["estimated_time"]?.n()?.toDouble(),
                            intervalTime = m["interval_time"]?.n()?.toLong(),
                            insumes = parseInsumes(m["insumes"])
                        )
                    }

                    extraExecutions.add(
                        ExtraExecutionDynamoItem(
                            pk = pk,
                            sk = sk,
                            entityType = item["entityType"]?.s() ?: "EXTRA_EXECUTION",
                            extraExecutionId = item["extra_execution_id"]?.s() ?: UUID.randomUUID().toString(),
                            serviceOrderId = item["service_order_id"]?.s() ?: orderId.toString(),
                            hasExtraServices = item["has_extra_services"]?.bool() ?: true,
                            extraServicesAccepted = item["extra_services_accepted"]?.bool(),
                            status = item["status"]?.s() ?: "WAITING_APPROVAL",
                            totalExtraPrice = item["total_extra_price"]?.n()?.toDouble() ?: 0.0,
                            notes = item["notes"]?.s(),
                            createDate = item["create_date"]?.s() ?: "",
                            updateDate = item["update_date"]?.s() ?: "",
                            executions = extraExecList
                        )
                    )
                }
            }
        }

        logger.info(
            "Consulta Single-Table concluída para OS [{}]: metadata={}, históricos={}, execuções={}, extra_executions={}",
            orderId, metadata != null, histories.size, executions.size, extraExecutions.size
        )

        return FullServiceOrder(metadata, histories, executions, extraExecutions)
    }

    private fun parseInsumes(insumesAttribute: AttributeValue?): List<InsumeDynamoItem> {
        return insumesAttribute?.l()?.mapNotNull { iv ->
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
    }

    /**
     * Busca ordens pendentes de envio ao serviço de pagamento através do GSI pending-dispatch-index
     */
    fun findPendingNotification(): List<ServiceOrderDynamoItem> {
        logger.debug("Buscando ordens pendentes de notificação via GSI pending-dispatch-index")
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
            val results = response.items().mapNotNull { item ->
                val idStr = item["id"]?.s() ?: item["service_order_id"]?.s() ?: return@mapNotNull null
                ServiceOrderDynamoItem(
                    id = UUID.fromString(idStr),
                    customerId = UUID.fromString(item["customer_id"]!!.s()),
                    vehicleId = UUID.fromString(item["vehicle_id"]!!.s()),
                    status = item["status"]!!.s(),
                    totalPrice = item["total_service_order_price"]?.n()?.toDouble()
                        ?: item["total_price"]?.n()?.toDouble() ?: 0.0,
                    description = item["description"]?.s(),
                    paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
                    updateDate = item["update_date"]?.s() ?: Instant.now().toString(),
                    originTraceId = item["origin_trace_id"]?.s()
                )
            }
            logger.info("Consulta ao GSI pending-dispatch-index retornou {} ordens", results.size)
            results
        }.getOrElse { e ->
            logger.error("Erro ao consultar GSI pending-dispatch-index: {}", e.message, e)
            emptyList()
        }
    }

    /**
     * Atualiza o status e/ou payment_dispatch_status no DynamoDB de forma TRANSACIONAL ATÔMICA:
     * O update em METADATA e o insert em HIST# são realizados juntos em um TransactWriteItems.
     * Ou acontecem ambos, ou nenhum!
     */
    fun updateStatus(
        id: UUID,
        newStatus: String,
        newDispatchStatus: String? = null,
        paymentStatus: String? = null,
        hasExtraServices: Boolean? = null,
        extraServicesAccepted: Boolean? = null,
        totalPrice: Double? = null,
        historyDescription: String? = null
    ) {
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

        if (hasExtraServices != null) {
            updateExpr.append(", #hes = :hes")
            names["#hes"] = "has_extra_services"
            values[":hes"] = AttributeValue.builder().bool(hasExtraServices).build()
        }

        if (extraServicesAccepted != null) {
            updateExpr.append(", #esa = :esa")
            names["#esa"] = "extra_services_accepted"
            values[":esa"] = AttributeValue.builder().bool(extraServicesAccepted).build()
        }

        if (totalPrice != null) {
            updateExpr.append(", #top = :top")
            names["#top"] = "total_service_order_price"
            values[":top"] = AttributeValue.builder().n(totalPrice.toString()).build()
        }

        val histTimestamp = Instant.now().toEpochMilli()
        val histId = UUID.randomUUID().toString()
        val historyValues = mutableMapOf<String, AttributeValue>(
            "PK" to AttributeValue.builder().s("ORDER#$id").build(),
            "SK" to AttributeValue.builder().s("HIST#$histTimestamp#$histId").build(),
            "entityType" to AttributeValue.builder().s("SERVICE_ORDER_HISTORY").build(),
            "history_id" to AttributeValue.builder().s(histId).build(),
            "status" to AttributeValue.builder().s(newStatus).build(),
            "register_time" to AttributeValue.builder().s(now).build(),
            "description" to AttributeValue.builder().s(historyDescription ?: "Status transitioned to $newStatus").build()
        )
        paymentStatus?.let { historyValues["payment_status"] = AttributeValue.builder().s(it).build() }

        val transactRequest = TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder()
                    .update(
                        Update.builder()
                            .tableName(tableName)
                            .key(key)
                            .updateExpression(updateExpr.toString())
                            .expressionAttributeNames(names)
                            .expressionAttributeValues(values)
                            .build()
                    )
                    .build(),
                TransactWriteItem.builder()
                    .put(
                        Put.builder()
                            .tableName(tableName)
                            .item(historyValues)
                            .build()
                    )
                    .build()
            )
            .build()

        logger.info(
            "Iniciando transação atômica DynamoDB para updateStatus: OS [ORDER#{}] -> Novo status={}, Dispatch={}, Payment={}",
            id, newStatus, newDispatchStatus, paymentStatus
        )

        try {
            dynamoDbClient.transactWriteItems(transactRequest)
            logger.info("Transação atômica DynamoDB de updateStatus concluída com SUCESSO para OS [ORDER#{}] -> {}", id, newStatus)
        } catch (e: TransactionCanceledException) {
            logger.error("Transação de updateStatus CANCELADA para OS [ORDER#{}]: Motivos: {}", id, e.cancellationReasons(), e)
            throw e
        } catch (e: Exception) {
            logger.error("Falha ao executar transação de updateStatus para OS [ORDER#{}]: {}", id, e.message, e)
            throw e
        }
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
            totalPrice = item["total_price"]?.n()?.toDouble()
                ?: item["total_service_order_price"]?.n()?.toDouble() ?: 0.0,
            description = item["description"]?.s(),
            paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
            updateDate = item["update_date"]?.s() ?: "",
            originTraceId = item["origin_trace_id"]?.s()
        )
    }
}
