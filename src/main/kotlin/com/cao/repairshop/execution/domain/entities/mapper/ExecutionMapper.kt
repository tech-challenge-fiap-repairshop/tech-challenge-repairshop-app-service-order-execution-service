package com.cao.repairshop.execution.domain.entities.mapper

import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.domain.entities.ExecutionHistory
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.execution.infra.controller.dtos.CreateExecutionRequest
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionHistoryResponse
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionDefinitionRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeItemRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeSummaryResponse
import java.math.BigDecimal
import java.util.UUID

fun Execution.toResponse() = ExecutionResponse(
    id = id,
    serviceOrderId = serviceOrderId,
    basicDescription = basicDescription,
    fullDescription = fullDescription,
    laborPrice = price,
    insumes = insumes.map { ei ->
        InsumeSummaryResponse(
            name = ei.insume.name,
            quantity = ei.quantity,
            unitPrice = ei.insume.unityPrice,
            totalPrice = ei.insume.unityPrice.multiply(BigDecimal.valueOf(ei.quantity.toLong()))
        )
    },
    totalPrice = getTotalPrice(),
    estimatedTime = estimatedTime,
    status = status,
    created = created,
    updated = updated
)

fun ExecutionHistory.toResponse() = ExecutionHistoryResponse(
    id = id,
    executionId = executionId,
    status = status,
    registerTime = registerTime,
    intervalTime = intervalTime
)

/**
 * Converte um item de requisição de insumo para entidade de domínio Insume e quantidade
 */
fun InsumeItemRequest.toDomain(): Pair<Insume, Int> {
    val insume = Insume(
        id = this.insumeId ?: UUID.randomUUID(),
        name = this.name,
        brand = this.brand,
        skuId = this.skuId,
        quantity = this.quantity,
        unityPrice = this.unityPrice
    )
    return insume to this.quantity
}

/**
 * Converte lista de insumos de requisição para lista de pares de domínio (Insume, Quantidade)
 */
fun List<InsumeItemRequest>.toDomainInsumes(): List<Pair<Insume, Int>> =
    this.map { it.toDomain() }

/**
 * Converte requisição unitária CreateExecutionRequest para a definição genérica de execução
 */
fun CreateExecutionRequest.toDefinition(): ExecutionDefinitionRequest =
    ExecutionDefinitionRequest(
        basicDescription = this.basicDescription,
        fullDescription = this.fullDescription,
        price = this.price,
        estimatedTime = this.estimatedTime,
        insumes = this.insumes
    )
