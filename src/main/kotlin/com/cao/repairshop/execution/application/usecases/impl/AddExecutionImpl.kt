package com.cao.repairshop.execution.application.usecases.impl

import com.cao.repairshop.execution.application.usecases.AddExecution
import com.cao.repairshop.execution.application.usecases.AddExecutionBatch
import com.cao.repairshop.execution.domain.entities.mapper.toDefinition
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionBatchRequest
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionRequest
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Caso de uso para adicionar execução individual na Ordem de Serviço.
 * Refatorado para unificar o fluxo através de lote (batch de 1 item),
 * eliminando duplicação de código de validação, mapeamento de insumos e persistência.
 */
@Service
class AddExecutionImpl(
    private val addExecutionBatch: AddExecutionBatch
) : AddExecution {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(serviceOrderId: UUID, request: CreateExecutionRequest): ExecutionResponse {
        logger.info(
            "Adicionando execução na OS [ORDER#{}]: tarefa='{}', delegando para processamento unificado em lote",
            serviceOrderId, request.basicDescription
        )
        val batchRequest = CreateExecutionBatchRequest(
            serviceOrderId = serviceOrderId,
            executions = listOf(request.toDefinition())
        )
        val results = addExecutionBatch.execute(serviceOrderId, batchRequest)
        return results.first()
    }
}
