package com.cao.repairshop.serviceorder.application.usecases.impl

import com.cao.repairshop.serviceorder.application.gateways.ServiceOrderGateway
import com.cao.repairshop.serviceorder.application.usecases.GetServiceOrderMetrics
import com.cao.repairshop.serviceorder.infra.controller.dtos.ServiceOrderMetricsResponse
import org.springframework.stereotype.Service

@Service
class GetServiceOrderMetricsImpl(
    private val serviceOrderGateway: ServiceOrderGateway
) : GetServiceOrderMetrics {

    override fun execute(): ServiceOrderMetricsResponse {
        val avgMinutes = serviceOrderGateway.getAverageExecutionTimeMinutes()
        return ServiceOrderMetricsResponse(averageExecutionTimeMinutes = avgMinutes)
    }
}
