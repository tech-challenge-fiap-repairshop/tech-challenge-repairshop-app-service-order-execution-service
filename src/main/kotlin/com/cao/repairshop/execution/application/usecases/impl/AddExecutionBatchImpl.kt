package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.execution.application.usecases.AddExecutionBatch
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.execution.domain.entities.mapper.toResponse
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionBatchRequest
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class AddExecutionBatchImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : AddExecutionBatch {

    override fun execute(serviceOrderId: UUID, request: CreateExecutionBatchRequest): List<ExecutionResponse> {
        val order = serviceOrderGateway.findDetailedById(serviceOrderId)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        order.ensureNotTerminalState("add executions in batch")

        val executions = request.executions.map { execDef ->
            val resolvedInsumes = execDef.insumes.map {
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

            order.addExecution(
                basicDescription = execDef.basicDescription,
                fullDescription = execDef.fullDescription,
                price = execDef.price ?: BigDecimal.ZERO,
                estimatedTime = execDef.estimatedTime,
                insumes = resolvedInsumes
            )
        }

        serviceOrderGateway.save(order)
        return executions.map { it.toResponse() }
    }
}
