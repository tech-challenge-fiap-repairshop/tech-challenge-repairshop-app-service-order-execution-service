package com.cao.repairshop.serviceorder.infra.persistence.specifications

import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.persistence.models.ServiceOrderEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.criteria.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime
import java.util.UUID

class ServiceOrderSpecificationsTest {

    @Test
    fun `should apply default status filters and custom ordering when spec is null and unsorted`() {
        val root = mockk<Root<ServiceOrderEntity>>()
        val query = mockk<CriteriaQuery<*>>()
        val cb = mockk<CriteriaBuilder>()
        val pageable = mockk<Pageable>()

        // Status Path and In expression mock
        val statusPath = mockk<Path<ServiceOrderStatus>>()
        val inExpression = mockk<CriteriaBuilder.In<ServiceOrderStatus>>()
        val notPredicate = mockk<Predicate>()
        val andPredicate = mockk<Predicate>()

        every { root.get<ServiceOrderStatus>("status") } returns statusPath
        every {
            statusPath.`in`(
                ServiceOrderStatus.FINALIZED,
                ServiceOrderStatus.PAID,
                ServiceOrderStatus.CANCELED
            )
        } returns inExpression
        every { cb.not(inExpression) } returns notPredicate
        every { cb.and(notPredicate) } returns andPredicate

        // Pageable Sort mock
        val sort = mockk<Sort>()
        every { pageable.sort } returns sort
        every { sort.isUnsorted } returns true
        every { query.resultType } returns ServiceOrderEntity::class.java

        // Equal Predicates inside ordering case
        val equalPredicate1 = mockk<Predicate>()
        val equalPredicate2 = mockk<Predicate>()
        val equalPredicate3 = mockk<Predicate>()
        val equalPredicate4 = mockk<Predicate>()

        every { cb.equal(statusPath, ServiceOrderStatus.IN_EXECUTION) } returns equalPredicate1
        every { cb.equal(statusPath, ServiceOrderStatus.WAITING_APPROVAL) } returns equalPredicate2
        every { cb.equal(statusPath, ServiceOrderStatus.IN_DIAGNOSIS) } returns equalPredicate3
        every { cb.equal(statusPath, ServiceOrderStatus.RECEIVED) } returns equalPredicate4

        val selectCase = mockk<CriteriaBuilder.Case<Int>>()
        every { cb.selectCase<Int>() } returns selectCase
        every { selectCase.`when`(equalPredicate1, 1) } returns selectCase
        every { selectCase.`when`(equalPredicate2, 2) } returns selectCase
        every { selectCase.`when`(equalPredicate3, 3) } returns selectCase
        every { selectCase.`when`(equalPredicate4, 4) } returns selectCase
        every { selectCase.otherwise(5) } returns selectCase

        // EnterTime path mock
        val enterTimePath = mockk<Path<LocalDateTime>>()
        every { root.get<LocalDateTime>("enterTime") } returns enterTimePath

        val orderStatusCase = mockk<Order>()
        val orderEnterTime = mockk<Order>()
        every { cb.asc(selectCase) } returns orderStatusCase
        every { cb.asc(enterTimePath) } returns orderEnterTime
        every { query.orderBy(orderStatusCase, orderEnterTime) } returns query

        val spec = ServiceOrderSpecifications.withCustomOrderingAndFilters(null, pageable)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        assertSame(andPredicate, result)

        verify(exactly = 1) {
            query.orderBy(orderStatusCase, orderEnterTime)
        }
    }

