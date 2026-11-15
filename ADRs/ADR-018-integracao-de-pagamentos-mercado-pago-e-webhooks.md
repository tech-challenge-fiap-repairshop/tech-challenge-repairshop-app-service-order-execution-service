# ADR-018: Integração de Pagamentos com Mercado Pago, Resiliência via Circuit Breaker e Webhooks

**Data:** 2026-10-24  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

O faturamento da oficina mecânica depende da cobrança digital ágil e confiável de clientes que retiram seus veículos ou aprovam orçamentos remotamente. A forma de pagamento prioritária e instantânea é o **PIX via Mercado Pago**, complementada por links de checkout e cartões de crédito.

Contudo, integrações com parceiros financeiros externos apresentam desafios operacionais severos:
1. **Instabilidade Externa e Degradação de Latência**: Picos ou lentidões na API externa do Mercado Pago podem esgotar threads do servidor de pagamentos (*thread pool starvation*).
2. **Recepção Assíncrona de Pagamentos (Webhooks)**: Pagamentos PIX são efetuados no aplicativo bancário do cliente e notificados de volta via requisições HTTP Webhook enviadas pelos servidores do Mercado Pago, exigindo validação de segurança e idempotência contra notificações duplicadas.

---

## Decisão

Isolar toda a lógica financeira dentro do **`payment-service`**, adotando uma integração oficial com o **Mercado Pago** protegida por **Circuit Breaker** (Resilience4j) e recepção assíncrona de Webhooks com conferência atômica no DynamoDB:

```
┌─────────────────────────────────┐
│         Cliente Mobile          │
└──────┬───────────────────▲──────┘
       │                   │
  [1. Paga PIX]      [QR Code / Link]
       │                   │
       ▼                   │
┌──────────────────────────┴──────┐
│       Mercado Pago API          │
└──────┬──────────────────────────┘
       │
  [2. HTTP Webhook Event]
       │
       ▼
┌────────────────────────────────────────────────────────┐
│                    payment-service                     │
│                                                        │
│  ┌─────────────────────────┐  ┌─────────────────────┐  │
│  │ Webhook Controller      │  │ Circuit Breaker     │  │
│  │ - Validação HMAC        │  │ - Failure Rate: 50% │  │
│  │ - Idempotência DynamoDB │  │ - Wait Open: 10s    │  │
│  └───────────┬─────────────┘  └─────────────────────┘  │
│              │                                         │
│              ▼                                         │
│  ┌─────────────────────────┐                           │
│  │ DynamoDB: tb-payment    │                           │
│  │ - Gravação do Status    │                           │
│  └───────────┬─────────────┘                           │
│              │                                         │
│              ▼                                         │
│  ┌─────────────────────────┐                           │
│  │ SQS Publisher           │                           │
│  │ -> payment-success-queue│                           │
│  └─────────────────────────┘                           │
└────────────────────────────────────────────────────────┘
```

### 1. Mecanismo de Circuit Breaker
- **Taxa de Falha:** 50% de erros ou timeouts em uma janela deslizante de 10 chamadas aciona a abertura do circuito.
- **Wait Duration in Open State:** 10 segundos antes de realizar transição para *Half-Open*.
- **Fallback Gracioso:** Em caso de indisponibilidade do Mercado Pago, a requisição retorna um status de contingência amigável ao cliente com agendamento de retentativa, prevenindo travamento do pool de threads HTTP.

### 2. Tratamento Seguro e Idempotente de Webhooks
- O endpoint `POST /payments/webhook` recebe os eventos (`payment.created`, `payment.updated`).
- A autenticação do webhook valida a assinatura do header `x-signature` contra o segredo compartilhado.
- A conciliação busca os dados atualizados do pagamento na API do gateway e atualiza o item `item_payment` no DynamoDB utilizando escrita condicional para impedir reprocessamento de pagamentos já consolidados.
- Uma vez quitado com sucesso, o evento é publicado imediatamente na fila `payment-success-queue`.

---

## Consequências

### Positivas
- **Isolamento de Falhas Externas**: Lentidões do Mercado Pago não contaminam o restante da malha de microsserviços da oficina.
- **Confirmação em Tempo Real**: Notificações instantâneas via Webhook liberam a Ordem de Serviço para reparo sem espera do cliente no balcão.
- **Resiliência e Idempotência**: Entregas repetidas do webhook são absorvidas transparentemente pelo DynamoDB.

### Desafios e Mitigações
- **Divergência Temporária de Webhook Não Entregue**: Mitigada pelo scheduler periódico de reconciliação (`scheduler_os_payment_atualization` a cada 10 min) que consulta faturas pendentes no GSI `dispatch_notificate_status`.

---

## Referências
- Mercado Pago Developers, *API Reference & Webhooks Security*.
- Michael Nygard, *Release It! Design and Deploy Production-Ready Software* (Circuit Breaker Pattern).
