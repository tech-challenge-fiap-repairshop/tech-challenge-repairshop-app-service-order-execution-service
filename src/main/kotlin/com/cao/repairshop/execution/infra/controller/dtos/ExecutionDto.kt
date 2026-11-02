package com.cao.repairshop.execution.infra.controller.dtos

import com.cao.repairshop.core.validator.annotation.SafeString
import com.cao.repairshop.execution.domain.ExecutionStatus
import com.cao.repairshop.serviceorder.infra.controller.dtos.ExecutionDefinitionRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeItemRequest
import com.cao.repairshop.serviceorder.infra.controller.dtos.InsumeSummaryResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class CreateExecutionRequest(
    @Schema(description = "Service order ID this execution belongs to")
    @field:NotNull val serviceOrderId: UUID,

    @Schema(description = "Service execution description/category", example = "Troca de Óleo")
    @field:NotBlank val basicDescription: String,

    @Schema(description = "Detailed execution description", example = "Transmission fluid replacement using 5W30")
    @field:SafeString val fullDescription: String? = null,

    @Schema(description = "Execution price", example = "450.00")
    @field:NotNull val price: BigDecimal,

    @Schema(description = "Estimated time in hours", example = "3.0")
    val estimatedTime: BigDecimal? = null,

    @Schema(description = "List of insumes with quantities to link")
    @field:Size(max = 50, message = "Maximum of 50 insumes allowed per execution")
    val insumes: List<InsumeItemRequest> = emptyList()
)

data class CreateExecutionBatchRequest(
    @Schema(description = "Service order ID this batch belongs to")
    @field:NotNull val serviceOrderId: UUID,

    @Schema(description = "List of services to add")
    @field:Size(max = 30, message = "Maximum of 30 services allowed per batch")
    val executions: List<ExecutionDefinitionRequest>
)

data class UpdateExecutionRequest(
    @Schema(description = "Service execution description/category", example = "Troca de Óleo")
    @field:NotBlank val basicDescription: String,

    @Schema(description = "Detailed execution description", example = "Transmission fluid replacement using 5W30")
    @field:SafeString val fullDescription: String? = null,

    @Schema(description = "Execution price", example = "450.00")
    @field:NotNull val price: BigDecimal,

    @Schema(description = "Estimated time in hours", example = "3.0")
    val estimatedTime: BigDecimal? = null
)

data class ExecutionStatusUpdateRequest(
    @Schema(description = "New execution status", examples = ["FINALIZED","INITIATED","PENDING"])
    @field:NotNull val status: ExecutionStatus
)

data class ExecutionResponse(
    @Schema(description = "Execution unique identifier")
    val id: UUID,
    @Schema(description = "Parent service order ID")
    val serviceOrderId: UUID,
    @Schema(description = "Service description", example = "Troca de Óleo")
    val basicDescription: String,
    @Schema(description = "Detailed execution description", example = "Transmission fluid replacement using 5W30")
    val fullDescription: String?,
    @Schema(description = "Labor price only", example = "450.00")
    val laborPrice: BigDecimal,
    @Schema(description = "List of insumes used in this execution")
    val insumes: List<InsumeSummaryResponse> = emptyList(),
    @Schema(description = "Total price for this execution (labor + insumes)", example = "650.00")
    val totalPrice: BigDecimal,
    @Schema(description = "Estimated time in hours", example = "3.0")
    val estimatedTime: BigDecimal?,
    @Schema(description = "Current status", example = "INITIATED")
    val status: ExecutionStatus,
    @Schema(description = "Record creation timestamp")
    val created: LocalDateTime?,
    @Schema(description = "Last update timestamp")
    val updated: LocalDateTime?
)

data class ExecutionHistoryResponse(
    @Schema(description = "History entry ID")
    val id: UUID,
    @Schema(description = "Execution ID")
    val executionId: UUID,
    @Schema(description = "Status at this point", example = "PENDING")
    val status: ExecutionStatus,
    @Schema(description = "Timestamp of the transition")
    val registerTime: LocalDateTime,
    @Schema(description = "Seconds since previous transition")
    val intervalTime: Long?
)
