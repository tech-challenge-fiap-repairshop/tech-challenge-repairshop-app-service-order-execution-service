package com.cao.repairshop.serviceorder.infra.persistence.models

import com.cao.repairshop.execution.infra.persistence.models.ExecutionEntity
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "tb_service_order")
class ServiceOrderEntity(
    @Id
    @Column(name = "id_tb_service_order")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "customer_id", nullable = false)
    var customerId: UUID,

    @Column(name = "vehicle_id", nullable = false)
    var vehicleId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ServiceOrderStatus = ServiceOrderStatus.RECEIVED,

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    var totalPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "enter_time")
    var enterTime: LocalDateTime? = null,

    @Column(name = "end_time")
    var endTime: LocalDateTime? = null,

    @Column(name = "valid_date")
    var validDate: LocalDate? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", insertable = false, updatable = false)
    var histories: MutableSet<ServiceOrderHistoryEntity> = mutableSetOf(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order", insertable = false, updatable = false)
    var executions: MutableSet<ExecutionEntity> = mutableSetOf(),

    @CreationTimestamp
    @Column(name = "created", updatable = false)
    var created: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated")
    var updated: LocalDateTime? = null
) {
    override fun equals(other: Any?): Boolean = other is ServiceOrderEntity && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "ServiceOrderEntity(id=$id, status=$status)"
}
