package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.execution.domain.entities.Insume
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
import java.util.UUID

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
            val resolvedInsumes = sd.insumes.map {
                val insume = Insume(
                    id = it.insumeId ?: UUID.randomUUID(),
                    name = it.name,
                    brand = it.brand,
                    skuId = it.skuId,
                    quantity = it.quantity,
                    unityPrice = it.unityPrice
                )
                insume to it.quantity
            }
            order.addExecution(
                basicDescription = sd.basicDescription,
                fullDescription = sd.fullDescription,
                price = sd.price ?: BigDecimal.ZERO,
                estimatedTime = sd.estimatedTime,
                insumes = resolvedInsumes
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
