package com.cao.repairshop.serviceorder.infra.controller.interfaces

import com.cao.repairshop.execution.infra.controller.dtos.ExecutionMetricsResponse
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import com.cao.repairshop.serviceorder.infra.controller.dtos.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

@Tag(name = "Service Orders", description = "Service order lifecycle management")
interface ServiceOrderApi {

    @Operation(summary = "Open a new service order (initial state: IN_DIAGNOSIS)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Service order created",
            content = [Content(schema = Schema(implementation = ServiceOrderResponse::class))]),
        ApiResponse(responseCode = "400", description = "Validation error", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Customer or vehicle not found", content = [Content()])
    )
    fun create(request: CreateServiceOrderRequest): ServiceOrderResponse

    @Operation(summary = "List all service orders with pagination and filtering")
    @ApiResponses(ApiResponse(responseCode = "200", description = "Paginated service order list"))
    fun findAll(
        customerId: UUID?,
        vehicleId: UUID?,
        status: ServiceOrderStatus?,
        pageable: Pageable
    ): Page<ServiceOrderResponse>

    @Operation(summary = "Find service order by ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Service order found",
            content = [Content(schema = Schema(implementation = ServiceOrderResponse::class))]),
        ApiResponse(responseCode = "404", description = "Service order not found", content = [Content()])
    )
    fun findById(id: UUID): ServiceOrderResponse

    @Operation(summary = "Finish diagnosis and send order to WAITING_APPROVAL")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Diagnosis finished, order moved to WAITING_APPROVAL",
            content = [Content(schema = Schema(implementation = ServiceOrderResponse::class))]),
        ApiResponse(responseCode = "400", description = "No executions registered", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Service order not found", content = [Content()])
    )
    fun finishDiagnosis(id: UUID): ServiceOrderResponse

    @Operation(summary = "Advance service order status")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Status updated",
            content = [Content(schema = Schema(implementation = ServiceOrderResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid state transition", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Service order not found", content = [Content()])
    )
    fun advanceStatus(id: UUID, request: ServiceOrderStatusUpdateRequest): ServiceOrderResponse

    @Operation(summary = "Approve or reject service order")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Approval processed",
            content = [Content(schema = Schema(implementation = ServiceOrderResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid approval state", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Service order not found", content = [Content()])
    )
    fun approve(id: UUID, request: ApprovalRequest): ServiceOrderResponse

    @Operation(summary = "Add extra executions discovered during APPROVED or IN_EXECUTION status")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Extra executions added, order moved to WAITING_APPROVAL_EXTRA_TASKS",
            content = [Content(schema = Schema(implementation = ServiceOrderResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid status for extra tasks", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Service order not found", content = [Content()])
    )
    fun addExtraExecutions(id: UUID, request: AddExtraExecutionsRequest): ServiceOrderResponse

    @Operation(summary = "Approve or reject extra executions")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Extra executions approval processed",
            content = [Content(schema = Schema(implementation = ServiceOrderResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid approval state", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Service order not found", content = [Content()])
    )
    fun approveExtraExecutions(id: UUID, request: ApprovalRequest): ServiceOrderResponse

    @Operation(summary = "Get execution time metrics")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Metrics retrieved",
            content = [Content(schema = Schema(implementation = ServiceOrderMetricsResponse::class))])
    )
    fun getMetrics(): ServiceOrderMetricsResponse

    @Operation(summary = "Get individual services execution time metrics")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Execution metrics retrieved",
            content = [Content(schema = Schema(implementation = ExecutionMetricsResponse::class))])
    )
    fun getExecutionMetrics(): ExecutionMetricsResponse
}
