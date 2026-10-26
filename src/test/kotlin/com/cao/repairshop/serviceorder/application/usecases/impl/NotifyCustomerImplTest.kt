package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.messaging.ServiceOrderSqsMessenger
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class NotifyCustomerImplTest {

    private lateinit var sqsMessenger: ServiceOrderSqsMessenger
    private lateinit var notifyCustomerImpl: NotifyCustomerImpl

    @BeforeEach
    fun setup() {
        sqsMessenger = mockk(relaxed = true)
        notifyCustomerImpl = NotifyCustomerImpl(sqsMessenger)
    }

    @Test
    fun `should send notification via SQS when waiting approval`() {
        val order = ServiceOrder(
            id = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            totalPrice = BigDecimal("250.00")
        )

        notifyCustomerImpl.execute(order, ServiceOrderStatus.WAITING_APPROVAL)

        verify(exactly = 1) {
            sqsMessenger.sendOrderForApproval(order.id, order.customerId, order.vehicleId, 250.0, any())
        }
    }

    @Test
    fun `should send notification via SQS when order is finalized`() {
        val order = ServiceOrder(
            id = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            totalPrice = BigDecimal("350.00")
        )

        notifyCustomerImpl.execute(order, ServiceOrderStatus.FINALIZED)

        verify(exactly = 1) {
            sqsMessenger.sendOrderReadyForPayment(order.id, order.customerId, 350.0)
        }
    }
}
