package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.core.exception.InvalidStateTransitionException
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.ManageExtraExecutions
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponseWithExtras
import com.cao.repairshop.serviceorder.infra.controller.dtos.AddExtraExecutionsRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ApprovalRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import com.cao.repairshop.serviceorder.infra.gateways.mapper.ServiceOrderDynamoMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Caso de uso para gestão de execuções extras (inclusão de lote e aprovação/recusa).
 * Refatorado para desacoplar montagem de itens DynamoDB e mapeamento de DTOs de resposta.
 */
@Service
class ManageExtraExecutionsImpl(
    private val serviceOrderGateway: ServiceOrderGateway,
    private val mapper: ServiceOrderDynamoMapper = ServiceOrderDynamoMapper()
) : ManageExtraExecutions {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun addExtraExecutions(id: UUID, request: AddExtraExecutionsRequest): ServiceOrderResponse {
        logger.info("Iniciando inclusão de execuções extras para a OS [{}]: {} tarefas solicitadas", id, request.executions.size)

        val order = serviceOrderGateway.findDetailedById(id)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        if (order.status != ServiceOrderStatus.APPROVED && order.status != ServiceOrderStatus.IN_EXECUTION) {
            logger.warn("Tentativa inválida de adicionar execuções extras na OS [{}] com status '{}'", id, order.status)
            throw InvalidStateTransitionException(
                "Cannot register extra executions for Service Order in status '${order.status}'. Allowed statuses: APPROVED, IN_EXECUTION."
            )
        }

        val extraItem = mapper.buildExtraExecutionItem(id, request)

        order.hasExtraServices = true
        order.extraServicesAccepted = null
        order.advanceStatus(
            ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS,
            "Tarefas extras adicionadas: ${request.notes ?: "Aguardando aprovação do cliente"}"
        )

        val saved = serviceOrderGateway.saveWithExtraExecution(order, extraItem)
        logger.info(
            "Lote de execuções extras [ExtraId={}] adicionado com SUCESSO na OS [{}], status={}",
            extraItem.extraExecutionId, id, saved.status
        )

        return saved.toResponseWithExtras(listOf(extraItem))
    }

    override fun approveExtraExecutions(id: UUID, request: ApprovalRequest): ServiceOrderResponse {
        logger.info("Processando aprovação/recusa de execuções extras para a OS [{}]: Aprovado={}", id, request.approved)

        val order = serviceOrderGateway.findDetailedById(id)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        if (order.status != ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS) {
            logger.warn("Tentativa de aprovação de tarefas extras na OS [{}] com status incompatível '{}'", id, order.status)
            throw InvalidStateTransitionException(
                "Cannot approve/refuse extra executions for Service Order in status '${order.status}'. Expected: WAITING_APPROVAL_EXTRA_TASKS."
            )
        }

        val fullOrder = serviceOrderGateway.getFullOrder(id)
        val pendingExtras = fullOrder.extraExecutions.filter { it.status == "WAITING_APPROVAL" }
        logger.info("Encontrados {} lotes extras pendentes de aprovação para a OS [{}]", pendingExtras.size, id)

        if (request.approved) {
            order.hasExtraServices = true
            order.extraServicesAccepted = true
            order.advanceStatus(ServiceOrderStatus.APPROVAL_EXTRA_TASKS, "Tarefas extras aprovadas pelo cliente")

            pendingExtras.forEach { pe ->
                pe.status = "APPROVED"
                pe.extraServicesAccepted = true
                pe.updateDate = Instant.now().toString()

                pe.executions.forEach { ee ->
                    val domainExec = mapper.toDomainExecution(ee, order.id)
                    order.executions.add(domainExec)
                }
            }
            order.recalculateTotalPrice()

            order.advanceStatus(ServiceOrderStatus.IN_EXECUTION, "Execução retomada com tarefas extras integradas")
        } else {
            order.hasExtraServices = true
            order.extraServicesAccepted = false
            order.advanceStatus(
                ServiceOrderStatus.REFUSED_EXTRA_TASKS,
                "Tarefas extras recusadas pelo cliente: ${request.notes ?: "Sem observações"}"
            )

            pendingExtras.forEach { pe ->
                pe.status = "REFUSED"
                pe.extraServicesAccepted = false
                pe.updateDate = Instant.now().toString()
            }

            order.advanceStatus(ServiceOrderStatus.IN_EXECUTION, "Execução retomada com os serviços originais")
        }

        val saved = serviceOrderGateway.saveWithExtraExecutions(order, pendingExtras)
        logger.info(
            "Decisão de execuções extras processada com SUCESSO para a OS [{}]: Status={}, Novo Preço={}",
            id, saved.status, saved.totalPrice
        )

        return saved.toResponseWithExtras(fullOrder.extraExecutions)
    }
}
