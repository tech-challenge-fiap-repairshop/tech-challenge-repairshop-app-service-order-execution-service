package com.cao.repairshop.core.metrics

import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BusinessMetricsBinder(
    private val dynamoRepository: DynamoServiceOrderRepository
) : MeterBinder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun bindTo(registry: MeterRegistry) {
        logger.info("Initializing Service Order & Execution Business Micrometer Metrics for DynamoDB...")

        // 1. Ordens de Serviço Pendentes de Notificação
        Gauge.builder("repairshop_business_service_orders_pending_notification") {
            runCatching { dynamoRepository.findPendingNotification().size.toDouble() }.getOrDefault(0.0)
        }
            .description("Total de ordens de serviço pendentes de notificação/despacho")
            .register(registry)

        // 2. Status Gauge
        ServiceOrderStatus.entries.forEach { status ->
            Gauge.builder("repairshop_business_service_orders_by_status") {
                0.0
            }
                .tag("status", status.name)
                .description("Métrica de monitoramento de status da OS: ${status.name}")
                .register(registry)
        }
    }
}
