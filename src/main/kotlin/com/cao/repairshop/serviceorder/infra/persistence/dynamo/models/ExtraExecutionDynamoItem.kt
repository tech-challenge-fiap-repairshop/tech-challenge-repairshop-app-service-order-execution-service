package com.cao.repairshop.serviceorder.infra.persistence.dynamo.models

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant

/**
 * Representa o item_extra_executions no DynamoDB tb-service_order.
 * PK = ORDER#<service_order_id>
 * SK = EXTRA_EXEC#<timestamp>#<extra_execution_id>
 */
@DynamoDbBean
data class ExtraExecutionDynamoItem(
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("PK")
    var pk: String = "",

    @get:DynamoDbSortKey
    @get:DynamoDbAttribute("SK")
    var sk: String = "",

    @get:DynamoDbAttribute("entityType")
    var entityType: String = "EXTRA_EXECUTION",

    @get:DynamoDbAttribute("extra_execution_id")
    var extraExecutionId: String = "",

    @get:DynamoDbAttribute("service_order_id")
    var serviceOrderId: String = "",

    @get:DynamoDbAttribute("has_extra_services")
    var hasExtraServices: Boolean = true,

    @get:DynamoDbAttribute("extra_services_accepted")
    var extraServicesAccepted: Boolean? = null,

    @get:DynamoDbAttribute("status")
    var status: String = "WAITING_APPROVAL",

    @get:DynamoDbAttribute("total_extra_price")
    var totalExtraPrice: Double = 0.0,

    @get:DynamoDbAttribute("notes")
    var notes: String? = null,

    @get:DynamoDbAttribute("create_date")
    var createDate: String = Instant.now().toString(),

    @get:DynamoDbAttribute("update_date")
    var updateDate: String = Instant.now().toString(),

    @get:DynamoDbAttribute("executions")
    var executions: List<ExecutionDynamoItem> = emptyList()
)
