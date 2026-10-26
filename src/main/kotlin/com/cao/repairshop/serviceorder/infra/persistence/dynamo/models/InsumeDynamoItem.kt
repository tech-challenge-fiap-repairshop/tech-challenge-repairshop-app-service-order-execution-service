package com.cao.repairshop.serviceorder.infra.persistence.dynamo.models

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean

/**
 * Representa os insumos embutidos na lista List<Insumes> de item_execution.
 * No desenho: id_insume, name, brand, quantity, insume_price, sku_id, unity_insume_price
 */
@DynamoDbBean
data class InsumeDynamoItem(
    @get:DynamoDbAttribute("id_insume")
    var idInsume: String = "",

    @get:DynamoDbAttribute("name")
    var name: String = "",

    @get:DynamoDbAttribute("brand")
    var brand: String = "",

    @get:DynamoDbAttribute("quantity")
    var quantity: Double = 1.0,

    @get:DynamoDbAttribute("insume_price")
    var insumePrice: Double = 0.0,

    @get:DynamoDbAttribute("sku_id")
    var skuId: String = "",

    @get:DynamoDbAttribute("unity_insume_price")
    var unityInsumePrice: Double = 0.0
)
