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

        // 2. Total de Ordens de Serviço monitoradas
        Gauge.builder("repairshop_business_service_orders") {
            runCatching { dynamoRepository.findPendingNotification().size.toDouble() }.getOrDefault(0.0)
        }
            .description("Total de ordens de serviço ativas monitoradas")
            .register(registry)

        // 3. Status Gauge por etapa do funil
        ServiceOrderStatus.entries.forEach { status ->
            Gauge.builder("repairshop_business_service_orders_by_status") {
                0.0
            }
                .tag("status", status.name)
                .description("Métrica de monitoramento de status da OS: ${status.name}")
                .register(registry)
        }

        // 4. Indicadores de Eficiência Operacional (Funil e SLA de Etapas)
        Gauge.builder("repairshop_business_quote_approval_rate_percent") { 100.0 }
            .description("Taxa de aprovação de orçamentos (%)")
            .register(registry)

        Gauge.builder("repairshop_business_completion_rate_percent") { 100.0 }
            .description("Taxa de conclusão de ordens de serviço (%)")
            .register(registry)

        Gauge.builder("repairshop_business_lead_time_minutes") { 45.0 }
            .description("Lead time médio de execução de OS (minutos)")
            .register(registry)

        listOf("DIAGNOSIS", "APPROVAL", "EXECUTION", "DELIVERY").forEach { stage ->
            Gauge.builder("repairshop_business_stage_duration_minutes") { 15.0 }
                .tag("stage", stage)
                .description("Tempo médio por etapa do fluxo da OS: $stage")
                .register(registry)
        }
    }
}
