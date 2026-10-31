package com.cao.repairshop.serviceorder.domain

import com.cao.repairshop.serviceorder.domain.entities.ServiceOrder
import org.springframework.stereotype.Component

@Component
class ApprovalDomainService {

    fun approve(order: ServiceOrder) {
        order.approve()
        order.advanceStatus(ServiceOrderStatus.IN_EXECUTION)
    }

    fun refuse(order: ServiceOrder) {
        order.refuse()
    }
}
