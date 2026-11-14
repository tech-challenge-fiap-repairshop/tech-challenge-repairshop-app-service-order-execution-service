package com.cao.repairshop.serviceorder.infra.controller.dtos

import com.cao.repairshop.core.validator.annotation.SafeString
import com.cao.repairshop.serviceorder.domain.ServiceOrderStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class InsumeItemRequest(
    @Schema(description = "Insume name", example = "Óleo Sintético 5W30")
    @field:NotBlank(message = "Insume name is required")
    val name: String,

    @Schema(description = "Quantity used", example = "4")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int = 1,

    @Schema(description = "Unit price of insume", example = "45.00")
    val unityPrice: BigDecimal = BigDecimal.ZERO,

    @Schema(description = "Insume brand", example = "Castrol")
    val brand: String? = null,

    @Schema(description = "SKU ID", example = "SKU-OIL-5W30")
    val skuId: String? = null,

    @Schema(description = "Insume ID (optional)")
    val insumeId: UUID? = null
)

data class ExecutionDefinitionRequest(
    @Schema(description = "Service execution description/category", example = "Troca de Óleo")
    @field:NotBlank(message = "basicDescription is required")
    val basicDescription: String,

    @Schema(description = "Detailed execution description", example = "Troca completa de óleo e filtro com inspeção visual")
    @field:SafeString
    val fullDescription: String? = null,

    @Schema(description = "Service price", example = "250.00")
    val price: BigDecimal? = null,

    @Schema(description = "Estimated time in hours", example = "1.5")
    val estimatedTime: BigDecimal? = null,

    @Schema(description = "List of insumes with quantities to link")
    @field:Size(max = 50, message = "Maximum of 50 insumes allowed per execution")
    val insumes: List<InsumeItemRequest> = emptyList()
)

data class CreateServiceOrderRequest(
    @Schema(description = "Customer ID (UUID) provided by register-service", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d", required = true)
    @field:NotNull(message = "customerId is mandatory")
    val customerId: UUID,

    @Schema(description = "Vehicle ID (UUID) provided by register-service", example = "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e", required = true)
    @field:NotNull(message = "vehicleId is mandatory")
    val vehicleId: UUID,

    @Schema(description = "List of services to include in the order (optional at creation)")
    @field:Size(max = 30, message = "Maximum of 30 services allowed per order")
    val services: List<ExecutionDefinitionRequest> = emptyList()
)

data class AddExtraExecutionsRequest(
    @Schema(description = "List of newly discovered extra services", required = true)
    @field:NotEmpty(message = "At least one extra execution must be provided")
    @field:Valid
    val executions: List<ExecutionDefinitionRequest>,

    @Schema(description = "Notes explaining why extra services are necessary", example = "Vazamento no retentor de óleo detectado na desmontagem")
    val notes: String? = null
)

data class ServiceOrderStatusUpdateRequest(
    @Schema(description = "New status for the service order", example = "IN_EXECUTION")
    @field:NotNull
    val status: ServiceOrderStatus
)

data class ApprovalRequest(
    @Schema(description = "Whether the order or extra services are approved", example = "true")
    @field:NotNull
    val approved: Boolean,

    @Schema(description = "Optional notes or reason for refusal")
    val notes: String? = null
)

data class ServiceOrderResponse(
    @Schema(description = "Service order unique identifier")
    val id: UUID,
    @Schema(description = "Customer ID")
    val customerId: UUID,
    @Schema(description = "Vehicle ID")
    val vehicleId: UUID,
    @Schema(description = "Current status", example = "IN_DIAGNOSIS")
    val status: ServiceOrderStatus,
    @Schema(description = "Total accumulated price", example = "1430.50")
    val totalPrice: BigDecimal,
    @Schema(description = "Vehicle entry time")
    val enterTime: LocalDateTime?,
    @Schema(description = "Projected or actual end time")
    val endTime: LocalDateTime?,
    @Schema(description = "Whether extra services were identified")
    val hasExtraServices: Boolean = false,
    @Schema(description = "Whether extra services were accepted by customer")
    val extraServicesAccepted: Boolean? = null,
    @Schema(description = "Record creation timestamp")
    val created: LocalDateTime?,
    @Schema(description = "Last update timestamp")
    val updated: LocalDateTime?,
    @Schema(description = "Services linked to this order")
    val services: List<ExecutionSummary> = emptyList(),
    @Schema(description = "Extra execution batches")
    val extraExecutions: List<ExtraExecutionResponse> = emptyList(),
    @Schema(description = "Status transition history")
    val history: List<ServiceOrderHistoryEntry> = emptyList()
)

data class ExtraExecutionResponse(
    @Schema(description = "Extra execution batch ID")
    val id: UUID,
    @Schema(description = "Service order ID")
    val serviceOrderId: UUID,
    @Schema(description = "Whether extra services exist")
    val hasExtraServices: Boolean,
    @Schema(description = "Whether extra services were accepted")
    val extraServicesAccepted: Boolean?,
    @Schema(description = "Status of extra services", example = "WAITING_APPROVAL")
    val status: String,
    @Schema(description = "Total price for this extra batch", example = "350.00")
    val totalExtraPrice: BigDecimal,
    @Schema(description = "Notes/justification")
    val notes: String?,
    @Schema(description = "Executions included in this extra batch")
    val executions: List<ExecutionSummary> = emptyList()
)

data class ExecutionSummary(
    @Schema(description = "Service ID")
    val id: UUID,
    @Schema(description = "Service description", example = "Troca de Pastilha de Freio")
    val basicDescription: String,
    @Schema(description = "Detailed execution description", example = "Substituição das pastilhas dianteiras e sangria")
    val fullDescription: String?,
    @Schema(description = "Labor price only", example = "150.00")
    val laborPrice: BigDecimal,
    @Schema(description = "List of insumes used in this service")
    val insumes: List<InsumeSummaryResponse> = emptyList(),
    @Schema(description = "Total price for this service (labor + insumes)", example = "350.00")
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
    val intervalTime: Long?,
    @Schema(description = "Description or notes")
    val description: String? = null
)

data class ServiceOrderMetricsResponse(
    @Schema(description = "Average execution time in minutes", example = "145.5")
    val averageExecutionTimeMinutes: Double?
)
