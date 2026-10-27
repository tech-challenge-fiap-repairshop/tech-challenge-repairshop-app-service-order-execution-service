package com.cao.repairshop.serviceorder.infra.persistence.dynamo

import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ExtraExecutionDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderHistoryDynamoItem
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.models.ServiceOrderMetadataDynamoItem

data class FullServiceOrder(
    val metadata: ServiceOrderMetadataDynamoItem?,
    val histories: List<ServiceOrderHistoryDynamoItem>,
    val executions: List<ExecutionDynamoItem>,
    val extraExecutions: List<ExtraExecutionDynamoItem>
)
