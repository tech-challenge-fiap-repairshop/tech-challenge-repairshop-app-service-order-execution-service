package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.core.exception.InvalidStateTransitionException
import com.cao.repairshop.execution.domain.entities.Insume
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.ManageExtraExecutions
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.mapper.toResponse
import com.cao.repairshop.serviceorder.domain.entities.mapper.toServiceOrderHistoryEntry
import com.cao.repairshop.serviceorder.infra.controller.dtos.*
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.InsumeDynamoItem
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class ManageExtraExecutionsImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : ManageExtraExecutions {

    override fun addExtraExecutions(id: UUID, request: AddExtraExecutionsRequest): ServiceOrderResponse {
        val order = serviceOrderGateway.findDetailedById(id)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        if (order.status != ServiceOrderStatus.APPROVED && order.status != ServiceOrderStatus.IN_EXECUTION) {
            throw InvalidStateTransitionException(
                "Cannot register extra executions for Service Order in status '${order.status}'. Allowed statuses: APPROVED, IN_EXECUTION."
            )
        }

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
                pk = "ORDER#$id",
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

        val extraItem = ExtraExecutionDynamoItem(
            pk = "ORDER#$id",
            sk = "EXTRA_EXEC#$nowMillis#$extraId",
            entityType = "EXTRA_EXECUTION",
            extraExecutionId = extraId.toString(),
            serviceOrderId = id.toString(),
            hasExtraServices = true,
            extraServicesAccepted = null,
            status = "WAITING_APPROVAL",
            totalExtraPrice = batchPrice,
            notes = request.notes,
            createDate = now.toString(),
            updateDate = now.toString(),
            executions = executionDynamoItems
        )

        serviceOrderGateway.saveExtraExecution(extraItem)

        order.hasExtraServices = true
        order.extraServicesAccepted = null
        order.advanceStatus(
            ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS,
            "Tarefas extras adicionadas: ${request.notes ?: "Aguardando aprovação do cliente"}"
        )

        val saved = serviceOrderGateway.save(order)
        return mapToResponseWithExtras(saved, listOf(extraItem))
    }

    override fun approveExtraExecutions(id: UUID, request: ApprovalRequest): ServiceOrderResponse {
        val order = serviceOrderGateway.findDetailedById(id)
            ?: throw EntityNotFoundException(ErrorMessages.ServiceOrder.NOT_FOUND)

        if (order.status != ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS) {
            throw InvalidStateTransitionException(
                "Cannot approve/refuse extra executions for Service Order in status '${order.status}'. Expected: WAITING_APPROVAL_EXTRA_TASKS."
            )
        }

        val fullOrder = serviceOrderGateway.getFullOrder(id)
        val pendingExtras = fullOrder.extraExecutions.filter { it.status == "WAITING_APPROVAL" }

        if (request.approved) {
            order.hasExtraServices = true
            order.extraServicesAccepted = true
            order.advanceStatus(ServiceOrderStatus.APPROVAL_EXTRA_TASKS, "Tarefas extras aprovadas pelo cliente")

            pendingExtras.forEach { pe ->
                pe.status = "APPROVED"
                pe.extraServicesAccepted = true
                pe.updateDate = Instant.now().toString()
                serviceOrderGateway.saveExtraExecution(pe)

                // Integra tarefas extras na lista principal da OS
                pe.executions.forEach { ee ->
                    val domainInsumes = ee.insumes.map { di ->
                        Insume(
                            id = UUID.fromString(di.idInsume),
                            name = di.name,
                            brand = di.brand,
                            skuId = di.skuId,
                            quantity = di.quantity.toInt(),
                            unityPrice = BigDecimal.valueOf(di.unityInsumePrice)
                        ) to di.quantity.toInt()
                    }
                    order.addExecution(
                        basicDescription = ee.basicDescription.ifBlank { ee.taskName },
                        fullDescription = ee.fullDescription,
                        price = BigDecimal.valueOf(ee.executionPrice),
                        estimatedTime = ee.estimatedTime?.let { BigDecimal.valueOf(it) },
                        insumes = domainInsumes
                    )
                }
            }

            order.advanceStatus(ServiceOrderStatus.IN_EXECUTION, "Execução retomada com tarefas extras integradas")
        } else {
            order.hasExtraServices = true
            order.extraServicesAccepted = false
            order.advanceStatus(
                ServiceOrderStatus.REFUSED_EXTRA_TASKS,
                "Tarefas extras recusadas pelo cliente: ${request.notes ?: "Sem observações"}"
            )

            pendingExtras.forEach { pe ->
                pe.status = "REFUSED"
                pe.extraServicesAccepted = false
                pe.updateDate = Instant.now().toString()
                serviceOrderGateway.saveExtraExecution(pe)
            }

            order.advanceStatus(ServiceOrderStatus.IN_EXECUTION, "Execução retomada com os serviços originais")
        }

        val saved = serviceOrderGateway.save(order)
        return mapToResponseWithExtras(saved, fullOrder.extraExecutions)
    }

    private fun mapToResponseWithExtras(
        order: com.cao.repairshop.serviceorder.domain.entities.ServiceOrder,
        extraItems: List<ExtraExecutionDynamoItem>
    ): ServiceOrderResponse {
        val base = order.toResponse()
        val extraResponses = extraItems.map { ei ->
            ExtraExecutionResponse(
                id = UUID.fromString(ei.extraExecutionId),
                serviceOrderId = UUID.fromString(ei.serviceOrderId),
                hasExtraServices = ei.hasExtraServices,
                extraServicesAccepted = ei.extraServicesAccepted,
                status = ei.status,
                totalExtraPrice = BigDecimal.valueOf(ei.totalExtraPrice),
                notes = ei.notes,
                executions = ei.executions.map { ee ->
                    ExecutionSummary(
                        id = UUID.fromString(ee.executionId),
                        basicDescription = ee.basicDescription.ifBlank { ee.taskName },
                        fullDescription = ee.fullDescription,
                        laborPrice = BigDecimal.valueOf(ee.executionPrice),
                        insumes = ee.insumes.map { ii ->
                            InsumeSummaryResponse(
                                name = ii.name,
                                quantity = ii.quantity.toInt(),
                                unitPrice = BigDecimal.valueOf(ii.unityInsumePrice),
                                totalPrice = BigDecimal.valueOf(ii.insumePrice)
                            )
                        },
                        totalPrice = BigDecimal.valueOf(ee.executionPrice + ee.insumes.sumOf { it.insumePrice }),
                        status = ee.status
                    )
                }
            )
        }

        return base.copy(
            hasExtraServices = order.hasExtraServices,
            extraServicesAccepted = order.extraServicesAccepted,
            extraExecutions = extraResponses
        )
    }
}
