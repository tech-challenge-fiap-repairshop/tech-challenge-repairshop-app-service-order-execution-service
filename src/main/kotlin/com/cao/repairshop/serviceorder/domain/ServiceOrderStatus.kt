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
    FINALIZED("All services completed and finalized"),
    PAID("Payment confirmed and invoice issued"),
    CANCELED("Service order canceled");

    fun allowedTransitions(): Set<ServiceOrderStatus> = when (this) {
        RECEIVED -> setOf(IN_DIAGNOSIS)
        IN_DIAGNOSIS -> setOf(WAITING_APPROVAL)
        WAITING_APPROVAL -> setOf(APPROVED, REFUSED)
        APPROVED -> setOf(IN_EXECUTION)
        IN_EXECUTION -> setOf(FINALIZED)
        FINALIZED -> setOf(PAID)
        REFUSED -> setOf(CANCELED)
        PAID -> emptySet()
        CANCELED -> emptySet()
    }
}
