package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.execution.application.usecases.AdvanceExecutionStatus
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.domain.entities.mapper.toResponse
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdvanceExecutionStatusImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : AdvanceExecutionStatus {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(serviceOrderId: UUID, executionId: UUID, newStatus: ExecutionStatus): ExecutionResponse {
        logger.info(
            "Avançando status da execução [EXEC#{}] na OS [ORDER#{}]: novo status={}",
            executionId, serviceOrderId, newStatus
        )
        val order = serviceOrderGateway.findDetailedById(serviceOrderId)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        order.ensureNotTerminalState("advance execution status")

        val execution = order.executions.find { it.id == executionId }
            ?: throw EntityNotFoundException(ErrorMessages.Execution.NOT_FOUND)

        val previousStatus = execution.status
        execution.advanceStatus(newStatus)
        order.checkCompletion()

        serviceOrderGateway.save(order)
        logger.info(
            "Status da execução [EXEC#{}] avançado com SUCESSO: {} -> {} (Status atual da OS: {})",
            executionId, previousStatus, execution.status, order.status
        )

        return execution.toResponse()
    }
}
