package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.FindServiceOrder
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionSummary
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExtraExecutionResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeSummaryResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class FindServiceOrderImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : FindServiceOrder {

    override fun findById(id: UUID): ServiceOrderResponse {
        val order = serviceOrderGateway.findDetailedById(id)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        val fullOrder = serviceOrderGateway.getFullOrder(id)
        val extraResponses = fullOrder.extraExecutions.map { ei ->
            ExtraExecutionResponse(
                id = UUID.fromString(ei.extraExecutionId),
                serviceOrderId = UUID.fromString(ei.serviceOrderId),
                hasExtraServices = ei.hasExtraServices,
                extraServicesAccepted = ei.extraServicesAccepted,
                status = ei.status,
                totalExtraPrice = BigDecimal.valueOf(ei.totalExtraPrice),
                notes = ei.notes,
                executions = ei.executions.map { ee ->
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
        }

        return order.toResponse().copy(
            hasExtraServices = order.hasExtraServices,
            extraServicesAccepted = order.extraServicesAccepted,
            extraExecutions = extraResponses
        )
    }

    override fun findAll(
        customerId: UUID?,
        vehicleId: UUID?,
        status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrderResponse> {
        return serviceOrderGateway.findAll(customerId, vehicleId, status, pageable).map { it.toResponse() }
    }
}
