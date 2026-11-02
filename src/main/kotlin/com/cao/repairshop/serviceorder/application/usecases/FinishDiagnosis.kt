package com.cao.repairshop.serviceorder.application.usecases

import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderResponse
import java.util.UUID

interface FinishDiagnosis {
    fun execute(id: UUID): ServiceOrderResponse
}
