# ADR-016: Persistência Poliglota com Database per Service (SQL PostgreSQL e NoSQL DynamoDB)

**Data:** 2026-10-24  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

Com a decomposição do monólito em quatro microsserviços autônomos (`register-service`, `service-order-execution-service`, `payment-service` e `notification-service`), a estratégia de armazenamento precisa refletir as características intrínsecas de cada domínio de negócio (*Bounded Context*):
1. **Dados Cadastrais e Autenticação (`register-service`)**: Exigem integridade referencial estrita, unicidade de documentos/placas (CPF, email, placa veicular), relacionamentos 1:N clássicos (Cliente ➔ Veículos) e garantias transacionais ACID.
2. **Ordens de Serviço e Execução Técnica (`service-order-execution-service`)**: Volume elevado de eventos, mutabilidade intensa de tarefas e etapas de diagnóstico com estruturas de dados semi-estruturadas, exigindo baixa latência de leitura e alta escalabilidade horizontal sob picos de atendimento.
3. **Faturamento e Transações Financeiras (`payment-service`)**: Alta taxa de escrita de cobranças, recibos e webhooks de pagamento com necessidade de auditoria e flexibilidade de consulta por múltiplos índices secundários (GSIs).

O acoplamento através de uma base única compartilhada (*Shared Database*) violaria o princípio de independência operacional, criaria gargalos de locking e impediria a evolução independente dos modelos de dados.

---

## Decisão

Adotar o padrão **Database per Service** combinado com **Persistência Poliglota**, selecionando a tecnologia de banco ideal para cada carga de trabalho:

```
┌─────────────────────────────────┐      ┌─────────────────────────────────┐
│        register-service         │      │ service-order-execution-service │
└────────────────┬────────────────┘      └────────────────┬────────────────┘
                 │                                        │
                 ▼                                        ▼
    ┌─────────────────────────┐              ┌─────────────────────────┐
    │  PostgreSQL RDS (SQL)   │              │  DynamoDB (NoSQL)       │
    │  tabelas relacionais:   │              │  Single-Table Design:   │
    │  tb_user, tb_customer,  │              │  tb-service_order       │
    │  tb_vehicle             │              └─────────────────────────┘
    └─────────────────────────┘
                 ▲                                        ▲
                 │                                        │
┌────────────────┴────────────────┐      ┌────────────────┴────────────────┐
│   Redis (user-login-cache)      │      │         payment-service         │
│   Sessões JWT em memória        │      └────────────────┬────────────────┘
└─────────────────────────────────┘                       │
                                                          ▼
                                             ┌─────────────────────────┐
                                             │  DynamoDB (NoSQL)       │
                                             │  tb-payment             │
                                             └─────────────────────────┘
```

1. **PostgreSQL Relacional (AWS RDS `register-db`)**:
   - Alocado exclusivamente ao `register-service`.
   - Gerenciado por migrações versionadas com Flyway (`db/migration`).
   - Atende ao cadastro de usuários, clientes e veículos garantindo chaves estrangeiras, restrições UNIQUE e consistência forte.
2. **Cache em Memória (Redis `user-login-cache`)**:
   - Apoia o `register-service` no armazenamento de tokens de sessão, revogação de acessos e redução de queries repetitivas de validação cadastral.
3. **NoSQL Document / Key-Value (AWS DynamoDB `tb-service_order`)**:
   - Alocado ao `service-order-execution-service` sob **Single-Table Design**.
   - Chave composta (`PK: order#<id>`, `SK: metadata / HIST#... / EXEC#...`) unificando metadados, histórico e tarefas mecânicas na mesma partição física.
4. **NoSQL Document / Key-Value (AWS DynamoDB `tb-payment`)**:
   - Alocado ao `payment-service`.
   - Modela cobranças (`item_payment`) e faturas fiscais (`item_invoice`), indexados por Global Secondary Indexes (`pending-dispatch-index`, `dispatch_notificate_status`).

**Regra de Ouro**: É estritamente proibido o acesso direto de um microsserviço à base de dados de outro microsserviço. Toda interoperabilidade de dados é realizada exclusivamente via APIs HTTP REST ou mensagens assíncronas via AWS SQS.

---

## Consequências

### Positivas
- **Desacoplamento Total de Schemas**: Alterações na estrutura da OS não impactam o esquema cadastral do PostgreSQL nem exigem migrações coordenadas.
- **Escalabilidade Sob Medida**: O DynamoDB escala em modo *On-Demand (Pay-Per-Request)* suportando picos sem necessidade de provisionamento de conexões, enquanto o RDS PostgreSQL é protegido dentro de subnets privadas e acessado via pool HikariCP.
- **Otimização de Custos e Latência**: Queries de OS no DynamoDB realizam buscas pontuais por PK/SK com latência em dígito único de milissegundo.

### Desafios e Mitigações
- **Falta de Integridade Referencial Cruzada (Sem Foreign Keys entre Serviços)**: IDs de clientes e veículos são armazenados como atributos desnormalizados na OS. Mitigado através de validação síncrona na abertura da OS via cliente REST.
- **Complexidade Operacional Multi-Engine**: Mitigada com IaC via Terraform modularizado (`infra-db-dynamo`, `infra-redis`, `infra-services`) e simulação local unificada via LocalStack e Docker Compose.

---

## Referências
- Martin Fowler, *Polyglot Persistence* (2011).
- Chris Richardson, *Pattern: Database per service* (Microservices Patterns, 2018).
- Alex DeBrie, *The DynamoDB Book* (Single-Table Design patterns).
