package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.execution.application.usecases.AddExecutionBatch
import com.cao.repairshop.execution.domain.entities.mapper.toDomainInsumes
import com.cao.repairshop.execution.domain.entities.mapper.toResponse
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionBatchRequest
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class AddExecutionBatchImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : AddExecutionBatch {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(serviceOrderId: UUID, request: CreateExecutionBatchRequest): List<ExecutionResponse> {
        logger.info(
            "Adicionando lote de execuções na OS [ORDER#{}]: {} tarefas no lote",
            serviceOrderId, request.executions.size
        )
        val order = serviceOrderGateway.findDetailedById(serviceOrderId)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        order.ensureNotTerminalState("add executions in batch")

        val executions = request.executions.map { execDef ->
            order.addExecution(
                basicDescription = execDef.basicDescription,
                fullDescription = execDef.fullDescription,
                price = execDef.price ?: BigDecimal.ZERO,
                estimatedTime = execDef.estimatedTime,
                insumes = execDef.insumes.toDomainInsumes()
            )
        }

        serviceOrderGateway.save(order)
        logger.info(
            "Lote de {} execuções adicionado com SUCESSO na OS [ORDER#{}]: novo preço={}",
            executions.size, serviceOrderId, order.totalPrice
        )

        return executions.map { it.toResponse() }
    }
}
