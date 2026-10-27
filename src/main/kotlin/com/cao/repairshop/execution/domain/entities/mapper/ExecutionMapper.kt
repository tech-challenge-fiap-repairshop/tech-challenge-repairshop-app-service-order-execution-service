package com.cao.repairshop.execution.domain.entities.mapper

import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.domain.entities.ExecutionHistory
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionHistoryResponse
import com.cao.repairshop.execution.infra.controller.dtos.ExecutionResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeSummaryResponse
import java.math.BigDecimal

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
