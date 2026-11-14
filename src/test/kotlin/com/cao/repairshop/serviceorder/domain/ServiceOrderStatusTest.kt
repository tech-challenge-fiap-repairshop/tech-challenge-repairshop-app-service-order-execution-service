package com.cao.repairshop.serviceorder.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServiceOrderStatusTest {

    @Test
    fun `RECEIVED allows IN_DIAGNOSIS and CANCELED`() {
        assertEquals(setOf(ServiceOrderStatus.IN_DIAGNOSIS, ServiceOrderStatus.CANCELED), ServiceOrderStatus.RECEIVED.allowedTransitions())
    }

    @Test
    fun `IN_DIAGNOSIS allows WAITING_APPROVAL and CANCELED`() {
        assertEquals(setOf(ServiceOrderStatus.WAITING_APPROVAL, ServiceOrderStatus.CANCELED), ServiceOrderStatus.IN_DIAGNOSIS.allowedTransitions())
    }

    @Test
    fun `WAITING_APPROVAL allows APPROVED, REFUSED, and CANCELED`() {
        assertEquals(setOf(ServiceOrderStatus.APPROVED, ServiceOrderStatus.REFUSED, ServiceOrderStatus.CANCELED), ServiceOrderStatus.WAITING_APPROVAL.allowedTransitions())
    }

    @Test
    fun `APPROVED allows IN_EXECUTION, WAITING_APPROVAL_EXTRA_TASKS, and CANCELED`() {
        assertEquals(setOf(ServiceOrderStatus.IN_EXECUTION, ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS, ServiceOrderStatus.CANCELED), ServiceOrderStatus.APPROVED.allowedTransitions())
    }

    @Test
    fun `REFUSED allows only CANCELED`() {
        assertEquals(setOf(ServiceOrderStatus.CANCELED), ServiceOrderStatus.REFUSED.allowedTransitions())
    }

    @Test
    fun `IN_EXECUTION allows WAITING_APPROVAL_EXTRA_TASKS, FINALIZED, and CANCELED`() {
        assertEquals(setOf(ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS, ServiceOrderStatus.FINALIZED, ServiceOrderStatus.CANCELED), ServiceOrderStatus.IN_EXECUTION.allowedTransitions())
    }

    @Test
    fun `WAITING_APPROVAL_EXTRA_TASKS allows APPROVAL_EXTRA_TASKS, REFUSED_EXTRA_TASKS, and CANCELED`() {
        assertEquals(setOf(ServiceOrderStatus.APPROVAL_EXTRA_TASKS, ServiceOrderStatus.REFUSED_EXTRA_TASKS, ServiceOrderStatus.CANCELED), ServiceOrderStatus.WAITING_APPROVAL_EXTRA_TASKS.allowedTransitions())
    }

    @Test
    fun `APPROVAL_EXTRA_TASKS allows IN_EXECUTION`() {
        assertEquals(setOf(ServiceOrderStatus.IN_EXECUTION), ServiceOrderStatus.APPROVAL_EXTRA_TASKS.allowedTransitions())
    }

    @Test
    fun `REFUSED_EXTRA_TASKS allows IN_EXECUTION and CANCELED`() {
        assertEquals(setOf(ServiceOrderStatus.IN_EXECUTION, ServiceOrderStatus.CANCELED), ServiceOrderStatus.REFUSED_EXTRA_TASKS.allowedTransitions())
    }

    @Test
    fun `FINALIZED allows only PAID`() {
        assertEquals(setOf(ServiceOrderStatus.PAID), ServiceOrderStatus.FINALIZED.allowedTransitions())
    }

    @Test
    fun `PAID is terminal and allows nothing`() {
        assertEquals(emptySet<ServiceOrderStatus>(), ServiceOrderStatus.PAID.allowedTransitions())
    }

    @Test
    fun `CANCELED is terminal and allows nothing`() {
        assertEquals(emptySet<ServiceOrderStatus>(), ServiceOrderStatus.CANCELED.allowedTransitions())
    }
}
