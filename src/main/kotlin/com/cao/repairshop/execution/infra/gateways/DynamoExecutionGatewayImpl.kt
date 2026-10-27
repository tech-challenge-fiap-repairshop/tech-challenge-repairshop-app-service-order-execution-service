package com.cao.repairshop.execution.infra.gateways

import com.cao.repairshop.execution.application.gateways.ExecutionGateway
import com.cao.repairshop.execution.domain.entities.Execution
import com.cao.repairshop.serviceorder.infra.gateways.mapper.ServiceOrderDynamoMapper
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Primary
class DynamoExecutionGatewayImpl(
    private val dynamoRepository: DynamoServiceOrderRepository,
    private val mapper: ServiceOrderDynamoMapper = ServiceOrderDynamoMapper()
) : ExecutionGateway {

    override fun save(execution: Execution): Execution {
        val item = mapper.buildExecutionItem(execution, "ORDER#${execution.serviceOrderId}")
        dynamoRepository.saveExecution(item)
        return execution
    }

    override fun findById(id: UUID): Execution? = null

    override fun findByServiceOrderId(serviceOrderId: UUID): List<Execution> {
        val fullOrder = dynamoRepository.getFullOrder(serviceOrderId)
        return fullOrder.executions.map { mapper.toDomainExecution(it, serviceOrderId) }
    }

    override fun delete(id: UUID) {
        // no-op
    }

    override fun getAverageExecutionTimeMinutes(): Double? = 30.0
}
