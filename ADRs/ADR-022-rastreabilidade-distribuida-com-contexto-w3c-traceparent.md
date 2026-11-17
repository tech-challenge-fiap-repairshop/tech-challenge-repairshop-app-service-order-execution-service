# ADR-022: Rastreabilidade Distribuída Ponta a Ponta com Contexto W3C Traceparent (PF-10)

**Data:** 2026-10-24  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

Com a divisão da oficina em múltiplos microsserviços orientados a eventos, uma única requisição de abertura de Ordem de Serviço pelo cliente atravessa até 4 serviços e 3 filas SQS assíncronas ao longo de seu ciclo de vida.

Sem um padrão uniforme de contexto de telemetria distribuída (**PF-10**):
1. **Quebra de Rastro em Filas Assíncronas**: O OpenTelemetry Java Agent propaga traces automaticamente em requisições HTTP REST síncronas através de headers HTTP padrão. No entanto, em **mensagens assíncronas enviadas para o AWS SQS e tarefas agendadas por schedulers**, o contexto de rastreio (`Trace ID` e `Span ID`) é perdido na fronteira do broker.
2. **Dificuldade de Diagnóstico**: Quando uma mensagem falha na fila de pagamento ou no envio de aprovação, o time de engenharia não conseguia correlacionar o log de erro no `payment-service` com a transação original iniciada no `register-service`.

---

## Decisão

Adotar o padrão canônico **W3C Trace Context (`traceparent`)** para injeção e extração explícita de contexto de rastreabilidade em todas as comunicações assíncronas via AWS SQS e nas chamadas HTTP síncronas entre os serviços:

```
[1. register-service]
  Inicia transação: TraceId = 4bf92f3577b34da6a3ce929d0e0e4736
  Injeta HTTP Header: traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
        │
        ▼
[2. service-order-execution-service]
  Recebe requisição e mantém o mesmo TraceId
  Ao publicar no SQS (approve-notification-queue):
    Injeta MessageAttribute: "traceparent" = "00-4bf92f3577b34da6a3ce929d0e0e4736-..."
        │
        ▼ (AWS SQS)
[3. notification-service]
  Consome mensagem SQS
  Extrai MessageAttribute "traceparent"
  Vincula o contexto OpenTelemetry / MDC ao mesmo TraceId
  Envia e-mail e gera span filho: ParentSpanId = ...
        │
        ▼
[Jaeger / Grafana Loki]
  Exibe a cascata completa unificada sob uma única linha do tempo:
  [register-service] ➔ [service-order] ➔ [SQS] ➔ [notification-service]
```

### 1. Formato Canônico W3C `traceparent`
- Formato: `00-<trace_id_32_hex>-<parent_id_16_hex>-<trace_flags_2_hex>` (ex: `00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`).

### 2. Implementação Técnica nos Microsserviços
- **Produtores SQS (Producers)**:
  - Recuperam o `SpanContext` atual via `Tracer` do OpenTelemetry.
  - Injetam o atributo de mensagem SQS do tipo `String` com nome `traceparent` antes da chamada `sendMessage()`.
- **Consumidores SQS (Consumers)**:
  - Interceptam mensagens recebidas, verificam a presença do atributo `traceparent`.
  - Utilizam `W3CTraceContextPropagator.getInstance().extract()` para restaurar o contexto pai e abrir um novo `Span` filho com a mesma árvore de rastreamento.
- **Schedulers de Polling**:
  - Propagam o trace pai gravado no metadado da OS durante a varredura do scheduler, garantindo continuidade do rastro.
- **Log Correlation (MDC)**:
  - Configurado padrão de log nos quatro serviços:
    `logging.pattern.level=%5p [${spring.application.name:},%X{trace_id:-},%X{span_id:-}]`
  - Permite pesquisar diretamente no Grafana Loki pelo `trace_id` para correlacionar logs de diferentes containers.

---

## Consequências

### Positivas
- **Observabilidade Total da Saga**: Engenheiros e avaliadores podem inspecionar no Jaeger exatamente quanto tempo cada etapa da OS demorou em cada fila e container.
- **Mean Time to Resolution (MTTR) Mínimo**: Erros na esteira assíncrona são instantaneamente localizados com um clique a partir do trace correlacionado aos logs do Loki.
- **Conformidade com Padrões Abertos**: Uso estrito das especificações da Cloud Native Computing Foundation (CNCF) e W3C, sem lock-in com fornecedor de telemetria.

### Desafios e Mitigações
- **Overhead de Atributos em Mensagens**: O cabeçalho `traceparent` consome cerca de 55 bytes por mensagem, impacto insignificante perante o teto de 256 KB do SQS.

---

## Referências
- W3C Recommendation, *Trace Context Level 1* (2021).
- OpenTelemetry Documentation, *Context Propagation & OpenTelemetry Java Instrumentation*.
