# ADR-015: Gestão Transacional Distribuída com Saga Pattern e Mecanismos de Compensação

**Data:** 2026-10-22  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

Com a divisão da aplicação nos três microsserviços especializados (**OS Service**, **Billing Service** e **Execution Service**), cada um operando com seu próprio banco de dados isolado (*Database per Service*), transações locais ACID tornam-se inviáveis para fluxos que cruzam múltiplos domínios.

O fluxo de atendimento da oficina mecânica envolve etapas interdependentes e críticas:
1. Abertura da Ordem de Serviço (`OS Service`).
2. Geração e envio do orçamento para aprovação do cliente (`Billing Service`).
3. Confirmação do pagamento via Mercado Pago (`Billing Service`).
4. Envio da OS para a fila de execução e reparos técnicos (`Execution Service`).
5. Conclusão dos reparos e liberação para retirada do veículo (`OS Service`).

Em caso de falha em qualquer uma dessas etapas (por exemplo: cliente rejeita o orçamento, pagamento não é aprovado pelo Mercado Pago, ou mecânico identifica falta de peças e inviabilidade técnica de reparo), o sistema **não pode deixar a aplicação em estado inconsistente**. É indispensável garantir **consistência eventual** e capacidade de **rollback semântico (transações de compensação)**.

---

## Decisão

Adotar o **Saga Pattern Coreografado (Choreographed Saga)** para a coordenação dos processos transacionais distribuídos, com suporte a mensageria assíncrona orientada a eventos e transações de compensação explícitas:

```
[FLUXO NOMINAL - CAMINHO FELIZ]
Cliente -> (POST /orders) -> [OS Service] --(OrderCreatedEvent)--> [Billing Service]
                                                                        │
[Execution Service] <--(PaymentApprovedEvent)-- [Mercado Pago Webhook] ◄┘
       │
(ServiceCompletedEvent)
       │
       ▼
  [OS Service] -> Status: FINALIZADA / PRONTA_PARA_RETIRADA


[FLUXO DE COMPENSAÇÃO - ROLLBACK POR FALHA]
Cenário: Falta de Peças / Inviabilidade Técnica na Oficina
[Execution Service] --(ExecutionFailedEvent)--> [Billing Service] -> Inicia Estorno/Reembolso MP
                                           │
                                           └──> [OS Service] -> Status: CANCELADA_COM_ESTORNO
```

### 1. Modelo de Coreografia Orientada a Eventos
- Os serviços comunicam-se emitindo e consumindo eventos através de tópicos de mensageria (RabbitMQ / AWS SQS-SNS).
- Nenhum serviço atua como um "controlador monometálico" sobre os demais, mantendo o acoplamento mínimo e alta autonomia operacional.

### 2. Matriz de Transações de Compensação (Rollback)

| Ponto de Falha | Gatilho | Ação de Compensação (Rollback) | Estado Final da OS |
| :--- | :--- | :--- | :--- |
| **Orçamento Rejeitado** | Cliente não aceita valores | `Billing Service` publica `BudgetRejectedEvent`. `OS Service` cancela a ordem e libera a fila. | `ORCAMENTO_RECUSADO` |
| **Pagamento Recusado/Expirado** | Falha no Mercado Pago | `Billing Service` publica `PaymentFailedEvent`. A OS não ingressa na fila mecânica; notifica cliente para nova tentativa. | `PAGAMENTO_PENDENTE` ou `CANCELADA` |
| **Inviabilidade Técnica de Reparo** | Diagnóstico detecta falha irreversível ou peças indisponíveis | `Execution Service` publica `ExecutionAbortedEvent`. `Billing Service` dispara estorno no Mercado Pago; `OS Service` notifica cliente. | `CANCELADA_COM_ESTORNO` |

### 3. Garantias de Idempotência e Confiabilidade
- **Chave de Idempotência (Idempotency Key):** Baseada no `orderId` e no `transactionId` em todos os consumers de mensagens, impedindo reprocessamento duplicado de eventos.
- **Dead Letter Queue (DLQ):** Mensagens não processáveis por erros técnicos são roteadas para uma DLQ para análise e reprocessamento posterior sem travar a fila principal.

---

## Consequências

### Positivas
- **Alta Disponibilidade e Desacoplamento:** Serviços continuam funcionando mesmo se um consumidor estiver temporariamente indisponível.
- **Auditabilidade Completa:** Todo o histórico de transições da Saga fica registrado nos eventos e correlacionado via OpenTelemetry.
- **Rollback Seguro:** Atende integralmente à exigência da Fase 4 de garantir rollback compensatório para qualquer falha distribuída.

### Negativas / Desafios Mitigados
- **Risco de Dependências Cíclicas:** Mitigado pelo design estrito dos eventos de domínio (fluxo unidirecional de estados).
- **Rastreabilidade da Saga:** Mitigado pela injeção obrigatória do `traceparent` (W3C Trace Context) nos cabeçalhos das mensagens do broker, permitindo visualizar a Saga de ponta a ponta no **Jaeger**.

---

## Referências
- Documento Oficial: `SOAT - Fase 4 - Tech challenge.pdf`
- Chris Richardson: *Microservices Patterns (Chapter 4: Managing transactions with sagas)*
- ADR-009: Pilha de Observabilidade Unificada com OpenTelemetry
- ADR-014: Decomposição em Microsserviços Especializados
