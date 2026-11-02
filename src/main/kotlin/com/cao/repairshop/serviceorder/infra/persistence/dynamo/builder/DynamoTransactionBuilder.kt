package com.cao.repairshop.serviceorder.infra.persistence.dynamo.builder

import com.cao.repairshop.serviceorder.infra.persistence.dynamo.ServiceOrderDynamoItem
import software.amazon.awssdk.services.dynamodb.model.*
import java.time.Instant
import java.util.UUID

/**
 * Utilitário responsável por construir as requisições transacionais (TransactWriteItemsRequest)
 * e atômicas de baixo nível do DynamoDB.
 */
object DynamoTransactionBuilder {

    /**
     * Constrói uma requisição de TransactWriteItems para atualizar o status da OS (em METADATA)
     * e gravar o histórico (em HIST#) de forma atômica (ACID).
     */
    fun buildUpdateStatusRequest(
        tableName: String,
        id: UUID,
        newStatus: String,
        newDispatchStatus: String? = null,
        paymentStatus: String? = null,
        hasExtraServices: Boolean? = null,
        extraServicesAccepted: Boolean? = null,
        totalPrice: Double? = null,
        historyDescription: String? = null
    ): TransactWriteItemsRequest {
        val key = mapOf(
            "PK" to AttributeValue.builder().s("ORDER#$id").build(),
            "SK" to AttributeValue.builder().s("METADATA").build()
        )

        val now = Instant.now().toString()
        val updateExpr = StringBuilder("SET #status = :newStatus, #update_date = :now")
        val names = mutableMapOf("#status" to "status", "#update_date" to "update_date")
        val values = mutableMapOf<String, AttributeValue>(
            ":newStatus" to AttributeValue.builder().s(newStatus).build(),
            ":now" to AttributeValue.builder().s(now).build()
        )

        if (newDispatchStatus != null) {
            updateExpr.append(", #pds = :pds")
            names["#pds"] = "payment_dispatch_status"
            values[":pds"] = AttributeValue.builder().s(newDispatchStatus).build()
        }

        if (paymentStatus != null) {
            updateExpr.append(", #ps = :ps")
            names["#ps"] = "payment_status"
            values[":ps"] = AttributeValue.builder().s(paymentStatus).build()
        }

        if (hasExtraServices != null) {
            updateExpr.append(", #hes = :hes")
            names["#hes"] = "has_extra_services"
            values[":hes"] = AttributeValue.builder().bool(hasExtraServices).build()
        }

        if (extraServicesAccepted != null) {
            updateExpr.append(", #esa = :esa")
            names["#esa"] = "extra_services_accepted"
            values[":esa"] = AttributeValue.builder().bool(extraServicesAccepted).build()
        }

        if (totalPrice != null) {
            updateExpr.append(", #top = :top")
            names["#top"] = "total_service_order_price"
            values[":top"] = AttributeValue.builder().n(totalPrice.toString()).build()
        }

        val histTimestamp = Instant.now().toEpochMilli()
        val histId = UUID.randomUUID().toString()
        val historyValues = mutableMapOf<String, AttributeValue>(
            "PK" to AttributeValue.builder().s("ORDER#$id").build(),
            "SK" to AttributeValue.builder().s("HIST#$histTimestamp#$histId").build(),
            "entityType" to AttributeValue.builder().s("SERVICE_ORDER_HISTORY").build(),
            "history_id" to AttributeValue.builder().s(histId).build(),
            "status" to AttributeValue.builder().s(newStatus).build(),
            "register_time" to AttributeValue.builder().s(now).build(),
            "description" to AttributeValue.builder().s(historyDescription ?: "Status transitioned to $newStatus").build()
        )
        paymentStatus?.let { historyValues["payment_status"] = AttributeValue.builder().s(it).build() }

        return TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder()
                    .update(
                        Update.builder()
                            .tableName(tableName)
                            .key(key)
                            .updateExpression(updateExpr.toString())
                            .expressionAttributeNames(names)
                            .expressionAttributeValues(values)
                            .build()
                    )
                    .build(),
                TransactWriteItem.builder()
                    .put(
                        Put.builder()
                            .tableName(tableName)
                            .item(historyValues)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    /**
     * Constrói PutItemRequest para persistência direta de ServiceOrderDynamoItem.
     */
    fun buildPutItemRequest(tableName: String, order: ServiceOrderDynamoItem): PutItemRequest {
        val item = mutableMapOf<String, AttributeValue>(
            "PK" to AttributeValue.builder().s("ORDER#${order.id}").build(),
            "SK" to AttributeValue.builder().s("METADATA").build(),
            "id" to AttributeValue.builder().s(order.id.toString()).build(),
            "service_order_id" to AttributeValue.builder().s(order.id.toString()).build(),
            "customer_id" to AttributeValue.builder().s(order.customerId.toString()).build(),
            "vehicle_id" to AttributeValue.builder().s(order.vehicleId.toString()).build(),
            "status" to AttributeValue.builder().s(order.status).build(),
            "total_price" to AttributeValue.builder().n(order.totalPrice.toString()).build(),
            "total_service_order_price" to AttributeValue.builder().n(order.totalPrice.toString()).build(),
            "payment_dispatch_status" to AttributeValue.builder().s(order.paymentDispatchStatus).build(),
            "update_date" to AttributeValue.builder().s(order.updateDate).build()
        )
        order.description?.let { item["description"] = AttributeValue.builder().s(it).build() }
        order.originTraceId?.let { item["origin_trace_id"] = AttributeValue.builder().s(it).build() }

        return PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build()
    }
}
