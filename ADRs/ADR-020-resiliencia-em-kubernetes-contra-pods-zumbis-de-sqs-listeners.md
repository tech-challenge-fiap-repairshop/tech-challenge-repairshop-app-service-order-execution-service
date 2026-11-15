# ADR-020: Resiliência em Kubernetes Contra Pods Zumbis de SQS Listeners (PF-11)

**Data:** 2026-10-24  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

Em arquiteturas orientadas a eventos operando no Kubernetes (AWS EKS), microsserviços utilizam listeners assíncronos (`@SqsListener` ou polling contínuo via AWS SDK) para processar mensagens de filas SQS.

Durante a operação de microsserviços em produção, observou-se o seguinte ponto de falha crítico (**PF-11**):
1. **Falha Silenciosa de Polling (Zombie Pods)**: Um pod pode sofrer travamento interno no loop de polling de mensagens (esgotamento de conexões com o endpoint SQS, thread starvation ou deadlock transitório).
2. **Inadequação das Probes Padrão**: As sondas de saúde padrão do Kubernetes (HTTP Actuator `/actuator/health`) testam apenas a capacidade do servidor web embutido (Tomcat/Netty) de responder a requisições HTTP e a conectividade com o banco. Dessa forma, o pod continua respondendo HTTP `200 OK` para o Kubelet, mas **deixa de consumir mensagens das filas SQS indefinidamente**.
3. **Efeito Cascata**: A fila SQS acumula mensagens sem consumo, inflando o tempo de espera do cliente e degradando o SLA de atendimento da oficina sem que o Kubernetes perceba a necessidade de reiniciar o pod (*Self-Healing*).

---

## Decisão

Implementar um padrão de **Resiliência Integrada entre Spring Boot Actuator e Kubernetes Lifecycle (PF-11)** em todos os microsserviços que consomem SQS:

```
┌────────────────────────────────────────────────────────┐
│             Kubernetes Kubelet (Node EKS)              │
└──────┬──────────────────────────────────────────▲──────┘
       │                                          │
 [HTTP GET /actuator/health/liveness]      [Health Status]
       │                                          │
       ▼                                          │
┌─────────────────────────────────────────────────┴──────┐
│                    Pod Spring Boot                     │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ SqsListenerHealthIndicator (Custom HealthIndicator)│
│  │                                                  │  │
│  │ - lastSuccessfulPollTimestamp                    │  │
│  │ - idleThresholdSeconds: 120s                     │  │
│  │ - isPollingHealthy(): boolean                    │  │
│  └──────────────────────────┬───────────────────────┘  │
│                             │                          │
│          ┌──────────────────┴──────────────────┐       │
│          │                                     │       │
│    [DENTRO DO PRAZO]                   [TIMEOUT > 120s]│
│          │                                     │       │
│          ▼                                     ▼       │
│    Status: UP (200 OK)               Status: DOWN (503)│
│    Kubelet mantém o Pod              Kubelet mata e    │
│                                      reinicia o Pod!   │
└────────────────────────────────────────────────────────┘
```

### 1. `SqsListenerHealthIndicator` Customizado
- Registra timestamp do último polling executado com sucesso ou mensagem recebida.
- Se o intervalo de inatividade exceder o limiar de tolerância (`payment.sqs.health-check.max-idle-seconds = 120s`), o indicador transiciona para `Status.DOWN` contendo diagnóstico detalhado da falha.

### 2. Segregação Estrita das Probes no Spring Boot Actuator
- **Readiness Probe (`/actuator/health/readiness`)**:
  - Avalia o estado de prontidão para receber tráfego HTTP de entrada e conectividade com bancos relacionais/NoSQL (`readinessState`, `db`).
- **Liveness Probe (`/actuator/health/liveness`)**:
  - Avalia se o pod está vivo e saudável para executar suas tarefas em segundo plano (`livenessState`, `sqsListener`).
  - Se o `sqsListener` entrar em estado `DOWN`, o endpoint retorna HTTP 503, acionando o Kubelet para **matar e recriar o pod imediatamente**, restabelecendo o ciclo de polling.

### 3. Graceful Shutdown e Hook `preStop`
- `server.shutdown=graceful` e `spring.lifecycle.timeout-per-shutdown-phase=30s` no `application.properties`.
- No manifesto K8s `deployment.yaml`:
  - `terminationGracePeriodSeconds: 45`: Concede tempo hábil para que mensagens em processamento ativo sejam concluídas antes do encerramento forçado (`SIGKILL`).
  - Hook `lifecycle.preStop.exec.command: ["/bin/sh", "-c", "sleep 5"]`: Aguarda que os endpoints do CoreDNS/kube-proxy retirem o IP do pod do roteamento antes de iniciar o teardown dos listeners.

---

## Consequências

### Positivas
- **Autocura Garantida (Zero Zombie Pods)**: Travamentos no consumo de mensagens SQS resultam no reinício automático do pod pelo orquestrador em menos de 2 minutos.
- **Drenagem Segura sem Perda de Mensagens**: Mensagens em trânsito não são descartadas abruptamente durante deploys ou reinícios planejados.
- **Observabilidade Clara**: Alertas do Actuator expõem o status dos listeners SQS nas métricas do Prometheus.

### Desafios e Mitigações
- **Risco de Falsos Positivos em Filas com Baixo Volume**: Em filas que raramente recebem mensagens, o listener poderia entrar em timeout por falta de tráfego. Mitigado através da configuração de *Long Polling* no SQS SDK (retornando empty poll com sucesso periódico) e possibilidade de desativação do health check em ambiente de desenvolvimento via flag `SQS_HEALTH_CHECK_ENABLED=false`.

---

## Referências
- Kubernetes Documentation, *Configure Liveness, Readiness and Startup Probes*.
- Spring Boot Reference Documentation, *Liveness and Readiness State & Writing Custom HealthIndicators*.
