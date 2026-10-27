package com.cao.repairshop.serviceorder.application.gateways

import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ServiceOrderGateway {
    fun save(order: ServiceOrder): ServiceOrder
    fun findDetailedById(id: UUID): ServiceOrder?
    fun findAll(
        customerId: UUID?,
        vehicleId: UUID?,
        status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrder>
    fun saveExtraExecution(extraItem: ExtraExecutionDynamoItem)
    fun getFullOrder(orderId: UUID): FullServiceOrder
    fun updateStatus(
        id: UUID,
        newStatus: String,
        newDispatchStatus: String? = null,
        paymentStatus: String? = null,
        hasExtraServices: Boolean? = null,
        extraServicesAccepted: Boolean? = null,
        totalPrice: Double? = null
    )
    fun existsByCustomerId(customerId: UUID): Boolean
    fun existsByVehicleId(vehicleId: UUID): Boolean
    fun getAverageExecutionTimeMinutes(): Double?
}
