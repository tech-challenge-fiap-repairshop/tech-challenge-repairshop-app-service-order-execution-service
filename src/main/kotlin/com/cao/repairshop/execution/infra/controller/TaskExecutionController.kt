package com.cao.repairshop.execution.infra.controller

import com.cao.repairshop.execution.application.usecases.AddExecution
import com.cao.repairshop.execution.application.usecases.AdvanceExecutionStatus
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionRequest
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Controller alinhado às rotas oficiais do desenho de arquitetura da Fase 4:
 * POST /execute_task/{execution_id}
 * POST /increment_execution_task/{service_order_id}
 */
@RestController
@Tag(name = "Executions - Fase 4", description = "Endpoints de execução técnica da OS conforme desenho de arquitetura")
class TaskExecutionController(
    private val addExecution: AddExecution,
    private val advanceExecutionStatus: AdvanceExecutionStatus
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/execute_task/{executionId}")
    @Operation(summary = "Executa / avança status de uma tarefa técnica")
    fun executeTask(
        @PathVariable executionId: UUID,
        @RequestParam(required = false) serviceOrderId: UUID?,
        @RequestParam(defaultValue = "FINALIZED") status: ExecutionStatus
    ): ExecutionResponse {
        val sId = serviceOrderId ?: executionId
        logger.info("REST POST /execute_task/{} recebido: serviceOrderId={}, status={}", executionId, sId, status)
        val response = advanceExecutionStatus.execute(sId, executionId, status)
        logger.info("REST POST /execute_task/{} concluído: status={}", executionId, response.status)
        return response
    }

    @PostMapping("/increment_execution_task/{serviceOrderId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona / incrementa tarefa técnica na OS")
    fun incrementExecutionTask(
        @PathVariable serviceOrderId: UUID,
        @RequestBody request: CreateExecutionRequest
    ): ExecutionResponse {
        logger.info("REST POST /increment_execution_task/{} recebido: tarefa='{}'", serviceOrderId, request.basicDescription)
        val response = addExecution.execute(serviceOrderId, request)
        logger.info("REST POST /increment_execution_task/{} concluído: executionId={}", serviceOrderId, response.id)
        return response
    }
}
