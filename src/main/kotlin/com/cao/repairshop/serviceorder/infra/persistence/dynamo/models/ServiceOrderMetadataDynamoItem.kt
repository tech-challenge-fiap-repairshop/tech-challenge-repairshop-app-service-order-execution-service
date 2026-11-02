package com.cao.repairshop.serviceorder.infra.persistence.dynamo.models

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant

/**
 * Representa o item_service_order_metadata no DynamoDB tb-service_order.
 * Mapeamento fiel do desenho de arquitetura da Fase 4:
 * PK = ORDER#<service_order_id>
 * SK = METADATA
 * GSI pending-dispatch-index: PK = payment_dispatch_status, SK = update_date
 */
@DynamoDbBean
data class ServiceOrderMetadataDynamoItem(
    @get:DynamoDbPartitionKey
    @get:DynamoDbAttribute("PK")
    var pk: String = "",

    @get:DynamoDbSortKey
    @get:DynamoDbAttribute("SK")
    var sk: String = "METADATA",

    @get:DynamoDbAttribute("entityType")
    var entityType: String = "SERVICE_ORDER_METADATA",

    @get:DynamoDbAttribute("service_order_id")
    var serviceOrderId: String = "",

    @get:DynamoDbAttribute("customer_id")
    var customerId: String = "",

    @get:DynamoDbAttribute("vehicle_id")
    var vehicleId: String = "",

    @get:DynamoDbAttribute("total_service_order_price")
    var totalServiceOrderPrice: Double = 0.0,

    @get:DynamoDbAttribute("enter_time")
    var enterTime: String = Instant.now().toString(),

    @get:DynamoDbAttribute("end_time")
    var endTime: String? = null,

    @get:DynamoDbAttribute("create_date")
    var createDate: String = Instant.now().toString(),

    @get:DynamoDbSecondarySortKey(indexNames = ["pending-dispatch-index"])
    @get:DynamoDbAttribute("update_date")
    var updateDate: String = Instant.now().toString(),

    @get:DynamoDbAttribute("status")
    var status: String = "IN_DIAGNOSIS",

    @get:DynamoDbAttribute("valid_date")
    var validDate: String? = null,

    @get:DynamoDbAttribute("payment_status")
    var paymentStatus: String = "PENDING",

    @get:DynamoDbSecondaryPartitionKey(indexNames = ["pending-dispatch-index"])
    @get:DynamoDbAttribute("payment_dispatch_status")
    var paymentDispatchStatus: String = "PENDING",

    @get:DynamoDbAttribute("has_extra_services")
    var hasExtraServices: Boolean = false,

    @get:DynamoDbAttribute("extra_services_accepted")
    var extraServicesAccepted: Boolean? = null,

    @get:DynamoDbAttribute("description")
    var description: String? = null,

    @get:DynamoDbAttribute("origin_trace_id")
    var originTraceId: String? = null
)
