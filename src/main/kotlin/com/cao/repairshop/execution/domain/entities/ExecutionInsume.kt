package com.cao.repairshop.execution.domain.entities

import java.util.UUID

class ExecutionInsume(
    val executionId: UUID,
    val insume: Insume,
    var quantity: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExecutionInsume) return false
        return executionId == other.executionId && insume.id == other.insume.id
    }

    override fun hashCode(): Int {
        var result = executionId.hashCode()
        result = 31 * result + insume.id.hashCode()
        return result
    }
}
