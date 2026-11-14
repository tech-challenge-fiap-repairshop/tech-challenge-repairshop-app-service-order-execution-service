package com.cao.repairshop.execution.infra.controller

import com.cao.repairshop.execution.application.usecases.*
import com.cao.repairshop.execution.infra.controller.dtos.*
import com.cao.repairshop.execution.infra.controller.interfaces.ExecutionApi
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/service-orders/{serviceOrderId}/executions")
class ExecutionController(
    private val addExecution: AddExecution,
    private val addExecutionBatch: AddExecutionBatch,
    private val findExecution: FindExecution,
    private val updateExecution: UpdateExecution,
    private val removeExecution: RemoveExecution,
    private val advanceExecutionStatus: AdvanceExecutionStatus
) : ExecutionApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(
        @PathVariable serviceOrderId: UUID,
        @RequestBody request: CreateExecutionRequest
    ): ExecutionResponse {
        logger.info("REST POST /executions recebido na OS [{}]: tarefa='{}', preço={}", serviceOrderId, request.basicDescription, request.price)
        val response = addExecution.execute(serviceOrderId, request)
        logger.info("REST POST /executions concluído na OS [{}]: executionId={}", serviceOrderId, response.id)
        return response
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    override fun createBatch(
        @PathVariable serviceOrderId: UUID,
        @RequestBody request: CreateExecutionBatchRequest
    ): List<ExecutionResponse> {
        logger.info("REST POST /executions/batch recebido na OS [{}]: {} tarefas no lote", serviceOrderId, request.executions.size)
        val response = addExecutionBatch.execute(serviceOrderId, request)
        logger.info("REST POST /executions/batch concluído na OS [{}]: {} tarefas criadas", serviceOrderId, response.size)
        return response
    }

    @GetMapping("/{executionId}")
    override fun findById(
        @PathVariable serviceOrderId: UUID,
        @PathVariable executionId: UUID
    ): ExecutionResponse {
        logger.debug("REST GET /executions/{} recebido na OS [{}]", executionId, serviceOrderId)
        return findExecution.findById(serviceOrderId, executionId)
    }

    @PutMapping("/{executionId}")
    override fun update(
        @PathVariable serviceOrderId: UUID,
        @PathVariable executionId: UUID,
        @RequestBody request: UpdateExecutionRequest
    ): ExecutionResponse {
        logger.info("REST PUT /executions/{} recebido na OS [{}]: tarefa='{}', preço={}", executionId, serviceOrderId, request.basicDescription, request.price)
        val response = updateExecution.execute(serviceOrderId, executionId, request)
        logger.info("REST PUT /executions/{} concluído na OS [{}]", executionId, serviceOrderId)
        return response
    }

    @DeleteMapping("/{executionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun delete(
        @PathVariable serviceOrderId: UUID,
        @PathVariable executionId: UUID
    ) {
        logger.info("REST DELETE /executions/{} recebido na OS [{}]", executionId, serviceOrderId)
        removeExecution.execute(serviceOrderId, executionId)
        logger.info("REST DELETE /executions/{} concluído na OS [{}]", executionId, serviceOrderId)
    }

    @PatchMapping("/{executionId}/status")
    override fun advanceStatus(
        @PathVariable serviceOrderId: UUID,
        @PathVariable executionId: UUID,
        @RequestBody request: ExecutionStatusUpdateRequest
    ): ExecutionResponse {
        logger.info("REST PATCH /executions/{}/status recebido na OS [{}]: novo status={}", executionId, serviceOrderId, request.status)
        val response = advanceExecutionStatus.execute(serviceOrderId, executionId, request.status)
        logger.info("REST PATCH /executions/{}/status concluído na OS [{}]: status={}", executionId, serviceOrderId, response.status)
        return response
    }
}
