package com.cao.repairshop.serviceorder.domain

import com.cao.repairshop.execution.domain.BasicExecution
import com.cao.repairshop.inventory.domain.entities.Insume
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
    fun `approve transitions order to APPROVED and returns stock requirements`() {
        val order = buildOrder()
        val insume = Insume(id = UUID.randomUUID(), name = "Filter", price = BigDecimal("25"), unityPrice = BigDecimal("25"), quantity = 10)
        order.addExecution(BasicExecution.OIL_CHANGE, null, BigDecimal("100"), null, listOf(insume to 3))

        val requirements = service.approve(order)

        assertThat(order.status).isEqualTo(ServiceOrderStatus.IN_EXECUTION)
        assertThat(requirements).hasSize(1)
        assertThat(requirements.first().quantity).isEqualTo(3)
    }

    @Test
    fun `approve with no insumes returns empty requirements`() {
        val order = buildOrder()
        order.addExecution(BasicExecution.OIL_CHANGE, null, BigDecimal("100"), null, emptyList<Pair<Insume, Int>>())

        val requirements = service.approve(order)

        assertThat(order.status).isEqualTo(ServiceOrderStatus.IN_EXECUTION)
        assertThat(requirements).isEmpty()
    }

    @Test
    fun `refuse transitions order to REFUSED`() {
        val order = buildOrder()

        service.refuse(order)

        assertThat(order.status).isEqualTo(ServiceOrderStatus.REFUSED)
    }
}
