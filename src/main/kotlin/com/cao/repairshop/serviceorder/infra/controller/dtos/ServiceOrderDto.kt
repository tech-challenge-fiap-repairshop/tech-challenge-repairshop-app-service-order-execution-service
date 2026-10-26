package com.cao.repairshop.serviceorder.infra.controller.dtos

import com.cao.repairshop.core.validator.annotation.SafeString
import com.cao.repairshop.core.validator.annotation.ValidBasicExecution
import com.cao.repairshop.execution.domain.BasicExecution
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class InsumeItemRequest(
    @Schema(description = "Insume ID")
    val insumeId: UUID,
    @Schema(description = "Quantity used", example = "1")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int = 1
)

data class ExecutionDefinitionRequest(
    @Schema(description = "Basic service execution category", example = "OIL_CHANGE", allowableValues = ["OIL_CHANGE", "SUSPENSION_REPLACEMENT", "WHEEL_ALIGNMENT", "BRAKE_INSPECTION", "ENGINE_DIAGNOSIS", "OTHER"])
    @field:NotBlank @field:ValidBasicExecution val basicDescription: String,

    @Schema(description = "Detailed execution description", example = "Engine oil change using synthetic oil")
    @field:SafeString val fullDescription: String? = null,

    @Schema(description = "Service price", example = "250.00")
    val price: BigDecimal?,

    @Schema(description = "Estimated time in hours", example = "2.5")
    val estimatedTime: BigDecimal? = null,

    @Schema(description = "List of insumes with quantities to link")
    @field:Size(max = 50, message = "Maximum of 50 insumes allowed per execution")
    val insumes: List<InsumeItemRequest> = emptyList()
)

data class CreateServiceOrderRequest(
    @Schema(description = "Customer ID (UUID)", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    val customerId: UUID? = null,

    @Schema(description = "Vehicle ID (UUID)", example = "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e")
    val vehicleId: UUID? = null,

    @Schema(description = "Customer email who requested the order", example = "customer@example.com")
    @field:Email(message = "Invalid email format")
    val customerEmail: String? = null,

    @Schema(description = "Vehicle plate for this order", example = "ABC-1234")
    val vehiclePlate: String? = null,

    @Schema(description = "List of services to include in the order")
    @field:Size(max = 30, message = "Maximum of 30 services allowed per order")
    val services: List<ExecutionDefinitionRequest> = emptyList()
)

data class ServiceOrderStatusUpdateRequest(
    @Schema(description = "New status for the service order", example = "IN_EXECUTION")
    @field:NotNull val status: ServiceOrderStatus
)

data class ApprovalRequest(
    @Schema(description = "Whether the order is approved", example = "true")
    @field:NotNull val approved: Boolean
)

data class ServiceOrderResponse(
    @Schema(description = "Service order unique identifier")
    val id: UUID,
    @Schema(description = "Customer ID")
    val customerId: UUID,
    @Schema(description = "Vehicle ID")
    val vehicleId: UUID,
    @Schema(description = "Current status", example = "WAITING_APPROVAL")
    val status: ServiceOrderStatus,
    @Schema(description = "Total accumulated price", example = "1430.50")
    val totalPrice: BigDecimal,
    @Schema(description = "Vehicle entry time")
    val enterTime: LocalDateTime?,
    @Schema(description = "Projected or actual end time")
    val endTime: LocalDateTime?,
    @Schema(description = "Record creation timestamp")
    val created: LocalDateTime?,
    @Schema(description = "Last update timestamp")
    val updated: LocalDateTime?,
    @Schema(description = "Services linked to this order")
    val services: List<ExecutionSummary> = emptyList(),
    @Schema(description = "Status transition history")
    val history: List<ServiceOrderHistoryEntry> = emptyList()
)

data class ExecutionSummary(
    @Schema(description = "Service ID")
    val id: UUID,
    @Schema(description = "Basic service category", example = "BRAKE_INSPECTION")
    val basicDescription: BasicExecution,
    @Schema(description = "Detailed execution description", example = "Brake pad replacement and bleeding")
    val fullDescription: String?,
    @Schema(description = "Labor price only", example = "350.00")
    val laborPrice: BigDecimal,
    @Schema(description = "List of insumes used in this service")
    val insumes: List<InsumeSummaryResponse> = emptyList(),
    @Schema(description = "Total price for this service (labor + insumes)", example = "450.00")
    val totalPrice: BigDecimal,
    @Schema(description = "Current status", example = "INITIATED")
    val status: String
)

data class InsumeSummaryResponse(
    @Schema(description = "Insume name", example = "Synthetic Oil 5W30")
    val name: String,
    @Schema(description = "Quantity used", example = "4")
    val quantity: Int,
    @Schema(description = "Unit price", example = "50.00")
    val unitPrice: BigDecimal,
    @Schema(description = "Total price for this insume", example = "200.00")
    val totalPrice: BigDecimal
)

data class ServiceOrderHistoryEntry(
    @Schema(description = "History entry ID")
    val id: UUID,
    @Schema(description = "Status at this point", example = "APPROVED")
    val status: ServiceOrderStatus,
    @Schema(description = "Timestamp of the transition")
    val registerTime: LocalDateTime,
    @Schema(description = "Seconds since previous transition")
    val intervalTime: Long?
)

data class ServiceOrderMetricsResponse(
    @Schema(description = "Average execution time in minutes", example = "145.5")
    val averageExecutionTimeMinutes: Double?
)
