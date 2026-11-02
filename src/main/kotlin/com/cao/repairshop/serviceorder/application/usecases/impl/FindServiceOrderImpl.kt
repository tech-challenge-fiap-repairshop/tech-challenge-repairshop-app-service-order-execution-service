package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.FindServiceOrder
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponse
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponseWithExtras
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FindServiceOrderImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : FindServiceOrder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun findById(id: UUID): ServiceOrderResponse {
        logger.debug("Buscando detalhes consolidados da OS [ORDER#{}]", id)
        val order = serviceOrderGateway.findDetailedById(id)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        val fullOrder = serviceOrderGateway.getFullOrder(id)
        logger.info("OS [ORDER#{}] encontrada: status={}, total_execuções={}, total_extras={}", id, order.status, order.executions.size, fullOrder.extraExecutions.size)

        return order.toResponseWithExtras(fullOrder.extraExecutions)
    }

    override fun findAll(
        customerId: UUID?,
        vehicleId: UUID?,
        status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrderResponse> {
        logger.debug("Listagem de Ordens de Serviço: customerId={}, vehicleId={}, status={}", customerId, vehicleId, status)
        return serviceOrderGateway.findAll(customerId, vehicleId, status, pageable).map { it.toResponse() }
    }
}
