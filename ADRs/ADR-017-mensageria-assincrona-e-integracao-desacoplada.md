# ADR-017: Mensageria Assíncrona para Eventos Desacoplados e Orquestração com AWS SQS e DLQs

**Data:** 2026-10-24  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

Na arquitetura de microsserviços da Fase 4, múltiplos processos de negócio cruzam fronteiras de domínios com requisitos heterogêneos de tempo de resposta:
1. Notificação de orçamentos para o cliente e recebimento de aprovações/recusas.
2. Encaminhamento de ordens de serviço aprovadas para a esteira de cobrança.
3. Despacho de confirmações de quitação de pagamentos para autorização do início dos trabalhos na bancada mecânica.

O uso exclusivo de chamadas HTTP síncronas para essas comunicações introduziria acoplamento temporal rígido, vulnerabilidade a falhas em cascata (*cascading failures*) e indisponibilidade sistêmica caso um dos serviços intermediários estivesse em reinicialização ou sofrendo picos de carga.

---

## Decisão

Adotar o **Amazon Simple Queue Service (AWS SQS)** como espinha dorsal da mensageria assíncrona entre os microsserviços, estruturado em uma topologia de cinco filas ponto-a-ponto com Dead Letter Queues (DLQs) associadas e controle de retentativa exponencial:

```
┌─────────────────────────────────┐
│ service-order-execution-service │
└──────┬───────────────────▲──────┘
       │                   │
  [Publica]             [Consome]
       │                   │
       ▼                   │
┌───────────────────────────────┐  ┌─────────────────────────────────────┐
│ SQS: approve-notification     │  │ SQS: callback-approve-notification  │
│ -queue                        │  │ -queue                              │
└──────────────┬────────────────┘  └──────────────────▲──────────────────┘
               │                                      │
           [Consome]                              [Publica]
               │                                      │
               ▼                                      │
┌─────────────────────────────────────────────────────┴─┐
│                 notification-service                  │
└──────▲────────────────────────────────────────────────┘
       │
   [Consome]
       │
┌──────┴────────────────────────┐
│ SQS: payment-notification     │
│ -queue                        │
└──────────────▲────────────────┘
               │
           [Publica]
               │
┌──────────────┴────────────────┐  ┌─────────────────────────────────────┐
│ SQS: os-ready-for-payment     │  │ SQS: payment-success                │
│ -queue                        │  │ -queue                              │
└──────────────▲────────────────┘  └──────────────────┬──────────────────┘
               │                                      │
           [Consome]                              [Publica]
               │                                      │
┌──────────────┴──────────────────────────────────────▼─┐
│                    payment-service                    │
└───────────────────────────────────────────────────────┘
```

### 1. Especificação das Filas e DLQs
Cada fila possui uma Dead Letter Queue dedicada (`<queue-name>-dlq`), configurada com política de redrive:
- `maxReceiveCount = 3`: Se uma mensagem falhar 3 vezes consecutivas durante o processamento, é isolada na DLQ correspondente sem bloquear a fila principal.
- `visibilityTimeout = 60s`: Tempo adequado para que o pod execute o processamento e comite o ACK (delete message) antes que a mensagem se torne visível para outro consumidor.
- Retenção: 4 dias para filas padrão e 14 dias para DLQs para fins de investigação e auditoria operacional.

### 2. Contrato de Mensagens e Idempotência
- Payloads padronizados em JSON estruturado com metadados do evento (`eventId`, `eventType`, `timestamp`, `serviceOrderId`, `payload`).
- Consumidores implementam deduplicação idempotente orientada a chave primária / escrita condicional no DynamoDB (`attribute_not_exists(pk)`), prevenindo reprocessamentos decorrentes da garantia *at-least-once delivery* do SQS.

---

## Consequências

### Positivas
- **Desacoplamento Temporal Completo**: Se o `notification-service` ou o `payment-service` estiverem temporariamente fora do ar para deploy, as mensagens permanecem enfileiradas no SQS sem perda de dados.
- **Absorção de Picos (Load Leveling)**: A taxa de consumo dos workers no Kubernetes é regulada independentemente da velocidade de postagem das mensagens.
- **Tolerância a Falhas e Isolamento de Mensagens Tóxicas**: Erros graves de deserialização ou regras de negócio inesperadas são encaminhados à DLQ, preservando a saúde da esteira.

### Desafios e Mitigações
- **Consistência Eventual**: O cliente pode aprovar um orçamento e a OS levar alguns segundos para refletir o status no banco. Mitigado pela UX com feedback claro de "Processando solicitação".
- **Monitoramento de Filas Mortas**: Mitigado via alarmes CloudWatch e dashboards centralizados no Grafana monitorando `ApproximateNumberOfMessagesVisible` nas DLQs.

---

## Referências
- Enterprise Integration Patterns (Hohpe & Woolf), *Message Channel & Dead Letter Channel*.
- AWS Architecture Center, *Best Practices for Amazon SQS*.
