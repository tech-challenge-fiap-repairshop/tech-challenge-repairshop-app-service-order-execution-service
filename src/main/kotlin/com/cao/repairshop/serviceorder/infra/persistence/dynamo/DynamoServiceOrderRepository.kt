package com.cao.repairshop.serviceorder.infra.persistence.dynamo

import com.cao.repairshop.serviceorder.infra.persistence.dynamo.builder.DynamoTransactionBuilder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.mapper.DynamoItemParser
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.*
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
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException
import java.time.Instant
import java.util.UUID

@Repository
class DynamoServiceOrderRepository(
    private val dynamoDbClient: DynamoDbClient,
    private val enhancedClient: DynamoDbEnhancedClient,
    @Value("\${dynamodb.table.service-order:tb-service_order-dev}")
    private val tableName: String,
    private val itemParser: DynamoItemParser = DynamoItemParser()
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

        val fullOrder = itemParser.parseFullOrder(orderId, response.items())
        logger.info(
            "Consulta Single-Table concluída para OS [{}]: metadata={}, históricos={}, execuções={}, extra_executions={}",
            orderId, fullOrder.metadata != null, fullOrder.histories.size, fullOrder.executions.size, fullOrder.extraExecutions.size
        )
        return fullOrder
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
            val results = itemParser.parsePendingNotifications(response.items())
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
        val transactRequest = DynamoTransactionBuilder.buildUpdateStatusRequest(
            tableName = tableName,
            id = id,
            newStatus = newStatus,
            newDispatchStatus = newDispatchStatus,
            paymentStatus = paymentStatus,
            hasExtraServices = hasExtraServices,
            extraServicesAccepted = extraServicesAccepted,
            totalPrice = totalPrice,
            historyDescription = historyDescription
        )

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
        val request = DynamoTransactionBuilder.buildPutItemRequest(tableName, order)
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

        return itemParser.parseServiceOrderItem(id, response.item())
    }
}
