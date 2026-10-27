package com.cao.repairshop.serviceorder.infra.gateways

import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrderHistory
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.InsumeDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderHistoryDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderMetadataDynamoItem
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
@Primary
class DynamoServiceOrderGatewayImpl(
    private val dynamoRepository: DynamoServiceOrderRepository
) : ServiceOrderGateway {

    override fun save(order: ServiceOrder): ServiceOrder {
        // 1. Salva item_service_order_metadata
        val meta = ServiceOrderMetadataDynamoItem(
            pk = "ORDER#${order.id}",
            sk = "METADATA",
            entityType = "SERVICE_ORDER_METADATA",
            serviceOrderId = order.id.toString(),
            customerId = order.customerId.toString(),
            vehicleId = order.vehicleId.toString(),
            totalServiceOrderPrice = order.totalPrice.toDouble(),
            enterTime = order.enterTime?.toInstant(ZoneOffset.UTC)?.toString() ?: Instant.now().toString(),
            endTime = order.endTime?.toInstant(ZoneOffset.UTC)?.toString(),
            createDate = order.created?.toInstant(ZoneOffset.UTC)?.toString() ?: Instant.now().toString(),
            updateDate = Instant.now().toString(),
            status = order.status.name,
            validDate = order.validDate?.toString(),
            paymentStatus = if (order.status == ServiceOrderStatus.PAID) "PAID" else "PENDING",
            paymentDispatchStatus = if (order.status == ServiceOrderStatus.FINALIZED) "PENDING" else "DISPATCHED",
            hasExtraServices = order.hasExtraServices,
            extraServicesAccepted = order.extraServicesAccepted
        )
        dynamoRepository.saveMetadata(meta)

        // 2. Salva item_service_order_history
        order.histories.forEach { h ->
            val timestamp = h.registerTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            val historyItem = ServiceOrderHistoryDynamoItem(
                pk = "ORDER#${order.id}",
                sk = "HIST#$timestamp#${h.id}",
                entityType = "SERVICE_ORDER_HISTORY",
                historyId = h.id.toString(),
                status = h.status.name,
                registerTime = h.registerTime.toInstant(ZoneOffset.UTC).toString(),
                intervalTime = h.intervalTime,
                description = h.description,
                paymentStatus = if (h.status == ServiceOrderStatus.PAID) "PAID" else null
            )
            dynamoRepository.saveHistory(historyItem)
        }

        // 3. Salva item_execution com List<Insumes> embutida
        order.executions.forEach { exec ->
            val timestamp = (exec.created ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC).toEpochMilli()
            val insumesItems = exec.insumes.map { ei ->
                InsumeDynamoItem(
                    idInsume = ei.insume.id.toString(),
                    name = ei.insume.name,
                    brand = ei.insume.brand ?: "GENERIC",
                    quantity = ei.quantity.toDouble(),
                    insumePrice = ei.insume.totalPrice.toDouble(),
                    skuId = ei.insume.skuId ?: "SKU-${ei.insume.id.toString().take(8)}",
                    unityInsumePrice = ei.insume.unityPrice.toDouble()
                )
            }

            val execItem = ExecutionDynamoItem(
                pk = "ORDER#${order.id}",
                sk = "EXEC#$timestamp#${exec.id}",
                entityType = "EXECUTION",
                executionId = exec.id.toString(),
                taskName = exec.basicDescription,
                executionPrice = exec.price.toDouble(),
                status = exec.status.name,
                createDate = (exec.created ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC).toString(),
                updateDate = Instant.now().toString(),
                basicDescription = exec.basicDescription,
                fullDescription = exec.fullDescription,
                estimatedTime = exec.estimatedTime?.toDouble(),
                insumes = insumesItems
            )
            dynamoRepository.saveExecution(execItem)
        }

        return order
    }

    override fun findDetailedById(id: UUID): ServiceOrder? {
        val fullOrder = dynamoRepository.getFullOrder(id)
        val metadata = fullOrder.metadata ?: return null

        val order = ServiceOrder(
            id = UUID.fromString(metadata.serviceOrderId),
            customerId = UUID.fromString(metadata.customerId),
            vehicleId = UUID.fromString(metadata.vehicleId),
            status = runCatching { ServiceOrderStatus.valueOf(metadata.status) }.getOrDefault(ServiceOrderStatus.IN_DIAGNOSIS),
            totalPrice = BigDecimal.valueOf(metadata.totalServiceOrderPrice),
            hasExtraServices = metadata.hasExtraServices,
            extraServicesAccepted = metadata.extraServicesAccepted
        )

        // Reconstrói execuções
        fullOrder.executions.forEach { ei ->
            val exec = Execution(
                id = UUID.fromString(ei.executionId),
                serviceOrderId = order.id,
                basicDescription = ei.basicDescription.ifBlank { ei.taskName },
                fullDescription = ei.fullDescription,
                price = BigDecimal.valueOf(ei.executionPrice),
                estimatedTime = ei.estimatedTime?.let { BigDecimal.valueOf(it) },
                status = runCatching { ExecutionStatus.valueOf(ei.status) }.getOrDefault(ExecutionStatus.INITIATED)
            )

            ei.insumes.forEach { ins ->
                val insumeEntity = Insume(
                    id = UUID.fromString(ins.idInsume.ifBlank { UUID.randomUUID().toString() }),
                    name = ins.name,
                    brand = ins.brand.ifBlank { null },
                    skuId = ins.skuId,
                    quantity = ins.quantity.toInt(),
                    unityPrice = BigDecimal.valueOf(if (ins.unityInsumePrice > 0.0) ins.unityInsumePrice else ins.insumePrice)
                )
                exec.addInsume(insumeEntity, ins.quantity.toInt())
            }

            order.executions.add(exec)
        }

        // Reconstrói histórico
        fullOrder.histories.forEach { h ->
            val status = runCatching { ServiceOrderStatus.valueOf(h.status) }.getOrDefault(ServiceOrderStatus.IN_DIAGNOSIS)
            order.histories.add(
                ServiceOrderHistory(
                    id = UUID.fromString(h.historyId),
                    serviceOrderId = order.id,
                    status = status,
                    description = h.description,
                    registerTime = LocalDateTime.now(),
                    intervalTime = h.intervalTime
                )
            )
        }

        return order
    }

    override fun findAll(
        customerId: UUID?,
        vehicleId: UUID?,
        status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrder> {
        val items = dynamoRepository.findPendingNotification().mapNotNull { findDetailedById(it.id) }
        return PageImpl(items, pageable, items.size.toLong())
    }

    override fun saveExtraExecution(extraItem: ExtraExecutionDynamoItem) {
        dynamoRepository.saveExtraExecution(extraItem)
    }

    override fun getFullOrder(orderId: UUID) = dynamoRepository.getFullOrder(orderId)

    override fun updateStatus(
        id: UUID,
        newStatus: String,
        newDispatchStatus: String?,
        paymentStatus: String?,
        hasExtraServices: Boolean?,
        extraServicesAccepted: Boolean?,
        totalPrice: Double?
    ) {
        dynamoRepository.updateStatus(
            id = id,
            newStatus = newStatus,
            newDispatchStatus = newDispatchStatus,
            paymentStatus = paymentStatus,
            hasExtraServices = hasExtraServices,
            extraServicesAccepted = extraServicesAccepted,
            totalPrice = totalPrice
        )
    }

    override fun existsByCustomerId(customerId: UUID): Boolean = true
    override fun existsByVehicleId(vehicleId: UUID): Boolean = true
    override fun getAverageExecutionTimeMinutes(): Double? = 45.0
}
