package com.cao.repairshop.serviceorder.infra.persistence.specifications

import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.persistence.models.ServiceOrderEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime
import java.util.UUID

object ServiceOrderSpecifications {

    fun withFilters(
        customerId: UUID?,
        vehicleId: UUID?,
        status: ServiceOrderStatus?
    ): Specification<ServiceOrderEntity> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

            customerId?.let {
                predicates.add(cb.equal(root.get<UUID>("customerId"), it))
            }
            vehicleId?.let {
                predicates.add(cb.equal(root.get<UUID>("vehicleId"), it))
            }
            if (status != null) {
                predicates.add(cb.equal(root.get<ServiceOrderStatus>("status"), status))
            } else {
                predicates.add(cb.not(root.get<ServiceOrderStatus>("status").`in`(
                    ServiceOrderStatus.FINALIZED,
                    ServiceOrderStatus.PAID,
                    ServiceOrderStatus.CANCELED
                )))
            }

            cb.and(*predicates.toTypedArray())
        }
    }

    fun withCustomOrderingAndFilters(
        spec: Specification<ServiceOrderEntity>?,
        pageable: Pageable
    ): Specification<ServiceOrderEntity> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()

            if (spec != null) {
                spec.toPredicate(root, query, cb)?.let { predicates.add(it) }
            } else {
                val statusExpression = root.get<ServiceOrderStatus>("status")
                predicates.add(
                    cb.not(
                        statusExpression.`in`(
                            ServiceOrderStatus.FINALIZED,
                            ServiceOrderStatus.PAID,
                            ServiceOrderStatus.CANCELED
                        )
                    )
                )
            }

            if (pageable.sort.isUnsorted && query.resultType != Long::class.java && query.resultType != java.lang.Long::class.java) {
                val statusExpression = root.get<ServiceOrderStatus>("status")
                val statusCase = cb.selectCase<Int>()
                    .`when`(cb.equal(statusExpression, ServiceOrderStatus.IN_EXECUTION), 1)
                    .`when`(cb.equal(statusExpression, ServiceOrderStatus.WAITING_APPROVAL), 2)
                    .`when`(cb.equal(statusExpression, ServiceOrderStatus.IN_DIAGNOSIS), 3)
                    .`when`(cb.equal(statusExpression, ServiceOrderStatus.RECEIVED), 4)
                    .otherwise(5)

                query.orderBy(
                    cb.asc(statusCase),
                    cb.asc(root.get<LocalDateTime>("enterTime"))
                )
            }

            if (predicates.isEmpty()) cb.conjunction() else cb.and(*predicates.toTypedArray())
        }
    }
}
