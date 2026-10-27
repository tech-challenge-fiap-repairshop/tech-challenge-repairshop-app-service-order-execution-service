package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.execution.application.usecases.AddExecution
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.execution.domain.entities.mapper.toResponse
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionRequest
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AddExecutionImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : AddExecution {

    override fun execute(serviceOrderId: UUID, request: CreateExecutionRequest): ExecutionResponse {
        val order = serviceOrderGateway.findDetailedById(serviceOrderId)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        order.ensureNotTerminalState("add execution")

        val resolvedInsumes = request.insumes.map {
            val insume = Insume(
                id = it.insumeId ?: UUID.randomUUID(),
                name = it.name,
                brand = it.brand,
                skuId = it.skuId,
                quantity = it.quantity,
                unityPrice = it.unityPrice
            )
            insume to it.quantity
        }

        val execution = order.addExecution(
            basicDescription = request.basicDescription,
            fullDescription = request.fullDescription,
            price = request.price,
            estimatedTime = request.estimatedTime,
            insumes = resolvedInsumes
        )

        serviceOrderGateway.save(order)
        return execution.toResponse()
    }
}
