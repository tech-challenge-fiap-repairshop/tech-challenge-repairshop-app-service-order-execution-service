package com.cao.repairshop.serviceorder.infra.gateways

import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.domain.entities.mapper.toDomain
import com.cao.repairshop.serviceorder.domain.entities.mapper.toEntity
import com.cao.repairshop.serviceorder.infra.persistence.repositories.ServiceOrderRepository
import com.cao.repairshop.serviceorder.infra.persistence.specifications.ServiceOrderSpecifications
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.*

@Component
class ServiceOrderGatewayImplJPA(
    private val serviceOrderRepository: ServiceOrderRepository
) : ServiceOrderGateway {

    override fun save(order: ServiceOrder): ServiceOrder {
        val entity = order.toEntity()
        val saved = serviceOrderRepository.save(entity)
        return saved.toDomain()
    }

    override fun findDetailedById(id: UUID): ServiceOrder? {
        return serviceOrderRepository.findDetailedById(id).orElse(null)?.toDomain()
    }

    override fun findAll(
        customerId: UUID?,
        vehicleId: UUID?,
        status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrder> {
        val spec = ServiceOrderSpecifications.withFilters(customerId, vehicleId, status)
        val finalSpec = ServiceOrderSpecifications.withCustomOrderingAndFilters(spec, pageable)
        return serviceOrderRepository.findAll(finalSpec, pageable).map { it.toDomain() }
    }

    override fun existsByCustomerId(customerId: UUID): Boolean {
        return serviceOrderRepository.existsByCustomerId(customerId)
    }

    override fun existsByVehicleId(vehicleId: UUID): Boolean {
        return serviceOrderRepository.existsByVehicleId(vehicleId)
    }

    override fun getAverageExecutionTimeMinutes(): Double? {
        return serviceOrderRepository.getAverageExecutionTimeMinutes()
    }
}