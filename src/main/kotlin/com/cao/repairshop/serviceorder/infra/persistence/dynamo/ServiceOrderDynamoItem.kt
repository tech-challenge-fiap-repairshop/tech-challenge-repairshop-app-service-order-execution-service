package com.cao.repairshop.serviceorder.infra.persistence.dynamo

import java.time.Instant
import java.util.UUID

data class ServiceOrderDynamoItem(
    val id: UUID,
    val customerId: UUID,
    val vehicleId: UUID,
    val status: String,
    val totalPrice: Double = 0.0,
    val description: String? = null,
    val paymentDispatchStatus: String = "PENDING",
    val updateDate: String = Instant.now().toString(),
    val originTraceId: String? = null
)
