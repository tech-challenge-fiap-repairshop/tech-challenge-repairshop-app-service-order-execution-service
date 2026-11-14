package com.cao.repairshop.serviceorder.infra.controller

import com.cao.repairshop.execution.infra.controller.dtos.ExecutionMetricsResponse
import com.cao.repairshop.serviceorder.application.usecases.*
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.controller.dtos.*
import com.cao.repairshop.serviceorder.infra.controller.interfaces.ServiceOrderApi
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/service-orders")
class ServiceOrderController(
    private val createServiceOrder: CreateServiceOrder,
    private val findServiceOrder: FindServiceOrder,
    private val advanceServiceOrderStatus: AdvanceServiceOrderStatus,
    private val approveServiceOrder: ApproveServiceOrder,
    private val finishDiagnosis: FinishDiagnosis,
    private val manageExtraExecutions: ManageExtraExecutions,
    private val getServiceOrderMetrics: GetServiceOrderMetrics,
    private val getExecutionMetrics: GetExecutionMetrics
) : ServiceOrderApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("", "/create_service_order")
    @ResponseStatus(HttpStatus.CREATED)
    override fun create(@Valid @RequestBody request: CreateServiceOrderRequest): ServiceOrderResponse {
        logger.info("REST POST /service-orders recebido: customerId={}, vehicleId={}", request.customerId, request.vehicleId)
        val response = createServiceOrder.execute(request)
        logger.info("REST POST /service-orders concluído com sucesso: orderId={}", response.id)
        return response
    }

    @GetMapping
    override fun findAll(
        @RequestParam(required = false) customerId: UUID?,
        @RequestParam(required = false) vehicleId: UUID?,
        @RequestParam(required = false) status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrderResponse> {
        logger.debug("REST GET /service-orders recebido com filtros: customerId={}, vehicleId={}, status={}", customerId, vehicleId, status)
        return findServiceOrder.findAll(customerId, vehicleId, status, pageable)
    }

    @GetMapping("/{id}")
    override fun findById(@PathVariable id: UUID): ServiceOrderResponse {
        logger.debug("REST GET /service-orders/{} recebido", id)
        return findServiceOrder.findById(id)
    }

    @PostMapping("/{id}/finish-diagnosis")
    override fun finishDiagnosis(@PathVariable id: UUID): ServiceOrderResponse {
        logger.info("REST POST /service-orders/{}/finish-diagnosis recebido", id)
        val response = finishDiagnosis.execute(id)
        logger.info("REST POST /service-orders/{}/finish-diagnosis concluído: status={}", id, response.status)
        return response
    }

    @PatchMapping("/{id}/status")
    override fun advanceStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ServiceOrderStatusUpdateRequest
    ): ServiceOrderResponse {
        logger.info("REST PATCH /service-orders/{}/status recebido: novo status={}", id, request.status)
        val response = advanceServiceOrderStatus.execute(id, request.status)
        logger.info("REST PATCH /service-orders/{}/status concluído: status={}", id, response.status)
        return response
    }

    @PostMapping("/{id}/approve")
    override fun approve(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ApprovalRequest
    ): ServiceOrderResponse {
        logger.info("REST POST /service-orders/{}/approve recebido: aprovado={}", id, request.approved)
        val response = approveServiceOrder.execute(id, request)
        logger.info("REST POST /service-orders/{}/approve concluído: status={}", id, response.status)
        return response
    }

    @PostMapping("/{id}/extra-executions")
    override fun addExtraExecutions(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddExtraExecutionsRequest
    ): ServiceOrderResponse {
        logger.info("REST POST /service-orders/{}/extra-executions recebido: {} tarefas extras", id, request.executions.size)
        val response = manageExtraExecutions.addExtraExecutions(id, request)
        logger.info("REST POST /service-orders/{}/extra-executions concluído: status={}", id, response.status)
        return response
    }

    @PostMapping("/{id}/extra-executions/approve")
    override fun approveExtraExecutions(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ApprovalRequest
    ): ServiceOrderResponse {
        logger.info("REST POST /service-orders/{}/extra-executions/approve recebido: aprovado={}", id, request.approved)
        val response = manageExtraExecutions.approveExtraExecutions(id, request)
        logger.info("REST POST /service-orders/{}/extra-executions/approve concluído: status={}", id, response.status)
        return response
    }

    @GetMapping("/metrics")
    override fun getMetrics(): ServiceOrderMetricsResponse =
        getServiceOrderMetrics.execute()

    @GetMapping("/executions/metrics")
    override fun getExecutionMetrics(): ExecutionMetricsResponse =
        getExecutionMetrics.execute()
}