    @Test
    fun `should apply custom spec predicate and custom ordering when spec is provided and pageable is unsorted`() {
        val root = mockk<Root<ServiceOrderEntity>>()
        val query = mockk<CriteriaQuery<*>>()
        val cb = mockk<CriteriaBuilder>()
        val pageable = mockk<Pageable>()
        val innerSpec = mockk<Specification<ServiceOrderEntity>>()

        val customPredicate = mockk<Predicate>()
        val andPredicate = mockk<Predicate>()

        every { innerSpec.toPredicate(root, query, cb) } returns customPredicate
        every { cb.and(customPredicate) } returns andPredicate

        val sort = mockk<Sort>()
        every { pageable.sort } returns sort
        every { sort.isUnsorted } returns true
        every { query.resultType } returns ServiceOrderEntity::class.java

        // Status Path & Equal Predicates inside ordering case
        val statusPath = mockk<Path<ServiceOrderStatus>>()
        val equalPredicate1 = mockk<Predicate>()
        val equalPredicate2 = mockk<Predicate>()
        val equalPredicate3 = mockk<Predicate>()
        val equalPredicate4 = mockk<Predicate>()

        every { root.get<ServiceOrderStatus>("status") } returns statusPath
        every { cb.equal(statusPath, ServiceOrderStatus.IN_EXECUTION) } returns equalPredicate1
        every { cb.equal(statusPath, ServiceOrderStatus.WAITING_APPROVAL) } returns equalPredicate2
        every { cb.equal(statusPath, ServiceOrderStatus.IN_DIAGNOSIS) } returns equalPredicate3
        every { cb.equal(statusPath, ServiceOrderStatus.RECEIVED) } returns equalPredicate4

        val selectCase = mockk<CriteriaBuilder.Case<Int>>()
        every { cb.selectCase<Int>() } returns selectCase
        every { selectCase.`when`(equalPredicate1, 1) } returns selectCase
        every { selectCase.`when`(equalPredicate2, 2) } returns selectCase
        every { selectCase.`when`(equalPredicate3, 3) } returns selectCase
        every { selectCase.`when`(equalPredicate4, 4) } returns selectCase
        every { selectCase.otherwise(5) } returns selectCase

        // EnterTime path mock
        val enterTimePath = mockk<Path<LocalDateTime>>()
        every { root.get<LocalDateTime>("enterTime") } returns enterTimePath

        val orderStatusCase = mockk<Order>()
        val orderEnterTime = mockk<Order>()
        every { cb.asc(selectCase) } returns orderStatusCase
        every { cb.asc(enterTimePath) } returns orderEnterTime
        every { query.orderBy(orderStatusCase, orderEnterTime) } returns query

        val spec = ServiceOrderSpecifications.withCustomOrderingAndFilters(innerSpec, pageable)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        assertSame(andPredicate, result)

        verify(exactly = 1) {
            query.orderBy(orderStatusCase, orderEnterTime)
        }
    }

    @Test
    fun `should NOT apply custom ordering when pageable is sorted`() {
        val root = mockk<Root<ServiceOrderEntity>>()
        val query = mockk<CriteriaQuery<*>>()
        val cb = mockk<CriteriaBuilder>()
        val pageable = mockk<Pageable>()

        // Status Path and In expression mock
        val statusPath = mockk<Path<ServiceOrderStatus>>()
        val inExpression = mockk<CriteriaBuilder.In<ServiceOrderStatus>>()
        val notPredicate = mockk<Predicate>()
        val andPredicate = mockk<Predicate>()

        every { root.get<ServiceOrderStatus>("status") } returns statusPath
        every {
            statusPath.`in`(
                ServiceOrderStatus.FINALIZED,
                ServiceOrderStatus.PAID,
                ServiceOrderStatus.CANCELED
            )
        } returns inExpression
        every { cb.not(inExpression) } returns notPredicate
        every { cb.and(notPredicate) } returns andPredicate

        // Pageable Sort mock: isUnsorted is false
        val sort = mockk<Sort>()
        every { pageable.sort } returns sort
        every { sort.isUnsorted } returns false

        val spec = ServiceOrderSpecifications.withCustomOrderingAndFilters(null, pageable)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        assertSame(andPredicate, result)

        verify(exactly = 0) {
            query.orderBy(any<List<Order>>())
            query.orderBy(*anyVararg())
        }
    }

