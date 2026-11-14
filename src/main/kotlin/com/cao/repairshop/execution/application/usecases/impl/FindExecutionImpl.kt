package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.execution.application.usecases.FindExecution
import com.cao.repairshop.execution.domain.entities.mapper.toResponse
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FindExecutionImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : FindExecution {

    override fun findById(serviceOrderId: UUID, executionId: UUID): ExecutionResponse {
        val order = serviceOrderGateway.findDetailedById(serviceOrderId)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        val execution = order.executions.find { it.id == executionId }
            ?: throw EntityNotFoundException(ErrorMessages.Execution.NOT_FOUND)

        return execution.toResponse()
    }
}
