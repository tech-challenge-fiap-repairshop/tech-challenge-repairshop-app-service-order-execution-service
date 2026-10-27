package com.cao.repairshop.serviceorder.domain

enum class ServiceOrderStatus(
    val defaultMessage: String
) {
    RECEIVED("Service order received and initiated"),
    IN_DIAGNOSIS("Diagnosis in progress"),
    WAITING_APPROVAL("Waiting for customer approval"),
    APPROVED("Order approved by customer"),
    REFUSED("Order refused by customer"),
    IN_EXECUTION("Service execution in progress"),
    WAITING_APPROVAL_EXTRA_TASKS("Waiting for customer approval of extra tasks"),
    APPROVAL_EXTRA_TASKS("Extra tasks approved by customer"),
    REFUSED_EXTRA_TASKS("Extra tasks refused by customer"),
    FINALIZED("All services completed and finalized"),
    PAID("Payment confirmed and invoice issued"),
    CANCELED("Service order canceled");

    fun allowedTransitions(): Set<ServiceOrderStatus> = when (this) {
        RECEIVED -> setOf(IN_DIAGNOSIS, CANCELED)
        IN_DIAGNOSIS -> setOf(WAITING_APPROVAL, CANCELED)
        WAITING_APPROVAL -> setOf(APPROVED, REFUSED, CANCELED)
        APPROVED -> setOf(IN_EXECUTION, WAITING_APPROVAL_EXTRA_TASKS, CANCELED)
        REFUSED -> setOf(CANCELED)
        IN_EXECUTION -> setOf(WAITING_APPROVAL_EXTRA_TASKS, FINALIZED, CANCELED)
        WAITING_APPROVAL_EXTRA_TASKS -> setOf(APPROVAL_EXTRA_TASKS, REFUSED_EXTRA_TASKS, CANCELED)
        APPROVAL_EXTRA_TASKS -> setOf(IN_EXECUTION)
        REFUSED_EXTRA_TASKS -> setOf(IN_EXECUTION, CANCELED)
        FINALIZED -> setOf(PAID)
        PAID -> emptySet()
        CANCELED -> emptySet()
    }
}
