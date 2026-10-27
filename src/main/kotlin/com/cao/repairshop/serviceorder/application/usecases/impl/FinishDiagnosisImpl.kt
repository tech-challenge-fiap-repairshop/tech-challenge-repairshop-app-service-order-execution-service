package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.FinishDiagnosis
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FinishDiagnosisImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : FinishDiagnosis {

    override fun execute(id: UUID): ServiceOrderResponse {
        val order = serviceOrderGateway.findDetailedById(id)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        order.advanceStatus(ServiceOrderStatus.WAITING_APPROVAL, "Diagnóstico finalizado e enviado para aprovação do cliente")
        val saved = serviceOrderGateway.save(order)
        return saved.toResponse()
    }
}
