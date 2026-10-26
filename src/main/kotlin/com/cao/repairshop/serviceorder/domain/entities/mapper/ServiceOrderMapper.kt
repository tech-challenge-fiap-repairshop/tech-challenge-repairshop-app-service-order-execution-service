package com.cao.repairshop.serviceorder.domain.entities.mapper

import com.cao.repairshop.execution.domain.entities.mapper.toDomain
import com.cao.repairshop.execution.domain.entities.mapper.toEntity
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrderHistory
import com.cao.repairshop.serviceorder.infra.persistence.models.ServiceOrderEntity
import com.cao.repairshop.serviceorder.infra.persistence.models.ServiceOrderHistoryEntity
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
                    unitPrice = ei.insume.price,
                    totalPrice = ei.insume.price.multiply(BigDecimal.valueOf(ei.quantity.toLong()))
                )
            },
            totalPrice = it.getTotalPrice(),
            status = it.status.name
        )
    },
    history = histories.sortedBy { it.registerTime }.map { it.toServiceOrderHistoryEntry() }
)

fun ServiceOrderHistory.toServiceOrderHistoryEntry() = ServiceOrderHistoryEntry(
    id = id,
    status = status,
    registerTime = registerTime,
    intervalTime = intervalTime
)

fun ServiceOrderEntity.toDomain(): ServiceOrder {
    val serviceOrder = ServiceOrder(
        id = id,
        customerId = customerId,
        vehicleId = vehicleId,
        status = status,
        totalPrice = totalPrice,
        enterTime = enterTime,
        endTime = endTime,
        validDate = validDate,
        created = created,
        updated = updated
    )
    serviceOrder.histories = histories.map { it.toDomain() }.toMutableSet()
    serviceOrder.executions = executions.map { it.toDomain() }.toMutableSet()
    return serviceOrder
}

fun ServiceOrder.toEntity(): ServiceOrderEntity {
    val serviceOrderEntity = ServiceOrderEntity(
        id = id,
        customerId = customerId,
        vehicleId = vehicleId,
        status = status,
        totalPrice = totalPrice,
        enterTime = enterTime,
        endTime = endTime,
        validDate = validDate,
        created = created,
        updated = updated
    )
    serviceOrderEntity.histories = histories.map { it.toEntity(serviceOrderEntity) }.toMutableSet()
    serviceOrderEntity.executions = executions.map { it.toEntity(serviceOrderEntity) }.toMutableSet()
    return serviceOrderEntity
}

fun ServiceOrderHistory.toEntity(serviceOrderEntity: ServiceOrderEntity) = ServiceOrderHistoryEntity(
    id = id,
    serviceOrder = serviceOrderEntity,
    status = status,
    description = description,
    registerTime = registerTime,
    intervalTime = intervalTime
)

fun ServiceOrderHistoryEntity.toDomain() = ServiceOrderHistory(
    id = id,
    serviceOrderId = serviceOrder.id,
    status = status,
    description = description,
    registerTime = registerTime,
    intervalTime = intervalTime
)
