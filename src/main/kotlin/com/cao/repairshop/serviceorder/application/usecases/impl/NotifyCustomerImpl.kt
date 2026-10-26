package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.serviceorder.application.usecases.NotifyCustomer
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.messaging.ServiceOrderSqsMessenger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotifyCustomerImpl(
    private val sqsMessenger: ServiceOrderSqsMessenger
) : NotifyCustomer {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(order: ServiceOrder, status: ServiceOrderStatus) {
        logger.info("Disparando notificação para o cliente da OS {} (Status: {})", order.id, status)
        when (status) {
            ServiceOrderStatus.WAITING_APPROVAL -> {
                sqsMessenger.sendOrderForApproval(
                    orderId = order.id,
                    customerId = order.customerId,
                    vehicleId = order.vehicleId,
                    totalPrice = order.totalPrice.toDouble(),
                    description = "Ordem de serviço aguardando aprovação do cliente"
                )
            }
            ServiceOrderStatus.FINALIZED -> {
                // Notifica que a OS foi concluída e está pronta para cobrança
                sqsMessenger.sendOrderReadyForPayment(
                    orderId = order.id,
                    customerId = order.customerId,
                    amount = order.totalPrice.toDouble()
                )
            }
            else -> {
                logger.debug("Status {} registrado para OS {} sem necessidade de fila síncrona", status, order.id)
            }
        }
    }
}
