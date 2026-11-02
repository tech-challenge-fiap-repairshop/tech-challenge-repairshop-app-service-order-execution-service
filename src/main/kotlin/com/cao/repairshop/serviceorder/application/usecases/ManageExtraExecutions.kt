package com.cao.repairshop.serviceorder.application.usecases

import com.cao.repairshop.serviceorder.infra.controller.dtos.AddExtraExecutionsRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ApprovalRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import java.util.UUID

interface ManageExtraExecutions {
    fun addExtraExecutions(id: UUID, request: AddExtraExecutionsRequest): ServiceOrderResponse
    fun approveExtraExecutions(id: UUID, request: ApprovalRequest): ServiceOrderResponse
}
