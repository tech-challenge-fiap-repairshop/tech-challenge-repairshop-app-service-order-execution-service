package com.cao.repairshop.serviceorder.infra.gateways

import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.domain.entities.ExecutionHistory
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrderHistory
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.InsumeDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderHistoryDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderMetadataDynamoItem
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
@Primary
class DynamoServiceOrderGatewayImpl(
    private val dynamoRepository: DynamoServiceOrderRepository
) : ServiceOrderGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun save(order: ServiceOrder): ServiceOrder {
        logger.info("Persistindo Ordem de Serviço [ORDER#{}] atomicamente no DynamoDB (Status={})", order.id, order.status)
        val pk = "ORDER#${order.id}"

        val meta = buildMetadataItem(order, pk)
        val histories = buildHistoryItems(order, pk)
        val executions = buildExecutionItems(order, pk)

        // Detecta execuções que existiam previamente e foram removidas do aggregate
        val existingFull = dynamoRepository.getFullOrder(order.id)
        val currentExecIds = order.executions.map { it.id.toString() }.toSet()
        val executionsToDelete = existingFull.executions
            .filter { it.executionId !in currentExecIds }
            .map { it.sk }

        if (executionsToDelete.isNotEmpty()) {
            logger.info("Identificadas {} execuções para deleção atômica na OS [{}]", executionsToDelete.size, order.id)
        }

        dynamoRepository.saveTransactional(
            metadata = meta,
            histories = histories,
            executions = executions,
            executionsToDelete = executionsToDelete
        )

        return order
    }

    override fun saveWithExtraExecution(
        order: ServiceOrder,
        extraItem: ExtraExecutionDynamoItem
    ): ServiceOrder {
        logger.info(
            "Persistindo OS [ORDER#{}] com Lote de Execuções Extras [{}] atomicamente",
            order.id, extraItem.extraExecutionId
        )
        val pk = "ORDER#${order.id}"
        val meta = buildMetadataItem(order, pk)
        val histories = buildHistoryItems(order, pk)
        val executions = buildExecutionItems(order, pk)

        dynamoRepository.saveTransactional(
            metadata = meta,
            histories = histories,
            executions = executions,
            extraExecutions = listOf(extraItem)
        )
        return order
    }

    override fun saveWithExtraExecutions(
        order: ServiceOrder,
        extraItems: List<ExtraExecutionDynamoItem>
    ): ServiceOrder {
        logger.info(
            "Persistindo OS [ORDER#{}] com {} Lotes de Execuções Extras atomicamente",
            order.id, extraItems.size
        )
        val pk = "ORDER#${order.id}"
        val meta = buildMetadataItem(order, pk)
        val histories = buildHistoryItems(order, pk)
        val executions = buildExecutionItems(order, pk)

        dynamoRepository.saveTransactional(
            metadata = meta,
            histories = histories,
            executions = executions,
            extraExecutions = extraItems
        )
        return order
    }

    override fun findDetailedById(id: UUID): ServiceOrder? {
        logger.debug("Buscando detalhes da Ordem de Serviço [ORDER#{}] via Single-Table", id)
        val fullOrder = dynamoRepository.getFullOrder(id)
        val metadata = fullOrder.metadata ?: run {
            logger.debug("Ordem de Serviço [ORDER#{}] não encontrada no DynamoDB", id)
            return null
        }

        val order = ServiceOrder(
            id = UUID.fromString(metadata.serviceOrderId),
            customerId = UUID.fromString(metadata.customerId),
            vehicleId = UUID.fromString(metadata.vehicleId),
            status = runCatching { ServiceOrderStatus.valueOf(metadata.status) }.getOrDefault(ServiceOrderStatus.IN_DIAGNOSIS),
            totalPrice = BigDecimal.valueOf(metadata.totalServiceOrderPrice),
            hasExtraServices = metadata.hasExtraServices,
            extraServicesAccepted = metadata.extraServicesAccepted
        )

        // Reconstrói execuções técnicas e seus insumos embutidos
        fullOrder.executions.forEach { ei ->
            val exec = Execution(
                id = UUID.fromString(ei.executionId),
                serviceOrderId = order.id,
                basicDescription = ei.basicDescription.ifBlank { ei.taskName },
                fullDescription = ei.fullDescription,
                price = BigDecimal.valueOf(ei.executionPrice),
                estimatedTime = ei.estimatedTime?.let { BigDecimal.valueOf(it) },
                status = runCatching { ExecutionStatus.valueOf(ei.status) }.getOrDefault(ExecutionStatus.INITIATED)
            )

            ei.insumes.forEach { ins ->
                val insumeEntity = Insume(
                    id = UUID.fromString(ins.idInsume.ifBlank { UUID.randomUUID().toString() }),
                    name = ins.name,
                    brand = ins.brand.ifBlank { null },
                    skuId = ins.skuId,
                    quantity = ins.quantity.toInt(),
                    unityPrice = BigDecimal.valueOf(if (ins.unityInsumePrice > 0.0) ins.unityInsumePrice else ins.insumePrice)
                )
                exec.addInsume(insumeEntity, ins.quantity.toInt())
            }

            order.executions.add(exec)
        }

        // Reconstrói histórico
        fullOrder.histories.forEach { h ->
            val status = runCatching { ServiceOrderStatus.valueOf(h.status) }.getOrDefault(ServiceOrderStatus.IN_DIAGNOSIS)
            order.histories.add(
                ServiceOrderHistory(
                    id = UUID.fromString(h.historyId),
                    serviceOrderId = order.id,
                    status = status,
                    description = h.description,
                    registerTime = LocalDateTime.now(),
                    intervalTime = h.intervalTime
                )
            )
        }

        logger.info(
            "Ordem de Serviço [ORDER#{}] carregada com sucesso: Status={}, Execuções={}, Históricos={}",
            id, order.status, order.executions.size, order.histories.size
        )

        return order
    }

    override fun findAll(
        customerId: UUID?,
        vehicleId: UUID?,
        status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrder> {
        logger.debug("Listando Ordens de Serviço: customerId={}, vehicleId={}, status={}", customerId, vehicleId, status)
        val items = dynamoRepository.findPendingNotification().mapNotNull { findDetailedById(it.id) }
        return PageImpl(items, pageable, items.size.toLong())
    }

    override fun saveExtraExecution(extraItem: ExtraExecutionDynamoItem) {
        logger.info("Gravando item de execução extra avulso: ExtraId={}", extraItem.extraExecutionId)
        dynamoRepository.saveExtraExecution(extraItem)
    }

    override fun getFullOrder(orderId: UUID): FullServiceOrder {
        return dynamoRepository.getFullOrder(orderId)
    }

    override fun updateStatus(
        id: UUID,
        newStatus: String,
        newDispatchStatus: String?,
        paymentStatus: String?,
        hasExtraServices: Boolean?,
        extraServicesAccepted: Boolean?,
        totalPrice: Double?
    ) {
        logger.info("Atualizando status da OS [ORDER#{}] -> {}", id, newStatus)
        dynamoRepository.updateStatus(
            id = id,
            newStatus = newStatus,
            newDispatchStatus = newDispatchStatus,
            paymentStatus = paymentStatus,
            hasExtraServices = hasExtraServices,
            extraServicesAccepted = extraServicesAccepted,
            totalPrice = totalPrice
        )
    }

    override fun existsByCustomerId(customerId: UUID): Boolean = true
    override fun existsByVehicleId(vehicleId: UUID): Boolean = true
    override fun getAverageExecutionTimeMinutes(): Double? = 45.0

    private fun buildMetadataItem(order: ServiceOrder, pk: String): ServiceOrderMetadataDynamoItem {
        return ServiceOrderMetadataDynamoItem(
            pk = pk,
            sk = "METADATA",
            entityType = "SERVICE_ORDER_METADATA",
            serviceOrderId = order.id.toString(),
            customerId = order.customerId.toString(),
            vehicleId = order.vehicleId.toString(),
            totalServiceOrderPrice = order.totalPrice.toDouble(),
            enterTime = order.enterTime?.toInstant(ZoneOffset.UTC)?.toString() ?: Instant.now().toString(),
            endTime = order.endTime?.toInstant(ZoneOffset.UTC)?.toString(),
            createDate = order.created?.toInstant(ZoneOffset.UTC)?.toString() ?: Instant.now().toString(),
            updateDate = Instant.now().toString(),
            status = order.status.name,
            validDate = order.validDate?.toString(),
            paymentStatus = if (order.status == ServiceOrderStatus.PAID) "PAID" else "PENDING",
            paymentDispatchStatus = if (order.status == ServiceOrderStatus.FINALIZED) "PENDING" else "DISPATCHED",
            hasExtraServices = order.hasExtraServices,
            extraServicesAccepted = order.extraServicesAccepted
        )
    }

    private fun buildHistoryItems(order: ServiceOrder, pk: String): List<ServiceOrderHistoryDynamoItem> {
        return order.histories.map { h ->
            val timestamp = h.registerTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            ServiceOrderHistoryDynamoItem(
                pk = pk,
                sk = "HIST#$timestamp#${h.id}",
                entityType = "SERVICE_ORDER_HISTORY",
                historyId = h.id.toString(),
                status = h.status.name,
                registerTime = h.registerTime.toInstant(ZoneOffset.UTC).toString(),
                intervalTime = h.intervalTime,
                description = h.description,
                paymentStatus = if (h.status == ServiceOrderStatus.PAID) "PAID" else null
            )
        }
    }

    private fun buildExecutionItems(order: ServiceOrder, pk: String): List<ExecutionDynamoItem> {
        return order.executions.map { exec ->
            val timestamp = (exec.created ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC).toEpochMilli()
            val insumesItems = exec.insumes.map { ei ->
                InsumeDynamoItem(
                    idInsume = ei.insume.id.toString(),
                    name = ei.insume.name,
                    brand = ei.insume.brand ?: "GENERIC",
                    quantity = ei.quantity.toDouble(),
                    insumePrice = ei.insume.totalPrice.toDouble(),
                    skuId = ei.insume.skuId ?: "SKU-${ei.insume.id.toString().take(8)}",
                    unityInsumePrice = ei.insume.unityPrice.toDouble()
                )
            }

            ExecutionDynamoItem(
                pk = pk,
                sk = "EXEC#$timestamp#${exec.id}",
                entityType = "EXECUTION",
                executionId = exec.id.toString(),
                taskName = exec.basicDescription,
                executionPrice = exec.price.toDouble(),
                status = exec.status.name,
                createDate = (exec.created ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC).toString(),
                updateDate = Instant.now().toString(),
                basicDescription = exec.basicDescription,
                fullDescription = exec.fullDescription,
                estimatedTime = exec.estimatedTime?.toDouble(),
                insumes = insumesItems
            )
        }
    }
}
