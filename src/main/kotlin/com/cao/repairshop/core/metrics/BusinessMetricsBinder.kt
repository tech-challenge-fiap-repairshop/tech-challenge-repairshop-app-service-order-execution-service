package com.cao.repairshop.core.metrics

import com.cao.repairshop.inventory.infra.persistence.repositories.InsumeRepository
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.persistence.repositories.ServiceOrderRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BusinessMetricsBinder(
    private val serviceOrderRepository: ServiceOrderRepository,
    private val insumeRepository: InsumeRepository
) : MeterBinder {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun bindTo(registry: MeterRegistry) {
        logger.info("Initializing Service Order & Execution Business Micrometer Metrics...")

        // 1. Ordens de Serviço Totais
        Gauge.builder("repairshop_business_service_orders_total") {
            runCatching { serviceOrderRepository.count().toDouble() }.getOrDefault(0.0)
        }
            .description("Total geral de ordens de serviço criadas")
            .register(registry)

        // 2. Quantidade de OS por Etapa do Funil (Status)
        ServiceOrderStatus.entries.forEach { status ->
            Gauge.builder("repairshop_business_service_orders_by_status") {
                runCatching { serviceOrderRepository.countByStatus(status).toDouble() }.getOrDefault(0.0)
            }
                .tag("status", status.name)
                .description("Quantidade de ordens de serviço no status ${status.name}")
                .register(registry)
        }

        // 3. Tempo Médio Entre Etapas (em minutos)
        val stageTransitions = listOf(
            "RECEIVED" to "IN_DIAGNOSIS",
            "IN_DIAGNOSIS" to "WAITING_APPROVAL",
            "WAITING_APPROVAL" to "APPROVED",
            "WAITING_APPROVAL" to "REFUSED",
            "APPROVED" to "IN_EXECUTION",
            "IN_EXECUTION" to "FINALIZED",
            "FINALIZED" to "PAID"
        )

        stageTransitions.forEach { (fromStatus, toStatus) ->
            Gauge.builder("repairshop_business_stage_duration_minutes") {
                runCatching {
                    serviceOrderRepository.getAverageStageDurationMinutes(fromStatus, toStatus) ?: 0.0
                }.getOrDefault(0.0)
            }
                .tag("from_status", fromStatus)
                .tag("to_status", toStatus)
                .description("Tempo médio em minutos entre as etapas $fromStatus e $toStatus")
                .register(registry)
        }

        // 4. Lead Time Completo de Atendimento (RECEIVED -> PAID)
        Gauge.builder("repairshop_business_lead_time_minutes") {
            runCatching {
                serviceOrderRepository.getAverageLeadTimeMinutes() ?: 0.0
            }.getOrDefault(0.0)
        }
            .description("Lead time médio total de atendimento da OS (RECEIVED -> PAID) em minutos")
            .register(registry)

        // 5. Taxa de Conclusão de Serviços
        Gauge.builder("repairshop_business_completion_rate_percent") {
            runCatching {
                val total = serviceOrderRepository.count()
                val completed = serviceOrderRepository.countByStatus(ServiceOrderStatus.FINALIZED) +
                        serviceOrderRepository.countByStatus(ServiceOrderStatus.PAID)
                if (total > 0) (completed.toDouble() / total.toDouble()) * 100.0 else 0.0
            }.getOrDefault(0.0)
        }
            .description("Taxa percentual de conclusão e entrega de ordens de serviço")
            .register(registry)

        // 6. Gestão de Estoque e Almoxarifado
        Gauge.builder("repairshop_business_inventory_items_total") {
            runCatching {
                insumeRepository.getTotalStockQuantity()?.toDouble() ?: 0.0
            }.getOrDefault(0.0)
        }
            .description("Quantidade total de itens de insumos em estoque")
            .register(registry)

        Gauge.builder("repairshop_business_inventory_value_total_brl") {
            runCatching {
                insumeRepository.getTotalInventoryValue()?.toDouble() ?: 0.0
            }.getOrDefault(0.0)
        }
            .description("Valor patrimonial total em estoque em Reais (R$)")
            .register(registry)
    }
}
