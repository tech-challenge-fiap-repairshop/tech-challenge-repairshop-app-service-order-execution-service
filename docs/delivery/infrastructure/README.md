# Infraestrutura e Modelagem de Dados (Fase 3 vs Fase 4)

Este diretório centraliza os diagramas de **infraestrutura** e **modelagem de dados** do ecossistema **RepairShop**, organizados por fase evolutiva conforme o padrão **Database-per-Service** (`ADR-016`) e a consolidação de insumos em documentos NoSQL (`ADR-019`).

> **Nota sobre SQL na Fase 4:** A arquitetura relacional utiliza **Amazon RDS PostgreSQL 16** (`register-db`, schema versionado via Flyway em `V1__init_register_schema.sql`), complementado por **Amazon ElastiCache Redis 7.0** (`user-login-cache`). As tabelas operacionais de OS, Execuções e Pagamentos foram migradas para **Amazon DynamoDB** (*Single-Table Design*).

---

## 1. Estrutura de Diretórios

```text
docs/delivery/infrastructure/
├── README.md
├── fase_3/                                   # Arquitetura Monolítica Legada (RDS Único)
│   ├── database-er-diagram.png               # Modelo ER relacional único (10 tabelas + cadeias de status)
│   ├── repairshop-diagrama-infra-cloud.png   # Topologia AWS da Fase 3 (PNG)
│   ├── repairshop-diagrama-infra-cloud.svg   # Topologia AWS da Fase 3 (SVG)
│   └── status_chain.png                      # Cadeia legada de estados (OS e Execution)
└── fase_4/                                   # Fase 4, organizada por tema (Draw.io + SVG + PNG)
    ├── dados/                                # Persistência poliglota por Bounded Context
    │   ├── diagrama_dados_sql_register_service.{drawio,drawio.svg,drawio.png}
    │   ├── diagrama_dados_nosql_service_order.{drawio,drawio.svg,drawio.png}
    │   ├── diagrama_dados_nosql_payment.{drawio,drawio.svg,drawio.png}
    │   └── diagrama_dados_poliglota_fase_4.{drawio,drawio.svg,drawio.png}
    ├── saga/                                 # Saga Pattern Coreografado
    │   └── diagrama_saga_pattern_fase_4.{drawio,drawio.svg,drawio.png}
    └── aws/                                  # Recursos AWS, rede e Security Groups
        └── diagrama_arquitetura_aws_fase_4.{drawio,drawio.svg,drawio.png}
```

---

## 2. Diagramas da Fase 4 por Contexto

### 2.1. Contexto 1 — SQL Relacional (`register-service`: RDS + Redis)
- **Arquivos:** [`diagrama_dados_sql_register_service.drawio`](fase_4/dados/diagrama_dados_sql_register_service.drawio) | [SVG](fase_4/dados/diagrama_dados_sql_register_service.drawio.svg) | [PNG](fase_4/dados/diagrama_dados_sql_register_service.drawio.png)
- **Bancos:** Amazon RDS PostgreSQL 16 (`register-db`) + Amazon ElastiCache Redis 7.0 (`user-login-cache`, porta `6379`).
- **Tabelas SQL (`V1__init_register_schema.sql`):**
  - `tb_user` (`UserEntity.kt`): credenciais com hash BCrypt, CPF e e-mail únicos, papel `UserRole` (`ATTENDANT` | `CUSTOMER`).
  - `tb_customer` (`CustomerEntity.kt`): dados cadastrais e conversores JPA (`DocumentConverter`, `EmailConverter`).
  - `tb_vehicle` (`VehicleEntity.kt`): vínculo `1 : 0..N` via `customer_id` (`ON DELETE CASCADE`) e placa única (`PlateConverter`).
- **Evolução:** A antiga tabela `tb_insume` foi removida do domínio relacional (`ADR-019`).