    @Test
    fun `should NOT apply custom ordering when query resultType is Long`() {
        val root = mockk<Root<ServiceOrderEntity>>()
        val query = mockk<CriteriaQuery<*>>()
        val cb = mockk<CriteriaBuilder>()
        val pageable = mockk<Pageable>()

        // Status Path and In expression mock
        val statusPath = mockk<Path<ServiceOrderStatus>>()
        val inExpression = mockk<CriteriaBuilder.In<ServiceOrderStatus>>()
        val notPredicate = mockk<Predicate>()
        val andPredicate = mockk<Predicate>()

        every { root.get<ServiceOrderStatus>("status") } returns statusPath
        every {
            statusPath.`in`(
                ServiceOrderStatus.FINALIZED,
                ServiceOrderStatus.PAID,
                ServiceOrderStatus.CANCELED
            )
        } returns inExpression
        every { cb.not(inExpression) } returns notPredicate
        every { cb.and(notPredicate) } returns andPredicate

        // Pageable Sort mock: isUnsorted is true, but query resultType is Long
        val sort = mockk<Sort>()
        every { pageable.sort } returns sort
        every { sort.isUnsorted } returns true
        every { query.resultType } returns Long::class.java

        val spec = ServiceOrderSpecifications.withCustomOrderingAndFilters(null, pageable)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        assertSame(andPredicate, result)

        verify(exactly = 0) {
            query.orderBy(any<List<Order>>())
            query.orderBy(*anyVararg())
        }
    }

    @Test
    fun `should NOT apply custom ordering when query resultType is java Long`() {
        val root = mockk<Root<ServiceOrderEntity>>()
        val query = mockk<CriteriaQuery<*>>()
        val cb = mockk<CriteriaBuilder>()
        val pageable = mockk<Pageable>()

        // Status Path and In expression mock
        val statusPath = mockk<Path<ServiceOrderStatus>>()
        val inExpression = mockk<CriteriaBuilder.In<ServiceOrderStatus>>()
        val notPredicate = mockk<Predicate>()
        val andPredicate = mockk<Predicate>()

        every { root.get<ServiceOrderStatus>("status") } returns statusPath
        every {
            statusPath.`in`(
                ServiceOrderStatus.FINALIZED,
                ServiceOrderStatus.PAID,
                ServiceOrderStatus.CANCELED
            )
        } returns inExpression
        every { cb.not(inExpression) } returns notPredicate
        every { cb.and(notPredicate) } returns andPredicate

        // Pageable Sort mock: isUnsorted is true, but query resultType is java.lang.Long
        val sort = mockk<Sort>()
        every { pageable.sort } returns sort
        every { sort.isUnsorted } returns true
        every { query.resultType } returns java.lang.Long::class.java

        val spec = ServiceOrderSpecifications.withCustomOrderingAndFilters(null, pageable)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        assertSame(andPredicate, result)

        verify(exactly = 0) {
            query.orderBy(any<List<Order>>())
            query.orderBy(*anyVararg())
        }
    }

