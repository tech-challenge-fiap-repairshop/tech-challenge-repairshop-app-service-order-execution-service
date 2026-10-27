package com.cao.repairshop.serviceorder.domain.entities.mapper

import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrderHistory
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionSummary
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeSummaryResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderHistoryEntry
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import java.math.BigDecimal

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
