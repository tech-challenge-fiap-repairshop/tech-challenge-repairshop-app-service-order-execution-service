package com.cao.repairshop.serviceorder.domain.entities.mapper

import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrderHistory
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionSummary
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExtraExecutionResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeSummaryResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderHistoryEntry
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import java.math.BigDecimal
import java.util.UUID

fun ServiceOrder.toResponse() = ServiceOrderResponse(
    id = id,
    customerId = customerId,
    vehicleId = vehicleId,
    status = status,
    totalPrice = totalPrice,
    enterTime = enterTime,
    endTime = endTime,
    hasExtraServices = hasExtraServices,
    extraServicesAccepted = extraServicesAccepted,
    created = created,
    updated = updated,
    services = executions.map {
        ExecutionSummary(
            id = it.id,
            basicDescription = it.basicDescription,
            fullDescription = it.fullDescription,
            laborPrice = it.price,
            insumes = it.insumes.map { ei ->
                InsumeSummaryResponse(
                    name = ei.insume.name,
                    quantity = ei.quantity,
                    unitPrice = ei.insume.unityPrice,
                    totalPrice = ei.insume.unityPrice.multiply(BigDecimal.valueOf(ei.quantity.toLong()))
                )
            },
            totalPrice = it.getTotalPrice(),
            status = it.status.name
        )
    },
    extraExecutions = emptyList(),
    history = histories.sortedBy { it.registerTime }.map { it.toServiceOrderHistoryEntry() }
)

fun ServiceOrderHistory.toServiceOrderHistoryEntry() = ServiceOrderHistoryEntry(
    id = id,
    status = status,
    registerTime = registerTime,
    intervalTime = intervalTime,
    description = description
)

fun ExtraExecutionDynamoItem.toResponse() = ExtraExecutionResponse(
    id = UUID.fromString(extraExecutionId),
    serviceOrderId = UUID.fromString(serviceOrderId),
    hasExtraServices = hasExtraServices,
    extraServicesAccepted = extraServicesAccepted,
    status = status,
    totalExtraPrice = BigDecimal.valueOf(totalExtraPrice),
    notes = notes,
    executions = executions.map { ee ->
        ExecutionSummary(
            id = UUID.fromString(ee.executionId),
            basicDescription = ee.basicDescription.ifBlank { ee.taskName },
            fullDescription = ee.fullDescription,
            laborPrice = BigDecimal.valueOf(ee.executionPrice),
            insumes = ee.insumes.map { ii ->
                InsumeSummaryResponse(
                    name = ii.name,
                    quantity = ii.quantity.toInt(),
                    unitPrice = BigDecimal.valueOf(ii.unityInsumePrice),
                    totalPrice = BigDecimal.valueOf(ii.insumePrice)
                )
            },
            totalPrice = BigDecimal.valueOf(ee.executionPrice + ee.insumes.sumOf { it.insumePrice }),
            status = ee.status
        )
    }
)

fun List<ExtraExecutionDynamoItem>.toExtraResponses(): List<ExtraExecutionResponse> =
    this.map { it.toResponse() }

fun ServiceOrder.toResponseWithExtras(extraItems: List<ExtraExecutionDynamoItem>): ServiceOrderResponse =
    this.toResponse().copy(
        hasExtraServices = this.hasExtraServices,
        extraServicesAccepted = this.extraServicesAccepted,
        extraExecutions = extraItems.toExtraResponses()
    )
