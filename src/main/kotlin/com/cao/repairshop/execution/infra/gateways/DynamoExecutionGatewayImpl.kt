package com.cao.repairshop.execution.infra.gateways

import com.cao.repairshop.execution.application.gateways.ExecutionGateway
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.InsumeDynamoItem
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
@Primary
class DynamoExecutionGatewayImpl(
    private val dynamoRepository: DynamoServiceOrderRepository
) : ExecutionGateway {

    override fun save(execution: Execution): Execution {
        val timestamp = (execution.created ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC).toEpochMilli()
        val insumesItems = execution.insumes.map { ei ->
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

        val item = ExecutionDynamoItem(
            pk = "ORDER#${execution.serviceOrderId}",
            sk = "EXEC#$timestamp#${execution.id}",
            entityType = "EXECUTION",
            executionId = execution.id.toString(),
            taskName = execution.basicDescription,
            executionPrice = execution.price.toDouble(),
            status = execution.status.name,
            createDate = (execution.created ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC).toString(),
            updateDate = Instant.now().toString(),
            basicDescription = execution.basicDescription,
            fullDescription = execution.fullDescription,
            estimatedTime = execution.estimatedTime?.toDouble(),
            insumes = insumesItems
        )
        dynamoRepository.saveExecution(item)
        return execution
    }

    override fun findById(id: UUID): Execution? {
        return null
    }

    override fun findByServiceOrderId(serviceOrderId: UUID): List<Execution> {
        val fullOrder = dynamoRepository.getFullOrder(serviceOrderId)
        return fullOrder.executions.map { ei ->
            val exec = Execution(
                id = UUID.fromString(ei.executionId),
                serviceOrderId = serviceOrderId,
                basicDescription = ei.basicDescription.ifBlank { ei.taskName },
                fullDescription = ei.fullDescription,
                price = BigDecimal.valueOf(ei.executionPrice),
                estimatedTime = ei.estimatedTime?.let { BigDecimal.valueOf(it) },
                status = runCatching { ExecutionStatus.valueOf(ei.status) }.getOrDefault(ExecutionStatus.INITIATED)
            )
            ei.insumes.forEach { ins ->
                exec.addInsume(
                    Insume(
                        id = UUID.fromString(ins.idInsume.ifBlank { UUID.randomUUID().toString() }),
                        name = ins.name,
                        brand = ins.brand.ifBlank { null },
                        skuId = ins.skuId,
                        quantity = ins.quantity.toInt(),
                        unityPrice = BigDecimal.valueOf(if (ins.unityInsumePrice > 0.0) ins.unityInsumePrice else ins.insumePrice)
                    ),
                    ins.quantity.toInt()
                )
            }
            exec
        }
    }

    override fun delete(id: UUID) {
        // no-op
    }

    override fun getAverageExecutionTimeMinutes(): Double? = 30.0
}
