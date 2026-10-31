package com.cao.repairshop.serviceorder.infra.persistence.dynamo.models

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant

/**
 * Representa o item_service_order_history no DynamoDB tb-service_order.
 * PK = ORDER#<service_order_id>
 * SK = HIST#<timestamp>#<history_id>
 */
@DynamoDbBean
data class ServiceOrderHistoryDynamoItem(
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("PK")
    var pk: String = "",

    @get:DynamoDbSortKey
    @get:DynamoDbAttribute("SK")
    var sk: String = "",

    @get:DynamoDbAttribute("entityType")
    var entityType: String = "SERVICE_ORDER_HISTORY",

    @get:DynamoDbAttribute("history_id")
    var historyId: String = "",

    @get:DynamoDbAttribute("status")
    var status: String = "",

    @get:DynamoDbAttribute("register_time")
    var registerTime: String = Instant.now().toString(),

    @get:DynamoDbAttribute("interval_time")
    var intervalTime: Long? = null,

    @get:DynamoDbAttribute("description")
    var description: String? = null,

    @get:DynamoDbAttribute("payment_status")
    var paymentStatus: String? = null
)
