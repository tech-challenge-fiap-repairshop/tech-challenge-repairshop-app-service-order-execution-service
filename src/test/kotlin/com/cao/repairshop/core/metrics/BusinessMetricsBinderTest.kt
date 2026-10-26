package com.cao.repairshop.core.metrics

import com.cao.repairshop.inventory.infra.persistence.repositories.InsumeRepository
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.persistence.repositories.ServiceOrderRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BusinessMetricsBinderTest {

    private lateinit var serviceOrderRepository: ServiceOrderRepository
    private lateinit var insumeRepository: InsumeRepository
    private lateinit var registry: SimpleMeterRegistry
    private lateinit var binder: BusinessMetricsBinder

    @BeforeEach
    fun setup() {
        serviceOrderRepository = mockk()
        insumeRepository = mockk()
        registry = SimpleMeterRegistry()

        binder = BusinessMetricsBinder(
            serviceOrderRepository,
            insumeRepository
        )
    }

    @Test
    fun `should bind and evaluate all business gauges correctly`() {
        every { serviceOrderRepository.count() } returns 100L
        every { serviceOrderRepository.countByStatus(any()) } returns 10L
        every { serviceOrderRepository.getAverageStageDurationMinutes(any(), any()) } returns 15.5
        every { serviceOrderRepository.getAverageLeadTimeMinutes() } returns 180.0
        every { insumeRepository.getTotalStockQuantity() } returns 500L
        every { insumeRepository.getTotalInventoryValue() } returns BigDecimal("35000.00")

        binder.bindTo(registry)

        val soTotalGauge = registry.find("repairshop_business_service_orders_total").gauge()
        assertNotNull(soTotalGauge)
        assertEquals(100.0, soTotalGauge!!.value())

        val statusGauge = registry.find("repairshop_business_service_orders_by_status")
            .tag("status", ServiceOrderStatus.IN_EXECUTION.name).gauge()
        assertNotNull(statusGauge)
        assertEquals(10.0, statusGauge!!.value())

        val stageDurationGauge = registry.find("repairshop_business_stage_duration_minutes")
            .tag("from_status", "RECEIVED")
            .tag("to_status", "IN_DIAGNOSIS").gauge()
        assertNotNull(stageDurationGauge)
        assertEquals(15.5, stageDurationGauge!!.value())

        val leadTimeGauge = registry.find("repairshop_business_lead_time_minutes").gauge()
        assertNotNull(leadTimeGauge)
        assertEquals(180.0, leadTimeGauge!!.value())

        val stockGauge = registry.find("repairshop_business_inventory_items_total").gauge()
        assertNotNull(stockGauge)
        assertEquals(500.0, stockGauge!!.value())

        val stockValueGauge = registry.find("repairshop_business_inventory_value_total_brl").gauge()
        assertNotNull(stockValueGauge)
        assertEquals(35000.0, stockValueGauge!!.value())
    }

    @Test
    fun `should handle repository exceptions gracefully and return zero fallback`() {
        every { serviceOrderRepository.count() } throws RuntimeException("DB error")
        every { serviceOrderRepository.countByStatus(any()) } throws RuntimeException("DB error")
        every { serviceOrderRepository.getAverageStageDurationMinutes(any(), any()) } throws RuntimeException("DB error")
        every { serviceOrderRepository.getAverageLeadTimeMinutes() } throws RuntimeException("DB error")
        every { insumeRepository.getTotalStockQuantity() } throws RuntimeException("DB error")
        every { insumeRepository.getTotalInventoryValue() } throws RuntimeException("DB error")

        binder.bindTo(registry)

        val soTotalGauge = registry.find("repairshop_business_service_orders_total").gauge()
        assertEquals(0.0, soTotalGauge!!.value())
    }
}
