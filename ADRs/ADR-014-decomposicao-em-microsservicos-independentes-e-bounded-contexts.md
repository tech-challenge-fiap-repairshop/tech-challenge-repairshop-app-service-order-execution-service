# ADR-014: Decomposição do Monólito em Microsserviços Especializados e Bounded Contexts

**Data:** 2026-10-22  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

Com o crescimento da oficina mecânica para escala nacional operando com múltiplas filiais, o volume de ordens de serviço, a complexidade de diagnósticos técnicos, as interações de faturamento e as exigências de alta disponibilidade aumentaram expressivamente.

O monólito desenvolvido nas fases anteriores, embora modular internamente sob Clean Architecture, apresenta limitações inerentes de escalabilidade operacional:
1. **Acoplamento de Ciclo de Vida**: Qualquer alteração no módulo de faturamento exige o reteste e redeploy de toda a aplicação de ordens de serviço e execução mecânica.
2. **Escala Ineficiente de Recursos**: Picos de consulta de status de OS demandam escalar toda a infraestrutura da aplicação, incluindo rotinas pesadas de relatórios e faturamento.
3. **Ponto Único de Falha (SPOF)**: Erros catastróficos no módulo de pagamentos ou orçamentos podem indisponibilizar a triagem e o atendimento físico nas oficinas.
4. **Requisito Obrigatório da Fase 4**: Separação em, no mínimo, 3 microsserviços independentes, cada um com repositório próprio, infraestrutura dedicada e banco de dados isolado.

---

## Decisão

Decompor a aplicação monolítica em **três microsserviços independentes**, orientados pelo padrão **Domain-Driven Design (DDD)** e mapeados em Bounded Contexts estritos:

```
                               ┌────────────────────────────────┐
                               │   AWS API Gateway (HTTP v2)    │
                               └───────────────┬────────────────┘
                                               │
               ┌───────────────────────────────┼───────────────────────────────┐
               │                               │                               │
               ▼                               ▼                               ▼
┌─────────────────────────────┐ ┌─────────────────────────────┐ ┌─────────────────────────────┐
│          OS Service         │ │       Billing Service       │ │      Execution Service      │
│  (Abertura, Status, Hist.)  │ │  (Orçamento e Mercado Pago) │ │ (Fila de Reparo e Produção) │
│                             │ │                             │ │                             │
│   Repo Git Independente     │ │    Repo Git Independente    │ │    Repo Git Independente    │
│   Banco de Dados Próprio    │ │    Banco de Dados Próprio   │ │    Banco NoSQL Próprio      │
│   CI/CD + K8s Deployment    │ │    CI/CD + K8s Deployment   │ │    CI/CD + K8s Deployment   │
└──────────────┬──────────────┘ └──────────────┬──────────────┘ └──────────────┬──────────────┘
               │                               │                               │
               └───────────────────────┬───────┴───────────────────────────────┘
                                       ▼
                       ┌───────────────────────────────┐
                       │     Broker de Mensageria      │
                       │   (RabbitMQ / AWS SQS-SNS)    │
                       └───────────────────────────────┘
```

### 1. OS Service (`tech-challenge-repairshop-os-service`)
- **Responsabilidade:** Ciclo de vida da Ordem de Serviço (criação, triagem do cliente, consulta de histórico e estado geral da OS).
- **Interface:** RESTful síncrona para consultas de clientes e mecânicos; publicação de eventos de transição de estado.
- **Persistência:** Relacional (SQL PostgreSQL dedicado).

### 2. Billing Service (`tech-challenge-repairshop-billing-service`)
- **Responsabilidade:** Cálculo orçamentário, precificação de peças/mão-de-obra, emissão de cobrança e integração oficial com o **Mercado Pago** (checkout, QR Code Pix e recepção de Webhooks).
- **Interface:** RESTful para aprovação/rejeição de orçamentos e Webhooks do Mercado Pago; disparo de eventos de pagamento confirmado ou cancelado.
- **Persistência:** Relacional (SQL PostgreSQL dedicado) com registros de transações financeiras e auditoria.

### 3. Execution Service (`tech-challenge-repairshop-execution-service`)
- **Responsabilidade:** Gestão da fila de produção, alocação de mecânicos, checklist de diagnósticos técnicos e registro de etapas de reparo.
- **Interface:** Consumo assíncrono de eventos de pagamentos liberados; endpoints RESTful para atualização da bancada mecânica.
- **Persistência:** Não-Relacional (NoSQL MongoDB / DynamoDB), otimizado para esquemas flexíveis de checklists e logs de execução.

---

## Consequências

### Positivas
- **Desacoplamento Total:** Cada microsserviço evolui, testa e é publicado em seu próprio ritmo, através de repositórios e esteiras de CI/CD independentes.
- **Escala Granular:** O `Execution Service` e o `OS Service` podem ser escalados no Kubernetes (HPA) conforme a demanda de clientes e mecânicos, sem onerar o `Billing Service`.
- **Isolamento de Falhas:** A indisponibilidade momentânea da API do Mercado Pago não impede que mecânicos continuem executando serviços na oficina.
- **Aderência aos Requisitos do Tech Challenge Fase 4**: Cumprimento estrito de 3 serviços, bancos dedicados (SQL + NoSQL) e repositórios independentes.

### Negativas / Desafios Mitigados
- **Complexidade de Consistência Distribuída:** A ausência de transações ACID locais entre serviços é mitigada pela introdução do **Saga Pattern** com transações de compensação (especificado na [ADR-015](ADR-015-gestao-transacional-distribuida-com-saga-pattern.md)).
- **Complexidade de Rede e Rastreamento:** Mitigada pela manutenção do **OpenTelemetry** e **Jaeger** (definido na [ADR-009](file:///c:/FIAP%20-%20ALOT%20-%20PROJETOS/tech-challenge-repairshop-app/ADRs/ADR-009-pilha-de-observabilidade-unificada-opentelemetry.md)), com propagação obrigatória do `traceparent` (W3C Trace Context).

---

## Referências
- Documento Oficial: `SOAT - Fase 4 - Tech challenge.pdf`
- ADR-001: Linguagem e Framework da Aplicação Principal
- ADR-003: Desmembramento em Micro-repositórios e Governança Git
- Martin Fowler: *Microservices Pattern* e *Bounded Context* (Eric Evans, DDD)
