package com.cao.repairshop.core.metrics

import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.ServiceOrderDynamoItem
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class BusinessMetricsBinderTest {

    private lateinit var dynamoRepository: DynamoServiceOrderRepository
    private lateinit var registry: SimpleMeterRegistry
    private lateinit var binder: BusinessMetricsBinder

    @BeforeEach
    fun setup() {
        dynamoRepository = mockk()
        registry = SimpleMeterRegistry()
        binder = BusinessMetricsBinder(dynamoRepository)
    }

    @Test
    fun `should bind and evaluate business gauges correctly`() {
        val sampleItem = ServiceOrderDynamoItem(
            id = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            status = "FINALIZED"
        )
        every { dynamoRepository.findPendingNotification() } returns listOf(sampleItem)

        binder.bindTo(registry)

        val pendingGauge = registry.find("repairshop_business_service_orders_pending_notification").gauge()
        assertNotNull(pendingGauge)
        assertEquals(1.0, pendingGauge!!.value())

        val statusGauge = registry.find("repairshop_business_service_orders_by_status")
            .tag("status", ServiceOrderStatus.IN_EXECUTION.name).gauge()
        assertNotNull(statusGauge)
        assertEquals(0.0, statusGauge!!.value())
    }

    @Test
    fun `should handle repository exceptions gracefully and return zero fallback`() {
        every { dynamoRepository.findPendingNotification() } throws RuntimeException("Dynamo error")

        binder.bindTo(registry)

        val pendingGauge = registry.find("repairshop_business_service_orders_pending_notification").gauge()
        assertEquals(0.0, pendingGauge!!.value())
    }
}
