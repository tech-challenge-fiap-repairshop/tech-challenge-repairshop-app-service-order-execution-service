# 🏛️ Architecture Decision Records (ADRs) - Oficina API (RepairShop)

Este diretório contém os **Architecture Decision Records (ADRs)** do projeto **Oficina API (RepairShop)**, desenvolvidos conforme o padrão e template de **Michael Nygard**.

Os ADRs documentam formalmente as decisões arquiteturais tomadas durante o ciclo de desenvolvimento e evolução do projeto (Fases 1, 2, 3 e 4), detalhando o **Contexto**, a **Decisão**, e suas **Consequências** (Positivas, Negativas/Trade-offs, Riscos e Mitigações).

---

## 📑 Catálogo de Decisões Arquiteturais

| ADR | Título | Status | Data | Área / Domínio |
| :--- | :--- | :---: | :---: | :--- |
| **[ADR-001](ADR-001-linguagem-e-framework-aplicacao-principal.md)** | Adoção de Kotlin e Spring Boot com Clean Architecture e DDD para o Core da Oficina | `Aceito` | 2026-09-01 | Desenvolvimento / Aplicação |
| **[ADR-002](ADR-002-banco-de-dados-relacional-postgresql.md)** | Escolha do Banco de Dados Relacional PostgreSQL via AWS RDS e Versionamento com Flyway | `Aceito` | 2026-09-01 | Persistência / Dados |
| **[ADR-003](ADR-003-desmembramento-em-micro-repositorios-e-governanca-git.md)** | Desmembramento da Arquitetura em Múltiplos Repositórios Especializados e Governança Git | `Aceito` | 2026-09-01 | Governança / Repositórios |
| **[ADR-004](ADR-004-infraestrutura-como-codigo-terraform-e-ambientes.md)** | Provisionamento de Infraestrutura como Código com Terraform e Segregação de Ambientes | `Aceito` | 2026-09-01 | Infraestrutura / IaC |
| **[ADR-005](ADR-005-topologia-de-rede-vpc-e-security-groups-descentralizados.md)** | Topologia de Rede VPC Unificada e Descentralização do Ciclo de Vida de Security Groups | `Aceito` | 2026-09-01 | Redes / Segurança |
| **[ADR-006](ADR-006-orquestracao-de-conteineres-com-kubernetes-aws-eks.md)** | Orquestração de Contêineres com AWS EKS e Escalonamento Automático via HPA | `Aceito` | 2026-09-01 | Computação / Orquestração |
| **[ADR-007](ADR-007-microsservico-serverless-de-autenticacao-aws-lambda-auth.md)** | Isolamento do Serviço de Autenticação em Microsserviço Serverless AWS Lambda e JWT Stateless | `Aceito` | 2026-09-01 | Segurança / Serverless |
| **[ADR-008](ADR-008-ponto-unico-de-entrada-com-aws-api-gateway.md)** | Ponto Único de Entrada com AWS API Gateway (HTTP API v2) e Roteamento Desacoplado | `Aceito` | 2026-09-01 | Ingress / Gateway |
| **[ADR-009](ADR-009-pilha-de-observabilidade-unificada-opentelemetry.md)** | Pilha de Observabilidade Unificada com OpenTelemetry, Prometheus, Jaeger e Grafana Loki | `Aceito` | 2026-09-01 | Observabilidade / Telemetria |
| **[ADR-010](ADR-010-esteira-ci-cd-automacao-de-testes-e-quality-gate.md)** | Automação de CI/CD com GitHub Actions, Testes com Testcontainers, Trivy e Quality Gate SonarCloud | `Aceito` | 2026-09-01 | CI/CD / Qualidade |
| **[ADR-011](ADR-011-notificacoes-assincronas-e-ambiente-de-emulacao-email.md)** | Notificações de Status de Ordem de Serviço e Interceptação em Desenvolvimento com Mailpit | `Aceito` | 2026-09-01 | Comunicação / Testes |
| **[ADR-012](ADR-012-procedimento-seguro-de-destruicao-de-infraestrutura.md)** | Procedimento Controlado de Destruição de Infraestrutura em Nuvem com Safety Gate | `Aceito` | 2026-09-01 | DevOps / Operação |
| **[ADR-013](ADR-013-substituicao-do-mailhog-pelo-mailpit-para-testes-de-email.md)** | Substituição do MailHog pelo Mailpit como Servidor SMTP de Testes e Interceptação de E-mails | `Aceito` | 2026-09-08 | Comunicação / Testes |
| **[ADR-014](ADR-014-decomposicao-em-microsservicos-independentes-e-bounded-contexts.md)** | Decomposição do Monólito em Microsserviços Especializados e Bounded Contexts | `Aceito` | 2026-10-22 | Arquitetura de Software / DDD |
| **[ADR-015](ADR-015-gestao-transacional-distribuida-com-saga-pattern.md)** | Gestão Transacional Distribuída com Saga Pattern e Mecanismos de Compensação | `Aceito` | 2026-10-22 | Padrões de Projeto / Resiliência |
| **[ADR-016](ADR-016-persistencia-poliglota-e-database-per-service.md)** | Persistência Poliglota com Database per Service (SQL PostgreSQL e NoSQL DynamoDB) | `Aceito` | 2026-10-24 | Banco de Dados / Armazenamento |
| **[ADR-017](ADR-017-mensageria-assincrona-e-integracao-desacoplada.md)** | Mensageria Assíncrona para Eventos Desacoplados e Orquestração com AWS SQS e DLQs | `Aceito` | 2026-10-24 | Integração / Mensageria |
| **[ADR-018](ADR-018-integracao-de-pagamentos-mercado-pago-e-webhooks.md)** | Integração de Pagamentos com Mercado Pago, Resiliência via Circuit Breaker e Webhooks | `Aceito` | 2026-10-24 | Meios de Pagamento / Checkout |
| **[ADR-019](ADR-019-remocao-do-crud-relacional-de-insumo-em-favor-de-modelo-nosql.md)** | Remoção do CRUD Relacional de Insumos em Favor de Modelo NoSQL Single-Table | `Aceito` | 2026-10-25 | Modelagem de Dados / Refatoração |
| **[ADR-020](ADR-020-resiliencia-em-kubernetes-contra-pods-zumbis-de-sqs-listeners.md)** | Resiliência em Kubernetes Contra Pods Zumbis de SQS Listeners (PF-11) | `Aceito` | 2026-10-24 | Alta Disponibilidade / Kubernetes |
| **[ADR-021](ADR-021-orquestracao-multi-servico-no-mesmo-cluster-eks-namespaces-e-hpa.md)** | Orquestração Multi-Serviço no Mesmo Cluster EKS, Namespaces Compartilhados e Autoscaling HPA | `Aceito` | 2026-10-25 | Infraestrutura Cloud / EKS |
| **[ADR-022](ADR-022-rastreabilidade-distribuida-com-contexto-w3c-traceparent.md)** | Rastreabilidade Distribuída Ponta a Ponta com Contexto W3C Traceparent (PF-10) | `Aceito` | 2026-10-24 | Observabilidade / OpenTelemetry |
| **[ADR-023](ADR-023-padronizacao-de-nomenclatura-de-repositorios-app-e-infra.md)** | Padronização de Nomenclatura de Repositórios Git — Taxonomia '-app-' e '-infra-' | `Aceito` | 2026-10-25 | Governança / Padronização de Repositórios |

---

## 📐 Padrão de Estrutura dos Registros (Template de Michael Nygard)

Cada registro de decisão arquitetural (ADR) deste diretório segue a estrutura canônica:

```markdown
# [Número]: [Título da Decisão Arquitetural]

**Data:** YYYY-MM-DD
**Status:** [Proposto | Aceito | Rejeitado | Depreciado | Substituído por ADR-XXX]
**Autor:** [Nome ou Grupo]

## Contexto
[Descrição do cenário, requisitos de negócio e técnicos, forças e restrições]

## Decisão
[Explicação detalhada e técnica da decisão tomada, componentes e abordagens adotadas]

## Consequências
### Positivas
- [Ganhos, benefícios e vantagens obtidas]

### Negativas / Trade-offs
- [Custos, complexidades adicionais e compensações aceitas]

### Riscos e Mitigações
- [Potenciais riscos identificados e ações preventivas implementadas]
```
