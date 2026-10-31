package com.cao.repairshop.execution.domain.entities

import com.cao.repairshop.core.exception.ErrorMessages
import com.cao.repairshop.core.exception.InvalidStateTransitionException
import com.cao.repairshop.execution.domain.ExecutionStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class Execution(
    val id: UUID = UUID.randomUUID(),
    var serviceOrderId: UUID,
    var basicDescription: String,
    var fullDescription: String? = null,
    var price: BigDecimal,
    var estimatedTime: BigDecimal? = null,
    var status: ExecutionStatus = ExecutionStatus.INITIATED,
    var histories: MutableSet<ExecutionHistory> = mutableSetOf(),
    var insumes: MutableSet<ExecutionInsume> = mutableSetOf(),
    var created: LocalDateTime? = null,
    var updated: LocalDateTime? = null
) {

    fun getTotalPrice(): BigDecimal {
        val insumesTotal = insumes.fold(BigDecimal.ZERO) { acc, ei ->
            acc.add(ei.insume.unityPrice.multiply(BigDecimal.valueOf(ei.quantity.toLong())))
        }
        return price.add(insumesTotal)
    }

    fun advanceStatus(newStatus: ExecutionStatus, description: String? = null) {
        if (newStatus !in status.allowedTransitions())
            throw InvalidStateTransitionException(ErrorMessages.StateTransition.invalid(status, newStatus))
        status = newStatus
        recordHistory(newStatus, description ?: newStatus.defaultMessage)
    }

    fun addInsume(insume: Insume, quantity: Int) {
        val alreadyLinked = insumes.any { it.insume.id == insume.id }
        require(!alreadyLinked) { "Insume ${insume.id} is already linked to this execution." }

        insumes.add(
            ExecutionInsume(
                executionId = id,
                insume = insume,
                quantity = quantity
            )
        )
    }

    fun recordHistory(newStatus: ExecutionStatus, description: String? = null) {
        val lastHistory = histories.maxByOrNull { it.registerTime }
        val now = LocalDateTime.now()
        val interval = lastHistory?.let { ChronoUnit.SECONDS.between(it.registerTime, now) }

        histories.add(
            ExecutionHistory(
                id = UUID.randomUUID(),
                executionId = id,
                status = newStatus,
                description = description ?: newStatus.defaultMessage,
                registerTime = now,
                intervalTime = interval
            )
        )
    }

    override fun equals(other: Any?): Boolean = other is Execution && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "Execution(id=$id, basicDescription=$basicDescription)"
}
