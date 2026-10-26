package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.execution.domain.BasicExecution
import com.cao.repairshop.inventory.application.gateways.InsumeGateway
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.CreateServiceOrder
import com.cao.repairshop.serviceorder.application.usecases.NotifyCustomer
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.CreateServiceOrderRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class CreateServiceOrderImpl(
    private val serviceOrderGateway: ServiceOrderGateway,
    private val insumeGateway: InsumeGateway,
    private val notifyCustomer: NotifyCustomer
) : CreateServiceOrder {

    @Transactional
    override fun execute(request: CreateServiceOrderRequest): ServiceOrderResponse {
        val customerId = request.customerId
            ?: request.customerEmail?.let { UUID.nameUUIDFromBytes(it.toByteArray()) }
            ?: UUID.randomUUID()

        val vehicleId = request.vehicleId
            ?: request.vehiclePlate?.let { UUID.nameUUIDFromBytes(it.toByteArray()) }
            ?: UUID.randomUUID()

        val order = ServiceOrder(
            customerId = customerId,
            vehicleId = vehicleId,
            enterTime = LocalDateTime.now()
        )

        request.services.forEach { sd ->
            val resolvedInsumes = sd.insumes.mapNotNull {
                val insume = insumeGateway.findById(it.insumeId)
                if (insume != null) insume to it.quantity else null
            }
            order.addExecution(
                basicDescription = BasicExecution.valueOf(sd.basicDescription.uppercase()),
                fullDescription = sd.fullDescription,
                price = sd.price ?: BigDecimal.ZERO,
                estimatedTime = sd.estimatedTime,
                insumes = resolvedInsumes
            )
        }

        order.recordHistory(ServiceOrderStatus.RECEIVED)

        val saved = serviceOrderGateway.save(order)
        notifyCustomer.execute(saved, ServiceOrderStatus.RECEIVED)
        return saved.toResponse()
    }
}