### 2.2. Contexto 2 — NoSQL DynamoDB (`service-order-execution-service`: `tb-service_order`)
- **Arquivos:** [`diagrama_dados_nosql_service_order.drawio`](fase_4/dados/diagrama_dados_nosql_service_order.drawio) | [SVG](fase_4/dados/diagrama_dados_nosql_service_order.drawio.svg) | [PNG](fase_4/dados/diagrama_dados_nosql_service_order.drawio.png)
- **Tabela:** `tb-service_order-<env>` (*Single-Table Design*, `PAY_PER_REQUEST`, PITR e criptografia SSE).
- **Partition Key / Sort Key:** `PK = ORDER#<service_order_id>` e `SK` polimórfica:
  - `METADATA` (`ServiceOrderMetadataDynamoItem.kt`)
  - `EXEC#<timestamp>#<execution_id>` (`ExecutionDynamoItem.kt`, com `List<InsumeDynamoItem>` embutida)
  - `EXTRA_EXEC#<timestamp>#<extra_execution_id>` (`ExtraExecutionDynamoItem.kt`, subfluxo de tarefas extras)
  - `HIST#<timestamp>#<history_id>` (`ServiceOrderHistoryDynamoItem.kt`, trilha de auditoria)
- **GSI:** `pending-dispatch-index` (`PK = payment_dispatch_status`, `SK = update_date`, projeção `ALL`) consumido pelo scheduler `scheduler_os_ready_for_payment`.

### 2.3. Contexto 3 — NoSQL DynamoDB (`payment-service`: `tb-payment`)
- **Arquivos:** [`diagrama_dados_nosql_payment.drawio`](fase_4/dados/diagrama_dados_nosql_payment.drawio) | [SVG](fase_4/dados/diagrama_dados_nosql_payment.drawio.svg) | [PNG](fase_4/dados/diagrama_dados_nosql_payment.drawio.png)
- **Tabela:** `tb-payment-<env>` (*Single-Table Design*, `PAY_PER_REQUEST`, PITR e criptografia SSE).
- **Partition Key / Sort Key:** `PK = BILLING#<service_order_id>` e `SK` polimórfica:
  - `PAY#<timestamp>#<payment_id>` (`PaymentDynamoItem.kt` — suporte a Mercado Pago QR, contingência PIX `FALLBACK-MP-*` via Circuit Breaker e campos de estorno `refund_reason` / `refund_date`).
  - `INV#<timestamp>#<invoice_id>` (`InvoiceDynamoItem.kt` — fatura emitida por cobrança).
- **GSI:** `dispatch-notification-index` (`PK = dispatch_notificate_status`, `SK = payment_date`, projeção `ALL`) consumido pelo scheduler `scheduler_os_payment_atualization`.

### 2.4. Visão Consolidada Poliglota (Fase 4)
- **Arquivos:** [`diagrama_dados_poliglota_fase_4.drawio`](fase_4/dados/diagrama_dados_poliglota_fase_4.drawio) | [SVG](fase_4/dados/diagrama_dados_poliglota_fase_4.drawio.svg) | [PNG](fase_4/dados/diagrama_dados_poliglota_fase_4.drawio.png)
- Consolida os três contextos persistentes (mais o papel *stateless* do `notification-service`) e o fluxo de referências lógicas (`customer_id`, `vehicle_id`, `service_order_id`) via API REST e filas SQS (`os-ready-for-payment-queue`, `payment-success-queue`).

