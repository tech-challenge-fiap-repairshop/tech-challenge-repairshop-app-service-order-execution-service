package com.cao.repairshop.serviceorder.infra.gateways.mapper

import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrderHistory
import com.cao.repairshop.serviceorder.infra.controller.dtos.AddExtraExecutionsRequest
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.InsumeDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderHistoryDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderMetadataDynamoItem
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Mapper bidirecional responsável pela conversão entre Entidades de Domínio
 * (ServiceOrder, Execution, Insume) e Itens DynamoDB (Metadata, History, Execution, Insume).
 * Elimina duplicação entre DynamoServiceOrderGatewayImpl e DynamoExecutionGatewayImpl.
 */
@Component
class ServiceOrderDynamoMapper {

    fun buildMetadataItem(order: ServiceOrder, pk: String): ServiceOrderMetadataDynamoItem {
        return ServiceOrderMetadataDynamoItem(
            pk = pk,
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
    }

    fun buildHistoryItems(order: ServiceOrder, pk: String): List<ServiceOrderHistoryDynamoItem> {
        return order.histories.map { h ->
            val timestamp = h.registerTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            ServiceOrderHistoryDynamoItem(
                pk = pk,
                sk = "HIST#$timestamp#${h.id}",
                entityType = "SERVICE_ORDER_HISTORY",
                historyId = h.id.toString(),
                status = h.status.name,
                registerTime = h.registerTime.toInstant(ZoneOffset.UTC).toString(),
                intervalTime = h.intervalTime,
                description = h.description,
                paymentStatus = if (h.status == ServiceOrderStatus.PAID) "PAID" else null
            )
        }
    }

    fun buildExecutionItems(order: ServiceOrder, pk: String): List<ExecutionDynamoItem> {
        return order.executions.map { exec -> buildExecutionItem(exec, pk) }
    }

    fun buildExecutionItem(exec: Execution, pk: String): ExecutionDynamoItem {
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

        return ExecutionDynamoItem(
            pk = pk,
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
    }

    fun buildExtraExecutionItem(
        serviceOrderId: UUID,
        request: AddExtraExecutionsRequest
    ): ExtraExecutionDynamoItem {
        val extraId = UUID.randomUUID()
        val now = Instant.now()
        val nowMillis = now.toEpochMilli()

        var batchPrice = 0.0
        val executionDynamoItems = request.executions.map { ed ->
            val execId = UUID.randomUUID()
            val insumesItems = ed.insumes.map { ii ->
                val qty = ii.quantity.toDouble()
                val unitP = ii.unityPrice.toDouble()
                val insPrice = qty * unitP
                batchPrice += insPrice
                InsumeDynamoItem(
                    idInsume = (ii.insumeId ?: UUID.randomUUID()).toString(),
                    name = ii.name,
                    brand = ii.brand ?: "GENERIC",
                    quantity = qty,
                    insumePrice = insPrice,
                    skuId = ii.skuId ?: "SKU-${ii.name.take(4).uppercase()}",
                    unityInsumePrice = unitP
                )
            }

            val laborPrice = (ed.price ?: BigDecimal.ZERO).toDouble()
            batchPrice += laborPrice

            ExecutionDynamoItem(
                pk = "ORDER#$serviceOrderId",
                sk = "EXTRA_EXEC#$nowMillis#$execId",
                entityType = "EXECUTION",
                executionId = execId.toString(),
                taskName = ed.basicDescription,
                executionPrice = laborPrice,
                status = "INITIATED",
                createDate = now.toString(),
                updateDate = now.toString(),
                basicDescription = ed.basicDescription,
                fullDescription = ed.fullDescription,
                estimatedTime = ed.estimatedTime?.toDouble(),
                insumes = insumesItems
            )
        }

        return ExtraExecutionDynamoItem(
            pk = "ORDER#$serviceOrderId",
            sk = "EXTRA_EXEC#$nowMillis#$extraId",
            entityType = "EXTRA_EXECUTION",
            extraExecutionId = extraId.toString(),
            serviceOrderId = serviceOrderId.toString(),
            hasExtraServices = true,
            extraServicesAccepted = null,
            status = "WAITING_APPROVAL",
            totalExtraPrice = batchPrice,
            notes = request.notes,
            createDate = now.toString(),
            updateDate = now.toString(),
            executions = executionDynamoItems
        )
    }

    fun toDomainOrder(fullOrder: FullServiceOrder): ServiceOrder? {
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

        // Reconstrói execuções técnicas e seus insumos embutidos
        fullOrder.executions.forEach { ei ->
            order.executions.add(toDomainExecution(ei, order.id))
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

    fun toDomainExecution(ei: ExecutionDynamoItem, orderId: UUID): Execution {
        val exec = Execution(
            id = UUID.fromString(ei.executionId),
            serviceOrderId = orderId,
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

        return exec
    }
}
