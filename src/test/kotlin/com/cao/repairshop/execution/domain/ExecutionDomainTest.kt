package com.cao.repairshop.execution.domain

import com.cao.repairshop.core.exception.InvalidStateTransitionException
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class ExecutionDomainTest {

    private fun buildExecution(status: ExecutionStatus = ExecutionStatus.INITIATED): Execution {
        val order = ServiceOrder(customerId = UUID.randomUUID(), vehicleId = UUID.randomUUID())
        return Execution(serviceOrderId = order.id, basicDescription = "OIL_CHANGE", price = BigDecimal("100"), status = status)
    }

    private fun buildInsume(price: String = "25") = Insume(id = UUID.randomUUID(), name = "Oil Filter", unityPrice = BigDecimal(price), quantity = 10)

    @Test
    fun `advanceStatus valid transition INITIATED to PENDING records history`() {
        val exec = buildExecution(ExecutionStatus.INITIATED)
        exec.advanceStatus(ExecutionStatus.PENDING)
        assertThat(exec.status).isEqualTo(ExecutionStatus.PENDING)
        assertThat(exec.histories).hasSize(1)
    }

    @Test
    fun `advanceStatus invalid transition throws exception`() {
        val exec = buildExecution(ExecutionStatus.INITIATED)
        assertThatThrownBy { exec.advanceStatus(ExecutionStatus.FINALIZED) }
            .isInstanceOf(InvalidStateTransitionException::class.java)
    }

    @Test
    fun `addInsume links insume to execution`() {
        val exec = buildExecution()
        val insume = buildInsume()
        exec.addInsume(insume, 3)

        assertThat(exec.insumes).hasSize(1)
        assertThat(exec.insumes.first().quantity).isEqualTo(3)
    }

    @Test
    fun `addInsume duplicate insume throws exception`() {
        val exec = buildExecution()
        val insume = buildInsume()
        exec.addInsume(insume, 2)

        assertThatThrownBy { exec.addInsume(insume, 3) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("already linked")
    }

    @Test
    fun `recordHistory adds entry with status`() {
        val exec = buildExecution()
        exec.recordHistory(ExecutionStatus.INITIATED)
        assertThat(exec.histories).hasSize(1)
        assertThat(exec.histories.first().status).isEqualTo(ExecutionStatus.INITIATED)
    }

    @Test
    fun `recordHistory second call calculates interval`() {
        val exec = buildExecution()
        exec.recordHistory(ExecutionStatus.INITIATED)
        exec.recordHistory(ExecutionStatus.PENDING)

        val sorted = exec.histories.sortedBy { it.registerTime }
        assertThat(sorted.last().intervalTime).isNotNull
    }
    
    @Test
    fun `getTotalPrice calculates sum of base price and all insumes`() {
        val exec = buildExecution()
        exec.price = BigDecimal("100.00")
        
        val insume1 = buildInsume("50.00")
        val insume2 = buildInsume("30.00")
        
        exec.addInsume(insume1, 2) // 100.00
        exec.addInsume(insume2, 1) // 30.00
        
        // Total: 100 (base) + 100 (insume1) + 30 (insume2) = 230
        assertThat(exec.getTotalPrice()).isEqualByComparingTo(BigDecimal("230.00"))
    }
}
