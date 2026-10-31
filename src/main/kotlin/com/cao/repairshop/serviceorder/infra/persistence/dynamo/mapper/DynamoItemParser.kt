package com.cao.repairshop.serviceorder.infra.persistence.dynamo.mapper

import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.ServiceOrderDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.*
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.Instant
import java.util.UUID

/**
 * Componente responsável pelo parsing e desserialização de atributos brutos do DynamoDB
 * para as classes de modelo fortemente tipadas.
 */
@Component
class DynamoItemParser {

    /**
     * Mapeia os itens retornados pela Single-Table Query (PK = ORDER#id) para FullServiceOrder.
     */
    fun parseFullOrder(orderId: UUID, items: List<Map<String, AttributeValue>>): FullServiceOrder {
        if (items.isEmpty()) {
            return FullServiceOrder(null, emptyList(), emptyList(), emptyList())
        }

        val pk = "ORDER#$orderId"
        var metadata: ServiceOrderMetadataDynamoItem? = null
        val histories = mutableListOf<ServiceOrderHistoryDynamoItem>()
        val executions = mutableListOf<ExecutionDynamoItem>()
        val extraExecutions = mutableListOf<ExtraExecutionDynamoItem>()

        for (item in items) {
            val sk = item["SK"]?.s() ?: ""
            when {
                sk == "METADATA" -> metadata = parseMetadata(pk, sk, orderId, item)
                sk.startsWith("HIST#") -> histories.add(parseHistory(pk, sk, item))
                sk.startsWith("EXEC#") -> executions.add(parseExecution(pk, sk, item))
                sk.startsWith("EXTRA_EXEC#") -> extraExecutions.add(parseExtraExecution(pk, sk, orderId, item))
            }
        }

        return FullServiceOrder(metadata, histories, executions, extraExecutions)
    }

    fun parseMetadata(
        pk: String,
        sk: String,
        orderId: UUID,
        item: Map<String, AttributeValue>
    ): ServiceOrderMetadataDynamoItem = ServiceOrderMetadataDynamoItem(
        pk = pk,
        sk = sk,
        entityType = item["entityType"]?.s() ?: "SERVICE_ORDER_METADATA",
        serviceOrderId = item["service_order_id"]?.s() ?: orderId.toString(),
        customerId = item["customer_id"]?.s() ?: "",
        vehicleId = item["vehicle_id"]?.s() ?: "",
        totalServiceOrderPrice = item["total_service_order_price"]?.n()?.toDouble()
            ?: item["total_price"]?.n()?.toDouble() ?: 0.0,
        enterTime = item["enter_time"]?.s() ?: "",
        endTime = item["end_time"]?.s(),
        createDate = item["create_date"]?.s() ?: "",
        updateDate = item["update_date"]?.s() ?: "",
        status = item["status"]?.s() ?: "IN_DIAGNOSIS",
        validDate = item["valid_date"]?.s(),
        paymentStatus = item["payment_status"]?.s() ?: "PENDING",
        paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
        hasExtraServices = item["has_extra_services"]?.bool() ?: false,
        extraServicesAccepted = item["extra_services_accepted"]?.bool(),
        description = item["description"]?.s(),
        originTraceId = item["origin_trace_id"]?.s()
    )

    fun parseHistory(
        pk: String,
        sk: String,
        item: Map<String, AttributeValue>
    ): ServiceOrderHistoryDynamoItem = ServiceOrderHistoryDynamoItem(
        pk = pk,
        sk = sk,
        entityType = item["entityType"]?.s() ?: "SERVICE_ORDER_HISTORY",
        historyId = item["history_id"]?.s() ?: UUID.randomUUID().toString(),
        status = item["status"]?.s() ?: "",
        registerTime = item["register_time"]?.s() ?: "",
        intervalTime = item["interval_time"]?.n()?.toLong(),
        description = item["description"]?.s(),
        paymentStatus = item["payment_status"]?.s()
    )

    fun parseExecution(
        pk: String,
        sk: String,
        item: Map<String, AttributeValue>
    ): ExecutionDynamoItem = ExecutionDynamoItem(
        pk = pk,
        sk = sk,
        entityType = item["entityType"]?.s() ?: "EXECUTION",
        executionId = item["execution_id"]?.s() ?: UUID.randomUUID().toString(),
        taskName = item["task_name"]?.s() ?: "",
        executionPrice = item["execution_price"]?.n()?.toDouble() ?: 0.0,
        status = item["status"]?.s() ?: "INITIATED",
        createDate = item["create_date"]?.s() ?: "",
        updateDate = item["update_date"]?.s() ?: "",
        basicDescription = item["basic_description"]?.s() ?: "",
        fullDescription = item["full_description"]?.s(),
        estimatedTime = item["estimated_time"]?.n()?.toDouble(),
        intervalTime = item["interval_time"]?.n()?.toLong(),
        insumes = parseInsumes(item["insumes"])
    )

