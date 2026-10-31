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

data class OsReadyForPaymentEvent(
    val orderId: UUID,
    val customerId: UUID,
    val amount: Double
)

data class CallbackApprovalMessage(
    val orderId: UUID,
    val approved: Boolean,
    val reason: String? = null
)

data class PaymentSuccessMessage(
    val orderId: UUID,
    val paymentId: String,
    val status: String,
    val amount: Double? = null
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
    @Value("\${sqs.queue.os-ready-for-payment:os-ready-for-payment-queue-dev}")
    private val osReadyForPaymentQueue: String,
    @Value("\${sqs.queue.payment-success:payment-success-queue-dev}")
    private val paymentSuccessQueue: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        // Registra as filas de escuta no health indicator para monitoramento de pods zumbis
        SqsListenerHealthIndicator.registerMonitoredQueue(callbackApproveQueue, 120)
        SqsListenerHealthIndicator.registerMonitoredQueue(paymentSuccessQueue, 120)
    }

    private fun getQueueUrl(queueName: String): String {
        return runCatching {
            sqsClient.getQueueUrl { it.queueName(queueName) }.queueUrl()
        }.getOrElse { queueName }
    }

    /**
     * Publica evento para o Notification Service na fila approve-notification-queue
     * quando a OS atinge o status WAITING_APPROVAL
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
     * Publica evento na fila os-ready-for-payment-queue informando o payment-service que a OS está pronta para pagamento
     */
    fun sendOrderReadyForPayment(orderId: UUID, customerId: UUID, amount: Double) {
        val queueUrl = getQueueUrl(osReadyForPaymentQueue)
        val event = OsReadyForPaymentEvent(orderId, customerId, amount)
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
        logger.info("Publicado evento de OS pronta para pagamento {} na fila {} (valor: {})", orderId, osReadyForPaymentQueue, amount)
    }

    /**
     * Scheduler oficial do desenho: scheduler_os_ready_for_payment (a cada 30 min por padrão, ou configurável)
     * Varre ordens pendentes no DynamoDB (GSI pending-dispatch-index) e as publica em os-ready-for-payment-queue
     */
    @Scheduled(fixedDelayString = "\${scheduler.os-ready-for-payment.delay:1800000}", initialDelay = 15000)
    fun schedulerOsReadyForPayment() {
        logger.info("Executando scheduler_os_ready_for_payment...")
        val pendingOrders = dynamoRepository.findPendingNotification()
        for (order in pendingOrders) {
            runCatching {
                sendOrderReadyForPayment(order.id, order.customerId, order.totalPrice)
                dynamoRepository.updateStatus(order.id, order.status, "DISPATCHED")
                logger.info("OS {} despachada com sucesso para os-ready-for-payment-queue", order.id)
            }.onFailure { ex ->
                logger.error("Falha ao despachar OS {} para pagamento: {}", order.id, ex.message)
            }
        }
    }

    /**
     * Polling contínuo da fila de Callback de Aprovação: callback-approve-notification-queue
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

            SqsListenerHealthIndicator.recordHeartbeat(callbackApproveQueue)

            for (msg in response.messages()) {
                val callback = objectMapper.readValue(msg.body(), CallbackApprovalMessage::class.java)
                val newStatus = if (callback.approved) "APPROVED" else "CANCELED"
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
     * Polling contínuo da fila de Pagamento com Sucesso: payment-success-queue
     * Despachado pelo scheduler do payment-service quando o pagamento for concluído
     */
    @Scheduled(fixedDelay = 5000)
    fun pollPaymentSuccess() {
        val queueUrl = getQueueUrl(paymentSuccessQueue)
        runCatching {
            val response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(2)
                    .messageAttributeNames("All")
                    .build()
            )

            SqsListenerHealthIndicator.recordHeartbeat(paymentSuccessQueue)

            for (msg in response.messages()) {
                val paymentMsg = objectMapper.readValue(msg.body(), PaymentSuccessMessage::class.java)
                if (paymentMsg.status == "APPROVED" || paymentMsg.status == "COMPLETED") {
                    dynamoRepository.updateStatus(paymentMsg.orderId, "PAID", "DISPATCHED", "PAID")
                    logger.info("Pagamento confirmado via payment-success-queue: OS {} atualizada para PAID", paymentMsg.orderId)
                }

                sqsClient.deleteMessage(
                    DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(msg.receiptHandle())
                        .build()
                )
            }
        }.onFailure { ex ->
            logger.warn("Erro ao ler fila {}: {}", paymentSuccessQueue, ex.message)
        }
    }
}
