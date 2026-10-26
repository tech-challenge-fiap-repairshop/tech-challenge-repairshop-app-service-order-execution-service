package com.cao.repairshop.serviceorder.infra.messaging

import com.cao.repairshop.core.health.SqsListenerHealthIndicator
import com.cao.repairshop.serviceorder.infra.persistence.dynamo.DynamoServiceOrderRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Span
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.util.UUID

data class OrderApprovalEvent(
    val orderId: UUID,
    val customerId: UUID,
    val vehicleId: UUID,
    val totalPrice: Double,
    val description: String? = null
)

data class PaymentOrderEvent(
    val orderId: UUID,
    val customerId: UUID,
    val amount: Double
)

data class CallbackApprovalMessage(
    val orderId: UUID,
    val approved: Boolean,
    val reason: String? = null
)

data class CallbackPaymentMessage(
    val orderId: UUID,
    val paymentId: String,
    val status: String
)

@Component
class ServiceOrderSqsMessenger(
    private val sqsClient: SqsClient,
    private val dynamoRepository: DynamoServiceOrderRepository,
    private val objectMapper: ObjectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().findAndRegisterModules(),
    @Value("\${sqs.queue.approve-notification:approve-notification-queue-dev}")
    private val approveNotificationQueue: String,
    @Value("\${sqs.queue.callback-approve:callback-approve-notification-queue-dev}")
    private val callbackApproveQueue: String,
    @Value("\${sqs.queue.create-payment:create-payment-order-queue-dev}")
    private val createPaymentQueue: String,
    @Value("\${sqs.queue.callback-payment:callback-payment-order-queue-dev}")
    private val callbackPaymentQueue: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        // Registra as filas de callback no indicador de saúde para monitorar Pods Zumbis (PF-11)
        SqsListenerHealthIndicator.registerMonitoredQueue(callbackApproveQueue, 120)
        SqsListenerHealthIndicator.registerMonitoredQueue(callbackPaymentQueue, 120)
    }

    private fun getQueueUrl(queueName: String): String {
        return runCatching {
            sqsClient.getQueueUrl { it.queueName(queueName) }.queueUrl()
        }.getOrElse { queueName }
    }

    /**
     * Publica evento para o Notification Service com injeção do W3C Trace Context (PF-10)
     */
    fun sendOrderForApproval(orderId: UUID, customerId: UUID, vehicleId: UUID, totalPrice: Double, description: String?) {
        val queueUrl = getQueueUrl(approveNotificationQueue)
        val event = OrderApprovalEvent(orderId, customerId, vehicleId, totalPrice, description)
        val body = objectMapper.writeValueAsString(event)

        val currentSpan = Span.current()
        val traceparent = "00-${currentSpan.spanContext.traceId}-${currentSpan.spanContext.spanId}-01"

        val request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(body)
            .messageAttributes(mapOf(
                "traceparent" to MessageAttributeValue.builder().dataType("String").stringValue(traceparent).build()
            ))
            .build()

        sqsClient.sendMessage(request)
        logger.info("Publicado evento de aprovação da OS {} na fila {} (traceparent: {})", orderId, approveNotificationQueue, traceparent)
    }

    /**
     * Publica evento de ordem de pagamento para o Payment Service (PF-10)
     */
    fun sendOrderForPayment(orderId: UUID, customerId: UUID, amount: Double) {
        val queueUrl = getQueueUrl(createPaymentQueue)
        val event = PaymentOrderEvent(orderId, customerId, amount)
        val body = objectMapper.writeValueAsString(event)

        val currentSpan = Span.current()
        val traceparent = "00-${currentSpan.spanContext.traceId}-${currentSpan.spanContext.spanId}-01"

        val request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(body)
            .messageAttributes(mapOf(
                "traceparent" to MessageAttributeValue.builder().dataType("String").stringValue(traceparent).build()
            ))
            .build()

        sqsClient.sendMessage(request)
        logger.info("Publicado evento de cobrança da OS {} na fila {} (amount: {})", orderId, createPaymentQueue, amount)
    }

    /**
     * Polling contínuo da fila de Callback de Aprovação
     */
    @Scheduled(fixedDelay = 5000)
    fun pollCallbackApproval() {
        val queueUrl = getQueueUrl(callbackApproveQueue)
        runCatching {
            val response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(2)
                    .messageAttributeNames("All")
                    .build()
            )

            // Registra heartbeat indicando que o loop de mensageria está vivo (PF-11)
            SqsListenerHealthIndicator.recordHeartbeat(callbackApproveQueue)

            for (msg in response.messages()) {
                val callback = objectMapper.readValue(msg.body(), CallbackApprovalMessage::class.java)
                val newStatus = if (callback.approved) "IN_PROGRESS" else "CANCELED"
                dynamoRepository.updateStatus(callback.orderId, newStatus)
                logger.info("Callback de aprovação processado: OS {} -> {}", callback.orderId, newStatus)

                sqsClient.deleteMessage(
                    DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(msg.receiptHandle())
                        .build()
                )
            }
        }.onFailure { ex ->
            logger.warn("Erro ao ler fila {}: {}", callbackApproveQueue, ex.message)
        }
    }

    /**
     * Polling contínuo da fila de Callback de Pagamento
     */
    @Scheduled(fixedDelay = 5000)
    fun pollCallbackPayment() {
        val queueUrl = getQueueUrl(callbackPaymentQueue)
        runCatching {
            val response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(2)
                    .messageAttributeNames("All")
                    .build()
            )

            SqsListenerHealthIndicator.recordHeartbeat(callbackPaymentQueue)

            for (msg in response.messages()) {
                val callback = objectMapper.readValue(msg.body(), CallbackPaymentMessage::class.java)
                if (callback.status == "APPROVED" || callback.status == "COMPLETED") {
                    dynamoRepository.updateStatus(callback.orderId, "COMPLETED", "DISPATCHED")
                    logger.info("Pagamento confirmado via fila: OS {} finalizada e liberada para retirada", callback.orderId)
                }

                sqsClient.deleteMessage(
                    DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(msg.receiptHandle())
                        .build()
                )
            }
        }.onFailure { ex ->
            logger.warn("Erro ao ler fila {}: {}", callbackPaymentQueue, ex.message)
        }
    }
}