    fun parseExtraExecution(
        pk: String,
        sk: String,
        orderId: UUID,
        item: Map<String, AttributeValue>
    ): ExtraExecutionDynamoItem {
        val rawExecs = item["executions"]?.l() ?: emptyList()
        val extraExecList = rawExecs.mapNotNull { ev ->
            val m = ev.m() ?: return@mapNotNull null
            ExecutionDynamoItem(
                pk = pk,
                sk = m["SK"]?.s() ?: "",
                entityType = m["entityType"]?.s() ?: "EXECUTION",
                executionId = m["execution_id"]?.s() ?: UUID.randomUUID().toString(),
                taskName = m["task_name"]?.s() ?: "",
                executionPrice = m["execution_price"]?.n()?.toDouble() ?: 0.0,
                status = m["status"]?.s() ?: "INITIATED",
                createDate = m["create_date"]?.s() ?: "",
                updateDate = m["update_date"]?.s() ?: "",
                basicDescription = m["basic_description"]?.s() ?: "",
                fullDescription = m["full_description"]?.s(),
                estimatedTime = m["estimated_time"]?.n()?.toDouble(),
                intervalTime = m["interval_time"]?.n()?.toLong(),
                insumes = parseInsumes(m["insumes"])
            )
        }

        return ExtraExecutionDynamoItem(
            pk = pk,
            sk = sk,
            entityType = item["entityType"]?.s() ?: "EXTRA_EXECUTION",
            extraExecutionId = item["extra_execution_id"]?.s() ?: UUID.randomUUID().toString(),
            serviceOrderId = item["service_order_id"]?.s() ?: orderId.toString(),
            hasExtraServices = item["has_extra_services"]?.bool() ?: true,
            extraServicesAccepted = item["extra_services_accepted"]?.bool(),
            status = item["status"]?.s() ?: "WAITING_APPROVAL",
            totalExtraPrice = item["total_extra_price"]?.n()?.toDouble() ?: 0.0,
            notes = item["notes"]?.s(),
            createDate = item["create_date"]?.s() ?: "",
            updateDate = item["update_date"]?.s() ?: "",
            executions = extraExecList
        )
    }

    fun parseInsumes(insumesAttribute: AttributeValue?): List<InsumeDynamoItem> {
        return insumesAttribute?.l()?.mapNotNull { iv ->
            val m = iv.m() ?: return@mapNotNull null
            InsumeDynamoItem(
                idInsume = m["id_insume"]?.s() ?: "",
                name = m["name"]?.s() ?: "",
                brand = m["brand"]?.s() ?: "",
                quantity = m["quantity"]?.n()?.toDouble() ?: 1.0,
                insumePrice = m["insume_price"]?.n()?.toDouble() ?: 0.0,
                skuId = m["sku_id"]?.s() ?: "",
                unityInsumePrice = m["unity_insume_price"]?.n()?.toDouble() ?: 0.0
            )
        } ?: emptyList()
    }

    /**
     * Mapeia os itens retornados pela consulta ao GSI pending-dispatch-index.
     */
    fun parsePendingNotifications(items: List<Map<String, AttributeValue>>): List<ServiceOrderDynamoItem> {
        return items.mapNotNull { item ->
            val idStr = item["id"]?.s() ?: item["service_order_id"]?.s() ?: return@mapNotNull null
            val customerIdStr = item["customer_id"]?.s() ?: return@mapNotNull null
            val vehicleIdStr = item["vehicle_id"]?.s() ?: return@mapNotNull null
            ServiceOrderDynamoItem(
                id = UUID.fromString(idStr),
                customerId = UUID.fromString(customerIdStr),
                vehicleId = UUID.fromString(vehicleIdStr),
                status = item["status"]?.s() ?: "",
                totalPrice = item["total_service_order_price"]?.n()?.toDouble()
                    ?: item["total_price"]?.n()?.toDouble() ?: 0.0,
                description = item["description"]?.s(),
                paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
                updateDate = item["update_date"]?.s() ?: Instant.now().toString(),
                originTraceId = item["origin_trace_id"]?.s()
            )
        }
    }

    /**
     * Mapeia um único item GetItem do DynamoDB para ServiceOrderDynamoItem.
     */
    fun parseServiceOrderItem(id: UUID, item: Map<String, AttributeValue>): ServiceOrderDynamoItem {
        return ServiceOrderDynamoItem(
            id = id,
            customerId = UUID.fromString(item["customer_id"]?.s() ?: UUID.randomUUID().toString()),
            vehicleId = UUID.fromString(item["vehicle_id"]?.s() ?: UUID.randomUUID().toString()),
            status = item["status"]?.s() ?: "",
            totalPrice = item["total_price"]?.n()?.toDouble()
                ?: item["total_service_order_price"]?.n()?.toDouble() ?: 0.0,
            description = item["description"]?.s(),
            paymentDispatchStatus = item["payment_dispatch_status"]?.s() ?: "PENDING",
            updateDate = item["update_date"]?.s() ?: "",
            originTraceId = item["origin_trace_id"]?.s()
        )
    }
}
