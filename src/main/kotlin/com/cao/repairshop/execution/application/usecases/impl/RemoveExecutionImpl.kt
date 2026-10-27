package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.execution.application.usecases.RemoveExecution
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RemoveExecutionImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : RemoveExecution {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(serviceOrderId: UUID, executionId: UUID) {
        logger.info("Removendo execução [EXEC#{}] da OS [ORDER#{}]", executionId, serviceOrderId)
        val order = serviceOrderGateway.findDetailedById(serviceOrderId)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        order.ensureNotTerminalState("remove execution")

        val execution = order.executions.find { it.id == executionId }
            ?: throw EntityNotFoundException(ErrorMessages.Execution.NOT_FOUND)

        order.executions.remove(execution)
        order.recalculateTotalPrice()
        serviceOrderGateway.save(order)
        logger.info(
            "Execução [EXEC#{}] removida com SUCESSO da OS [ORDER#{}]: novo preço={}",
            executionId, serviceOrderId, order.totalPrice
        )
    }
}
