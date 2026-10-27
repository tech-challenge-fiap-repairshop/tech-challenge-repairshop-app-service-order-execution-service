package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.execution.domain.entities.mapper.toDomainInsumes
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.CreateServiceOrder
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.CreateServiceOrderRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class CreateServiceOrderImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : CreateServiceOrder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(request: CreateServiceOrderRequest): ServiceOrderResponse {
        logger.info(
            "Iniciando criação de Ordem de Serviço: customerId={}, vehicleId={}, quantidade_servicos={}",
            request.customerId, request.vehicleId, request.services.size
        )

        val order = ServiceOrder(
            customerId = request.customerId,
            vehicleId = request.vehicleId,
            status = ServiceOrderStatus.IN_DIAGNOSIS,
            enterTime = LocalDateTime.now()
        )

        request.services.forEach { sd ->
            order.addExecution(
                basicDescription = sd.basicDescription,
                fullDescription = sd.fullDescription,
                price = sd.price ?: BigDecimal.ZERO,
                estimatedTime = sd.estimatedTime,
                insumes = sd.insumes.toDomainInsumes()
            )
        }

        order.recordHistory(ServiceOrderStatus.IN_DIAGNOSIS, "Ordem de serviço criada em diagnóstico")

        val saved = serviceOrderGateway.save(order)
        logger.info(
            "Ordem de Serviço criada com SUCESSO: [ORDER#{}], status={}, preço_total={}",
            saved.id, saved.status, saved.totalPrice
        )

        return saved.toResponse()
    }
}
