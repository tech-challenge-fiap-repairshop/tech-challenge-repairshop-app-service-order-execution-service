package com.cao.repairshop.serviceorder.infra.gateways

import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.models.ServiceOrderEntity
import com.cao.repairshop.serviceorder.infra.persistence.repositories.ServiceOrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.util.Optional
import java.util.UUID

class ServiceOrderGatewayImplJPATest {

    private lateinit var serviceOrderRepository: ServiceOrderRepository
    private lateinit var serviceOrderGatewayImplJPA: ServiceOrderGatewayImplJPA

    @BeforeEach
    fun setup() {
        serviceOrderRepository = mockk()
        serviceOrderGatewayImplJPA = ServiceOrderGatewayImplJPA(serviceOrderRepository)
    }

    @Test
    fun `should save service order successfully`() {
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()
        val serviceOrder = mockk<ServiceOrder>(relaxed = true)
        every { serviceOrder.customerId } returns customerId
        every { serviceOrder.vehicleId } returns vehicleId

        val serviceOrderEntity = mockk<ServiceOrderEntity>(relaxed = true)
        every { serviceOrderEntity.id } returns UUID.randomUUID()
        every { serviceOrderEntity.customerId } returns customerId
        every { serviceOrderEntity.vehicleId } returns vehicleId
        every { serviceOrderRepository.save(any()) } returns serviceOrderEntity

        val result = serviceOrderGatewayImplJPA.save(serviceOrder)

        assertNotNull(result)
        verify { serviceOrderRepository.save(any()) }
    }

    @Test
    fun `should find detailed by id`() {
        val id = UUID.randomUUID()
        val serviceOrderEntity = mockk<ServiceOrderEntity>(relaxed = true)
        every { serviceOrderEntity.id } returns id
        every { serviceOrderEntity.customerId } returns UUID.randomUUID()
        every { serviceOrderEntity.vehicleId } returns UUID.randomUUID()
        every { serviceOrderRepository.findDetailedById(id) } returns Optional.of(serviceOrderEntity)

        val result = serviceOrderGatewayImplJPA.findDetailedById(id)

        assertNotNull(result)
        assertEquals(id, result?.id)
        verify { serviceOrderRepository.findDetailedById(id) }
    }

    @Test
    fun `should find all with pagination`() {
        val pageable: Pageable = PageRequest.of(0, 10)
        val entity = mockk<ServiceOrderEntity>(relaxed = true)
        every { entity.customerId } returns UUID.randomUUID()
        every { entity.vehicleId } returns UUID.randomUUID()
        val page = PageImpl(listOf(entity))

        every { serviceOrderRepository.findAll(any<Specification<ServiceOrderEntity>>(), pageable) } returns page

        val result = serviceOrderGatewayImplJPA.findAll(null, null, null, pageable)

        assertNotNull(result)
        assertEquals(1, result.totalElements)
    }

    @Test
    fun `should check existsByCustomerId`() {
        val id = UUID.randomUUID()
        every { serviceOrderRepository.existsByCustomerId(id) } returns true

        assertTrue(serviceOrderGatewayImplJPA.existsByCustomerId(id))
    }

    @Test
    fun `should check existsByVehicleId`() {
        val id = UUID.randomUUID()
        every { serviceOrderRepository.existsByVehicleId(id) } returns true

        assertTrue(serviceOrderGatewayImplJPA.existsByVehicleId(id))
    }

    @Test
    fun `should get average execution time minutes`() {
        every { serviceOrderRepository.getAverageExecutionTimeMinutes() } returns 120.0

        val result = serviceOrderGatewayImplJPA.getAverageExecutionTimeMinutes()

        assertEquals(120.0, result)
    }
}
