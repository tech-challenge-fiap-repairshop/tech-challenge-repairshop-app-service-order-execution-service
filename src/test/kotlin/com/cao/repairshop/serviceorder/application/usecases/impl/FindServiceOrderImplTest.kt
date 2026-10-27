package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.core.exception.EntityNotFoundException
import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.FullServiceOrder
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.UUID

class FindServiceOrderImplTest {

    private lateinit var serviceOrderGateway: ServiceOrderGateway
    private lateinit var findServiceOrderImpl: FindServiceOrderImpl

    @BeforeEach
    fun setup() {
        serviceOrderGateway = mockk(relaxed = true)
        findServiceOrderImpl = FindServiceOrderImpl(serviceOrderGateway)
    }

    @Test
    fun `should find service order by id successfully`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val serviceOrder = ServiceOrder(
            id = orderId,
            customerId = customerId,
            vehicleId = UUID.randomUUID(),
            enterTime = LocalDateTime.now()
        )

        every { serviceOrderGateway.findDetailedById(orderId) } returns serviceOrder
        every { serviceOrderGateway.getFullOrder(orderId) } returns FullServiceOrder(null, emptyList(), emptyList(), emptyList())

        val response = findServiceOrderImpl.findById(orderId)

        assertEquals(orderId, response.id)
        assertEquals(customerId, response.customerId)
    }

    @Test
    fun `should throw error when finding by id and order not found`() {
        every { serviceOrderGateway.findDetailedById(any()) } returns null

        assertThrows(EntityNotFoundException::class.java) {
            findServiceOrderImpl.findById(UUID.randomUUID())
        }
    }

    @Test
    fun `should find all service orders successfully`() {
        val serviceOrder = ServiceOrder(
            id = UUID.randomUUID(),
            customerId = UUID.randomUUID(),
            vehicleId = UUID.randomUUID(),
            enterTime = LocalDateTime.now()
        )
        val pageable = PageRequest.of(0, 10)
        
        every { serviceOrderGateway.findAll(null, null, null, pageable) } returns PageImpl(listOf(serviceOrder))

        val response = findServiceOrderImpl.findAll(null, null, null, pageable)

        assertEquals(1, response.totalElements)
        assertEquals(serviceOrder.id, response.content[0].id)
    }
}
