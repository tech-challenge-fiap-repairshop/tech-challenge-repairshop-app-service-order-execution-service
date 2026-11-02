package com.cao.repairshop.serviceorder.infra.gateways

import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.gateways.mapper.ServiceOrderDynamoMapper
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Primary
class DynamoServiceOrderGatewayImpl(
    private val dynamoRepository: DynamoServiceOrderRepository,
    private val mapper: ServiceOrderDynamoMapper = ServiceOrderDynamoMapper()
) : ServiceOrderGateway {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun save(order: ServiceOrder): ServiceOrder {
        logger.info("Persistindo Ordem de Serviço [ORDER#{}] atomicamente no DynamoDB (Status={})", order.id, order.status)
        val pk = "ORDER#${order.id}"

        val meta = mapper.buildMetadataItem(order, pk)
        val histories = mapper.buildHistoryItems(order, pk)
        val executions = mapper.buildExecutionItems(order, pk)

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
        val meta = mapper.buildMetadataItem(order, pk)
        val histories = mapper.buildHistoryItems(order, pk)
        val executions = mapper.buildExecutionItems(order, pk)

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
        val meta = mapper.buildMetadataItem(order, pk)
        val histories = mapper.buildHistoryItems(order, pk)
        val executions = mapper.buildExecutionItems(order, pk)

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
        val order = mapper.toDomainOrder(fullOrder)
        if (order != null) {
            logger.info(
                "Ordem de Serviço [ORDER#{}] carregada com sucesso: Status={}, Execuções={}, Históricos={}",
                id, order.status, order.executions.size, order.histories.size
            )
        } else {
            logger.debug("Ordem de Serviço [ORDER#{}] não encontrada no DynamoDB", id)
        }
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
}
