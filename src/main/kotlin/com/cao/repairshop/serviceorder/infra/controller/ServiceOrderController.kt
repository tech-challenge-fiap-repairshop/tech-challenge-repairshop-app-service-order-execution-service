package com.cao.repairshop.serviceorder.infra.controller

import com.cao.repairshop.execution.infra.controller.dtos.ExecutionMetricsResponse
import com.cao.repairshop.serviceorder.infra.controller.dtos.*
import com.cao.repairshop.serviceorder.infra.controller.interfaces.ServiceOrderApi
import com.cao.repairshop.serviceorder.application.usecases.*
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus

@RestController
@RequestMapping("/service-orders")
class ServiceOrderController(
    private val createServiceOrder: CreateServiceOrder,
    private val findServiceOrder: FindServiceOrder,
    private val advanceServiceOrderStatus: AdvanceServiceOrderStatus,
    private val approveServiceOrder: ApproveServiceOrder,
    private val getServiceOrderMetrics: GetServiceOrderMetrics,
    private val getExecutionMetrics: GetExecutionMetrics
) : ServiceOrderApi {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(@Valid @RequestBody request: CreateServiceOrderRequest): ServiceOrderResponse =
        createServiceOrder.execute(request)

    @PostMapping("/create_service_order")
    @ResponseStatus(HttpStatus.CREATED)
    fun createServiceOrderDirect(@Valid @RequestBody request: CreateServiceOrderRequest): ServiceOrderResponse =
        createServiceOrder.execute(request)

    @GetMapping
    override fun findAll(
        @RequestParam(required = false) customerId: UUID?,
        @RequestParam(required = false) vehicleId: UUID?,
        @RequestParam(required = false) status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrderResponse> {
        return findServiceOrder.findAll(customerId, vehicleId, status, pageable)
    }

    @GetMapping("/{id}")
    override fun findById(@PathVariable id: UUID): ServiceOrderResponse =
        findServiceOrder.findById(id)

    @PatchMapping("/{id}/status")
    override fun advanceStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ServiceOrderStatusUpdateRequest
    ): ServiceOrderResponse =
        advanceServiceOrderStatus.execute(id, request.status)

    @PostMapping("/{id}/approve")
    override fun approve(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ApprovalRequest
    ): ServiceOrderResponse =
        approveServiceOrder.execute(id, request)

    @GetMapping("/metrics")
    override fun getMetrics(): ServiceOrderMetricsResponse =
        getServiceOrderMetrics.execute()

    @GetMapping("/executions/metrics")
    override fun getExecutionMetrics(): ExecutionMetricsResponse =
        getExecutionMetrics.execute()
}