### 2.5. Arquitetura do Saga Pattern Coreografado (Fase 4)
- **Arquivos:** [`diagrama_saga_pattern_fase_4.drawio`](fase_4/saga/diagrama_saga_pattern_fase_4.drawio) | [SVG](fase_4/saga/diagrama_saga_pattern_fase_4.drawio.svg) | [PNG](fase_4/saga/diagrama_saga_pattern_fase_4.drawio.png)
- Baseado nas `ADR-015`, `ADR-017`, `ADR-018`, `ADR-020` e `ADR-022`:
  - **Fluxo Forward (`T1`–`T6`)**: Coreografia distribuída entre os 4 microsserviços (`register-service`, `service-order-execution-service`, `notification-service`, `payment-service`) mediada por 5 pares de filas AWS SQS + DLQ (`approve-notification-queue`, `callback-approve-notification-queue`, `os-ready-for-payment-queue`, `payment-notification-queue`, `payment-success-queue`, com `maxReceiveCount = 3`, `visibilityTimeout = 60s` e retenção de 14 dias nas DLQs).
  - **Compensações Semânticas (`C1`–`C4`)**:
    - `C1`: Recusa de orçamento (`REFUSED` / `CANCELED`), sem acionamento de cobrança.
    - `C2`: Recusa de tarefas extras (`REFUSED_EXTRA_TASKS`), protegida por validação HTTP `422`, retornando para `IN_EXECUTION` preservando o preço original aprovado.
    - `C3`: Falha ou indisponibilidade do Mercado Pago protegida por Circuit Breaker (`FALLBACK-MP-*`) e retentativa segura em pagamentos recusados.
    - `C4`: Estorno pós-pagamento (`REFUNDED` com registro de `refund_reason` e `refund_date`) propagado para `CANCELED` na OS.
  - **Confiabilidade & Observabilidade**: Outbox Pattern via GSIs do DynamoDB (`pending-dispatch-index` em janela de 30m e `dispatch-notification-index` em janela de 15m), atomicidade Single-Table (`TransactWriteItems`), proteção contra Zombie Pods (`SqsListenerHealthIndicator` em `/actuator/health/liveness` + `preStop`) e propagação de contexto W3C (`traceparent` / `origin_trace_id`).

### 2.6. Arquitetura de Recursos AWS (Fase 4)
- **Arquivos:** [`aws/diagrama_arquitetura_aws_fase_4.drawio`](fase_4/aws/diagrama_arquitetura_aws_fase_4.drawio) | [SVG](fase_4/aws/diagrama_arquitetura_aws_fase_4.drawio.svg) | [PNG](fase_4/aws/diagrama_arquitetura_aws_fase_4.drawio.png)
- Desenho **somente de infraestrutura** (sem tabelas): VPC `10.0.0.0/16`, subnets públicas (IGW, NAT Gateway, 4 NLBs `:8080`) e privadas (EKS `1.31` com namespace `repairshop`, RDS PostgreSQL 16, ElastiCache Redis 7.0, `lambda-auth`), Security Groups (`eks_nodes`, `rds`, `redis`, `lambda_sg`), API Gateway HTTP API, DynamoDB, 5 filas SQS + 5 DLQs, ECR, S3 (tfstate), IAM `LabRole` (IMDSv2), Mercado Pago externo e CI/CD (GitHub Actions).

---

## 3. Tabela Comparativa de Migração (Fase 3 ➔ Fase 4)

| Tabela SQL Legada (Fase 3 — RDS Único) | Destino na Fase 4 | Banco / Tecnologia | Referência ADR |
| :--- | :--- | :--- | :--- |
| `tb_user` | `tb_user` | RDS PostgreSQL (`register-db`) + Redis | `ADR-002`, `ADR-016` |
| `tb_customer` | `tb_customer` | RDS PostgreSQL (`register-db`) | `ADR-002`, `ADR-016` |
| `tb_vehicle` | `tb_vehicle` | RDS PostgreSQL (`register-db`) | `ADR-002`, `ADR-016` |
| `tb_insume`, `tb_execution_insume` | Documento embutido `insumes: List<InsumeDynamoItem>` em `EXEC#` | DynamoDB (`tb-service_order`) | `ADR-019` |
| `tb_service_order` | `METADATA` (`ServiceOrderMetadataDynamoItem`) | DynamoDB (`tb-service_order`) | `ADR-016` |
| `tb_service_order_history` | `HIST#...` (`ServiceOrderHistoryDynamoItem`) | DynamoDB (`tb-service_order`) | `ADR-016` |
| `tb_execution`, `tb_execution_history` | `EXEC#...` (`ExecutionDynamoItem`) + `EXTRA_EXEC#...` | DynamoDB (`tb-service_order`) | `ADR-016` |
| `tb_invoice` | `INV#...` (`InvoiceDynamoItem`) + `PAY#...` (`PaymentDynamoItem`) | DynamoDB (`tb-payment`) | `ADR-016`, `ADR-018` |