    @Test
    fun `should return conjunction when spec returns null predicate and predicates list is empty`() {
        val root = mockk<Root<ServiceOrderEntity>>()
        val query = mockk<CriteriaQuery<*>>()
        val cb = mockk<CriteriaBuilder>()
        val pageable = mockk<Pageable>()
        val innerSpec = mockk<Specification<ServiceOrderEntity>>()

        every { innerSpec.toPredicate(root, query, cb) } returns null

        val conjunctionPredicate = mockk<Predicate>()
        every { cb.conjunction() } returns conjunctionPredicate

        val sort = mockk<Sort>()
        every { pageable.sort } returns sort
        every { sort.isUnsorted } returns true
        every { query.resultType } returns ServiceOrderEntity::class.java

        // Status Path & Equal Predicates inside ordering case
        val statusPath = mockk<Path<ServiceOrderStatus>>()
        val equalPredicate1 = mockk<Predicate>()
        val equalPredicate2 = mockk<Predicate>()
        val equalPredicate3 = mockk<Predicate>()
        val equalPredicate4 = mockk<Predicate>()

        every { root.get<ServiceOrderStatus>("status") } returns statusPath
        every { cb.equal(statusPath, ServiceOrderStatus.IN_EXECUTION) } returns equalPredicate1
        every { cb.equal(statusPath, ServiceOrderStatus.WAITING_APPROVAL) } returns equalPredicate2
        every { cb.equal(statusPath, ServiceOrderStatus.IN_DIAGNOSIS) } returns equalPredicate3
        every { cb.equal(statusPath, ServiceOrderStatus.RECEIVED) } returns equalPredicate4

        val selectCase = mockk<CriteriaBuilder.Case<Int>>()
        every { cb.selectCase<Int>() } returns selectCase
        every { selectCase.`when`(equalPredicate1, 1) } returns selectCase
        every { selectCase.`when`(equalPredicate2, 2) } returns selectCase
        every { selectCase.`when`(equalPredicate3, 3) } returns selectCase
        every { selectCase.`when`(equalPredicate4, 4) } returns selectCase
        every { selectCase.otherwise(5) } returns selectCase

        // EnterTime path mock
        val enterTimePath = mockk<Path<LocalDateTime>>()
        every { root.get<LocalDateTime>("enterTime") } returns enterTimePath

        val orderStatusCase = mockk<Order>()
        val orderEnterTime = mockk<Order>()
        every { cb.asc(selectCase) } returns orderStatusCase
        every { cb.asc(enterTimePath) } returns orderEnterTime
        every { query.orderBy(orderStatusCase, orderEnterTime) } returns query

        val spec = ServiceOrderSpecifications.withCustomOrderingAndFilters(innerSpec, pageable)
        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        assertSame(conjunctionPredicate, result)

        verify(exactly = 1) {
            query.orderBy(orderStatusCase, orderEnterTime)
        }
    }

    @Test
    fun `should build default status specification when no parameters are provided`() {
        val spec = ServiceOrderSpecifications.withFilters(null, null, null)

        val root = mockk<Root<ServiceOrderEntity>>()
        val query = mockk<CriteriaQuery<*>>()
        val cb = mockk<CriteriaBuilder>()

        val statusPath = mockk<Path<ServiceOrderStatus>>()
        val inExpression = mockk<CriteriaBuilder.In<ServiceOrderStatus>>()
        val notPredicate = mockk<Predicate>()
        val andPredicate = mockk<Predicate>()

        every { root.get<ServiceOrderStatus>("status") } returns statusPath
        every {
            statusPath.`in`(
                ServiceOrderStatus.FINALIZED,
                ServiceOrderStatus.PAID,
                ServiceOrderStatus.CANCELED
            )
        } returns inExpression
        every { cb.not(inExpression) } returns notPredicate
        every { cb.and(notPredicate) } returns andPredicate

        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        assertSame(andPredicate, result)
    }

    @Test
    fun `should build specification with customer, vehicle and status predicates when all parameters are provided`() {
        val customerId = UUID.randomUUID()
        val vehicleId = UUID.randomUUID()
        val spec = ServiceOrderSpecifications.withFilters(customerId, vehicleId, ServiceOrderStatus.RECEIVED)

        val root = mockk<Root<ServiceOrderEntity>>()
        val query = mockk<CriteriaQuery<*>>()
        val cb = mockk<CriteriaBuilder>()

        val customerIdPath = mockk<Path<UUID>>()
        val vehicleIdPath = mockk<Path<UUID>>()
        val statusPath = mockk<Path<ServiceOrderStatus>>()

        every { root.get<UUID>("customerId") } returns customerIdPath
        every { root.get<UUID>("vehicleId") } returns vehicleIdPath
        every { root.get<ServiceOrderStatus>("status") } returns statusPath

        val eqCustomer = mockk<Predicate>()
        val eqVehicle = mockk<Predicate>()
        val eqStatus = mockk<Predicate>()
        val andPredicate = mockk<Predicate>()

        every { cb.equal(customerIdPath, customerId) } returns eqCustomer
        every { cb.equal(vehicleIdPath, vehicleId) } returns eqVehicle
        every { cb.equal(statusPath, ServiceOrderStatus.RECEIVED) } returns eqStatus
        every { cb.and(eqCustomer, eqVehicle, eqStatus) } returns andPredicate

        val result = spec.toPredicate(root, query, cb)

        assertNotNull(result)
        assertSame(andPredicate, result)
    }
}
