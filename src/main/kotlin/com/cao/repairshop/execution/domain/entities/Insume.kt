package com.cao.repairshop.execution.domain.entities

import java.math.BigDecimal
import java.util.UUID

data class Insume(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val brand: String? = null,
    val skuId: String? = null,
    val quantity: Int = 1,
    val unityPrice: BigDecimal = BigDecimal.ZERO
) {
    val totalPrice: BigDecimal get() = unityPrice.multiply(BigDecimal.valueOf(quantity.toLong()))
}
