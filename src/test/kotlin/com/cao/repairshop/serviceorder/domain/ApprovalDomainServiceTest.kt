package com.cao.repairshop.serviceorder.domain

import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class ApprovalDomainServiceTest {

    private val service = ApprovalDomainService()

    private fun buildOrder(): ServiceOrder {
        return ServiceOrder(customerId = UUID.randomUUID(), vehicleId = UUID.randomUUID(), status = ServiceOrderStatus.WAITING_APPROVAL, enterTime = LocalDateTime.now())
    }

    @Test
    fun `approve transitions order to IN_EXECUTION`() {
        val order = buildOrder()
        val insume = Insume(id = UUID.randomUUID(), name = "Filter", unityPrice = BigDecimal("25"), quantity = 10)
        order.addExecution("Troca de Óleo", null, BigDecimal("100"), null, listOf(insume to 3))

        service.approve(order)

        assertThat(order.status).isEqualTo(ServiceOrderStatus.IN_EXECUTION)
    }

    @Test
    fun `refuse transitions order to REFUSED`() {
        val order = buildOrder()

        service.refuse(order)

        assertThat(order.status).isEqualTo(ServiceOrderStatus.REFUSED)
    }
}
