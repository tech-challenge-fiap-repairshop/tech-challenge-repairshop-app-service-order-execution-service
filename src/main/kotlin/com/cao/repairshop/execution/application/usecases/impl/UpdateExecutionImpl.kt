package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.execution.application.usecases.UpdateExecution
import com.cao.repairshop.execution.domain.entities.mapper.toResponse
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import com.cao.repairshop.execution.infra.controller.dtos.UpdateExecutionRequest
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class UpdateExecutionImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : UpdateExecution {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(serviceOrderId: UUID, executionId: UUID, request: UpdateExecutionRequest): ExecutionResponse {
        logger.info(
            "Atualizando dados da execução [EXEC#{}] na OS [ORDER#{}]: tarefa='{}', preço={}",
            executionId, serviceOrderId, request.basicDescription, request.price
        )
        val order = serviceOrderGateway.findDetailedById(serviceOrderId)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        order.ensureNotTerminalState("update execution")

        val execution = order.executions.find { it.id == executionId }
            ?: throw EntityNotFoundException(ErrorMessages.Execution.NOT_FOUND)

        execution.apply {
            basicDescription = request.basicDescription
            fullDescription = request.fullDescription
            price = request.price
            estimatedTime = request.estimatedTime
            updated = LocalDateTime.now()
            recordHistory(status, "Execution attributes updated (price, description or time)")
        }

        order.recalculateTotalPrice()
        serviceOrderGateway.save(order)
        logger.info(
            "Execução [EXEC#{}] atualizada com SUCESSO na OS [ORDER#{}]: novo preço={}",
            executionId, serviceOrderId, order.totalPrice
        )

        return execution.toResponse()
    }
}
