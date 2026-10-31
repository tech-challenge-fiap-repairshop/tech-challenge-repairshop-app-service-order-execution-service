package com.cao.repairshop.serviceorder.infra.persistence.dynamo.models

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant

/**
 * Representa o item_execution no DynamoDB tb-service_order.
 * PK = ORDER#<service_order_id>
 * SK = EXEC#<timestamp>#<execution_id>
 * List<Insumes> embutida
 */
@DynamoDbBean
data class ExecutionDynamoItem(
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("PK")
    var pk: String = "",

    @get:DynamoDbSortKey
    @get:DynamoDbAttribute("SK")
    var sk: String = "",

    @get:DynamoDbAttribute("entityType")
    var entityType: String = "EXECUTION",

    @get:DynamoDbAttribute("execution_id")
    var executionId: String = "",

    @get:DynamoDbAttribute("task_name")
    var taskName: String = "",

    @get:DynamoDbAttribute("execution_price")
    var executionPrice: Double = 0.0,

    @get:DynamoDbAttribute("status")
    var status: String = "INITIATED",

    @get:DynamoDbAttribute("create_date")
    var createDate: String = Instant.now().toString(),

    @get:DynamoDbAttribute("update_date")
    var updateDate: String = Instant.now().toString(),

    @get:DynamoDbAttribute("basic_description")
    var basicDescription: String = "",

    @get:DynamoDbAttribute("full_description")
    var fullDescription: String? = null,

    @get:DynamoDbAttribute("estimated_time")
    var estimatedTime: Double? = null,

    @get:DynamoDbAttribute("interval_time")
    var intervalTime: Long? = null,

    @get:DynamoDbAttribute("insumes")
    var insumes: List<InsumeDynamoItem> = emptyList()
)
