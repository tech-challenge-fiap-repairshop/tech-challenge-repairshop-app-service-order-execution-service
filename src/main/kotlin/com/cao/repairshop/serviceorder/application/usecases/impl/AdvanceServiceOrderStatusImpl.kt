package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.AdvanceServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdvanceServiceOrderStatusImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : AdvanceServiceOrderStatus {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(id: UUID, newStatus: ServiceOrderStatus): ServiceOrderResponse {
        logger.info("Solicitado avanço de status para OS [ORDER#{}]: novo status={}", id, newStatus)
        val order = serviceOrderGateway.findDetailedById(id)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        val previousStatus = order.status
        order.advanceStatus(newStatus)
        val saved = serviceOrderGateway.save(order)
        logger.info("Status da OS [ORDER#{}] avançado com SUCESSO: {} -> {}", id, previousStatus, saved.status)

        return saved.toResponse()
    }
}