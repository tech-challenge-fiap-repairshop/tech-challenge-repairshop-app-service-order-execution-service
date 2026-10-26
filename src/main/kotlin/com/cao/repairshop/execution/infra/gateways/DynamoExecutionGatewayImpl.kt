package com.cao.repairshop.execution.infra.gateways

import com.cao.repairshop.execution.application.gateways.ExecutionGateway
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.inventory.domain.entities.Insume
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
                brand = "GENERIC",
                quantity = ei.quantity.toDouble(),
                insumePrice = ei.insume.price.toDouble(),
                skuId = "SKU-${ei.insume.id.toString().take(8)}",
                unityInsumePrice = ei.insume.price.toDouble()
            )
        }

        val item = ExecutionDynamoItem(
            pk = "ORDER#${execution.serviceOrderId}",
            sk = "EXEC#$timestamp#${execution.id}",
            entityType = "EXECUTION",
            executionId = execution.id.toString(),
            taskName = execution.basicDescription.name,
            executionPrice = execution.price.toDouble(),
            status = execution.status.name,
            createDate = (execution.created ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC).toString(),
            updateDate = Instant.now().toString(),
            basicDescription = execution.basicDescription.name,
            fullDescription = execution.fullDescription,
            estimatedTime = execution.estimatedTime?.toDouble(),
            insumes = insumesItems
        )
        dynamoRepository.saveExecution(item)
        return execution
    }

    override fun findById(id: UUID): Execution? {
        // Busca na OS correspondente
        return null
    }

    override fun findByServiceOrderId(serviceOrderId: UUID): List<Execution> {
        val (_, _, executions) = dynamoRepository.getFullOrder(serviceOrderId)
        return executions.map { ei ->
            val exec = Execution(
                id = UUID.fromString(ei.executionId),
                serviceOrderId = serviceOrderId,
                basicDescription = runCatching { com.cao.repairshop.execution.domain.BasicExecution.valueOf(ei.basicDescription) }.getOrDefault(com.cao.repairshop.execution.domain.BasicExecution.OTHER),
                fullDescription = ei.fullDescription,
                price = BigDecimal.valueOf(ei.executionPrice),
                estimatedTime = ei.estimatedTime?.let { BigDecimal.valueOf(it) },
                status = runCatching { com.cao.repairshop.execution.domain.ExecutionStatus.valueOf(ei.status) }.getOrDefault(com.cao.repairshop.execution.domain.ExecutionStatus.INITIATED)
            )
            ei.insumes.forEach { ins ->
                exec.addInsume(
                    Insume(
                        id = UUID.fromString(ins.idInsume.ifBlank { UUID.randomUUID().toString() }),
                        name = ins.name,
                        brand = ins.brand.ifBlank { null },
                        skuId = ins.skuId,
                        quantity = ins.quantity.toInt(),
                        price = BigDecimal.valueOf(ins.insumePrice),
                        unityPrice = BigDecimal.valueOf(if (ins.unityInsumePrice > 0.0) ins.unityInsumePrice else ins.insumePrice)
                    ),
                    ins.quantity.toInt()
                )
            }
            exec
        }
    }

    override fun delete(id: UUID) {
        // soft delete ou no-op no Dynamo
    }

    override fun getAverageExecutionTimeMinutes(): Double? = 30.0
}
