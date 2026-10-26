# 🔧 Oficina API — Core Application & EKS Microservices

**Sistema Integrado de Atendimento, Diagnóstico e Execução de Serviços para Oficina Mecânica**

[![Java](https://img.shields.io/badge/Java-24-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-EKS-326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-Instrumented-F5A800?logo=opentelemetry&logoColor=white)](https://opentelemetry.io/)
[![Coverage](https://img.shields.io/badge/Coverage-80%25+-brightgreen)](docs/delivery/sonar/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=alexandre-agamin_tech-challenge-fiap&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=alexandre-agamin_tech-challenge-fiap)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

*Repositório central da aplicação de negócio (backend Kotlin/Spring Boot), manifestos de orquestração Kubernetes (`k8s/`), esteiras de CI/CD automatizadas e instrumentação completa de observabilidade.*

**POSTECH 15SOAT — Tech Challenge Fase 3 — Grupo CAO**

---

## 📑 Sumário

- [Visão Geral e Contexto](#-visão-geral-e-contexto)
- [Ecossistema de Microsserviços e Infraestrutura (Fase 3)](#-ecossistema-de-microsserviços-e-infraestrutura-fase-3)
  - [Topologia Cloud e Microsserviços](#-ecossistema-de-microsserviços-e-infraestrutura-fase-3)
  - [Repositórios da Organização](#repositórios-da-organização)
  - [Governança Técnica: Decisões Arquiteturais (ADRs) e Propostas Técnicas (RFCs)](#-governança-técnica-decisões-arquiteturais-adrs-e-propostas-técnicas-rfcs)
- [Arquitetura de Software](#-arquitetura-de-software)
  - [Domain-Driven Design (DDD)](#domain-driven-design-ddd)
  - [Clean Architecture (Módulos de Domínio)](#clean-architecture-módulos-de-domínio)
  - [Diagrama de Sequência (Autenticação e Abertura de OS)](#-diagrama-de-sequência-autenticação-e-abertura-de-ordem-de-serviço-os)
- [Arquitetura Cloud & Orquestração Kubernetes (`k8s/`)](#-arquitetura-cloud--orquestração-kubernetes-k8s)
  - [Topologia Cloud e Orquestração](#topologia-cloud-e-orquestração)
  - [Práticas de Resiliência Cloud-Native](#práticas-de-resiliência-cloud-native)
  - [Estrutura dos Manifestos Kubernetes](#estrutura-dos-manifestos-kubernetes)
  - [Deploy no Kubernetes (EKS e Local)](#deploy-no-kubernetes-eks-e-local)
- [Observabilidade e Distributed Tracing (OpenTelemetry Stack)](#-observabilidade-e-distributed-tracing-opentelemetry-stack)
- [Esteiras de Integração e Entrega Contínua (CI/CD GitOps)](#-esteiras-de-integração-e-entrega-contínua-cicd-gitops)
  - [Pipeline Principal de CI/CD (`ci.yml`)](#pipeline-principal-de-cicd-ciyml)
  - [Secrets e Variáveis de Ambiente](#secrets-e-variáveis-de-ambiente)
  - [Pipeline de Destruição Controlada (`destroy.yml`)](#pipeline-de-destruição-controlada-destroyyml)
- [Banco de Dados e Persistência (PostgreSQL 16)](#-banco-de-dados-e-persistência-postgresql-16)
- [Documentação da API e Endpoints](#-documentação-da-api-e-endpoints)
  - [Máquina de Estados da Ordem de Serviço (OS)](#-máquina-de-estados-da-ordem-de-serviço-os)
  - [Máquina de Estados da Execução de Serviço](#-máquina-de-estados-da-execução-de-serviço)
- [Como Executar Localmente](#-como-executar-localmente)
  - [Stack Completa via Docker Compose](#stack-completa-via-docker-compose)
  - [Portas e Painéis de Acesso](#-portas-e-painéis-de-acesso)
  - [Fluxo de Primeiro Uso (Swagger UI / Postman)](#-fluxo-de-primeiro-uso-swagger-ui--postman)
- [Qualidade de Código, Testes e Segurança](#-qualidade-de-código-testes-e-segurança)
- [Documentação DDD e Artefatos Complementares](#-documentação-ddd-e-artefatos-complementares)
- [Autores](#-autores)

---

## 🎯 Visão Geral e Contexto

O **RepairShop** é uma solução corporativa desenvolvida para digitalizar e otimizar ponta a ponta a operação de oficinas mecânicas: desde a recepção e cadastro de clientes e veículos, passando pela abertura de Ordens de Serviço (OS), orçamentação dinâmica, controle de estoque de peças/insumos, execução monitorada de serviços com controle de tempo até a emissão de faturas e notificações automatizadas aos clientes.

### Evolução Arquitetural por Fases

| Fase | Foco Principal | Destaques da Entrega |
| :--- | :--- | :--- |
| **Fase 1** | **Domínio e Modelagem de Negócio** | Aplicação de DDD estratégico e tático, Clean Architecture modular, regras de integridade transacional, CRUDs completos e cobertura de testes unitários/integrados com banco de dados. |
| **Fase 2** | **Orquestração e Escalabilidade** | Conteinerização Docker multi-stage, orquestração Kubernetes com HPA (autoscaling por CPU), instrumentação com OpenTelemetry e automação de deploys. |
| **Fase 3** | **Desacoplamento Cloud e Microsserviços** | Desmembramento da infraestrutura em repositórios dedicados gerenciados via Terraform, criação do microsserviço de autenticação Serverless (AWS Lambda Java 21), roteamento unificado via AWS API Gateway HTTP v2, isolamento de rede VPC privada com RDS PostgreSQL, governança técnica formal com RFCs e ADRs, e esteiras de GitOps multi-ambiente (`dev`, `hml`, `prd`). |

---

## 🌐 Ecossistema de Microsserviços e Infraestrutura (Fase 3)

Em conformidade com as diretrizes do **AWS Well-Architected Framework**, a solução foi desacoplada em repositórios especializados na organização do GitHub:

```mermaid
flowchart TD
    %% Definições de Estilo
    classDef clientStyle fill:#ECEFF1,stroke:#607D8B,stroke-width:2px,color:#263238
    classDef apigwStyle fill:#FCE4EC,stroke:#C2185B,stroke-width:2px,color:#880E4F
    classDef lambdaStyle fill:#FFF3E0,stroke:#E65100,stroke-width:2px,color:#BF360C
    classDef vpcStyle fill:#F5F7FA,stroke:#0277BD,stroke-width:2px,color:#01579B,stroke-dasharray: 4 4
    classDef eksStyle fill:#E8EAF6,stroke:#3F51B5,stroke-width:2px,color:#1A237E
    classDef podStyle fill:#FFFFFF,stroke:#00ACC1,stroke-width:1.5px,color:#006064
    classDef otelStyle fill:#F3E5F5,stroke:#8E24AA,stroke-width:1.5px,color:#4A148C
    classDef rdsStyle fill:#E8F5E9,stroke:#2E7D32,stroke-width:2px,color:#1B5E20
    classDef nlbStyle fill:#E1F5FE,stroke:#0288D1,stroke-width:2px,color:#01579B

    User([👤 Cliente / Atendente / Admin]):::clientStyle -->|"HTTPS (Porta 443)"| APIGW["🚪 AWS API Gateway HTTP API v2\n(tech-challenge-repairshop-infra-apigateway)"]:::apigwStyle
    
    APIGW -->|"POST /auth/login (JWT)"| LambdaAuth["⚡ Lambda Auth Java 21\n(tech-challenge-repairshop-lambda-auth)"]:::lambdaStyle
    APIGW -->|"ANY /{proxy+} (Catch-all)"| NLB["⚖️ AWS Network Load Balancer (NLB)"]:::nlbStyle
    
    subgraph VPC["🏢 AWS VPC Privada — tech-challenge-repairshop-infra-network"]
        subgraph EKS_Cluster["☸️ Cluster EKS Gerenciado — tech-challenge-repairshop-infra-eks"]
            NLB --> AppService["☸️ Service LoadBalancer (Porta 8080)"]:::nlbStyle
            AppService --> AppPod["📦 repairshop-app Pods (Spring Boot + OTel Agent)\nNamespace: repairshop"]:::podStyle
            AppPod -.->|"Traces / Logs / Metrics"| OTelPod["🔭 OpenTelemetry Collector & Observability Stack"]:::otelStyle
        end
        
        subgraph Private_Data["🔒 Camada de Dados Privada (RDS PostgreSQL 16)"]
            RDS["🗄️ AWS RDS PostgreSQL 16\n(tech-challenge-repairshop-infra-db-rds)"]:::rdsStyle
            AppPod ==>|"TCP:5432 (JPA / Flyway)"| RDS
            LambdaAuth -.->|"TCP:5432 (Validação de CPF)"| RDS
        end
    end
    class VPC vpcStyle
    class EKS_Cluster eksStyle
    class Private_Data rdsStyle
```

### Repositórios da Organização

1. **[`tech-challenge-repairshop-app`](.) (Este Repositório):** Código-fonte do backend principal (Kotlin/Spring Boot/Java 24), manifestos Kubernetes (`k8s/`), configurações de observabilidade e pipeline de CI/CD da aplicação.
2. **[`tech-challenge-repairshop-infra-network`](https://github.com/fiap-postech-repairshop/tech-challenge-repairshop-infra-network):** Infraestrutura base de rede na AWS (VPC, Subnets Públicas/Privadas, Internet Gateway, NAT Gateway, Route Tables e repositórios AWS ECR).
3. **[`tech-challenge-repairshop-infra-eks`](https://github.com/fiap-postech-repairshop/tech-challenge-repairshop-infra-eks):** Provisionamento do Cluster AWS EKS, Managed Node Groups, Security Groups (`aws_security_group.eks_nodes`) e Metrics Server.
4. **[`tech-challenge-repairshop-lambda-auth`](https://github.com/fiap-postech-repairshop/tech-challenge-repairshop-lambda-auth):** Função Serverless em AWS Lambda (Java 21) com Clean Architecture para autenticação (`POST /auth/login`), Security Group próprio e geração de tokens JWT seguros.
5. **[`tech-challenge-repairshop-infra-apigateway`](https://github.com/fiap-postech-repairshop/tech-challenge-repairshop-infra-apigateway):** Provisionamento do AWS API Gateway HTTP v2 com roteamento dinâmico para a Lambda Auth e Proxy Transparente para o Load Balancer do EKS.
6. **[`tech-challenge-repairshop-infra-db-rds`](https://github.com/fiap-postech-repairshop/tech-challenge-repairshop-infra-db-rds):** Provisionamento da instância gerenciada AWS RDS PostgreSQL 16 nas sub-redes privadas com Security Group dedicado (`aws_security_group.rds`).
7. **[`tech-challenge-wiki-docs`](https://github.com/fiap-postech-repairshop/tech-challenge-wiki-docs):** Documentação centralizada da arquitetura, ADRs, diagramas estruturados e scripts de orquestração unificada (`create_all_infra` / `destroy_all_infra`).

### 📜 Governança Técnica: Decisões Arquiteturais (ADRs) e Propostas Técnicas (RFCs)

Como pilar central da evolução da **Fase 3**, todas as decisões de engenharia — incluindo migração cloud-native, segregação em micro-repositórios, modelo serverless de autenticação, governança de redes e banco de dados isolado, automação CI/CD e telemetria — foram desenvolvidas seguindo um modelo rigoroso de maturidade: discussão em **RFCs (Requests for Comments)** e formalização em **ADRs (Architecture Decision Records)** no padrão de Michael Nygard.

```
[ Necessidade / Demanda ] ──> [ RFC (Proposta Técnica & Alternativas) ] ──> [ Discussão & Consenso ] ──> [ ADR (Decisão Consolidada) ] ──> [ Entrega IaC / App ]
```

#### 🏛️ Catálogo de ADRs (Architecture Decision Records)
> 📁 A documentação completa com contexto, decisão detalhada, consequências, trade-offs e mitigações está disponível no diretório [`ADRs/`](ADRs/README.md).

| ADR | Decisão Arquitetural | Status | Domínio | Impacto Principal na Fase 3 |
| :--- | :--- | :---: | :--- | :--- |
| **[ADR-001](ADRs/ADR-001-linguagem-e-framework-aplicacao-principal.md)** | Adoção de Kotlin e Spring Boot com Clean Architecture e DDD | `Aceito` | Aplicação / Core | Preservação do domínio isolado de frameworks e manutenção de contratos ricos. |
| **[ADR-002](ADRs/ADR-002-banco-de-dados-relacional-postgresql.md)** | PostgreSQL via AWS RDS e Versionamento com Flyway | `Aceito` | Dados / Persistência | Instância gerenciada em sub-redes privadas com migrações de schema automatizadas. |
| **[ADR-003](ADRs/ADR-003-desmembramento-em-micro-repositorios-e-governanca-git.md)** | Desmembramento da Arquitetura em Múltiplos Repositórios Especializados e Governança Git | `Aceito` | Governança / Repositórios | Isolamento do ciclo de vida em 7 repositórios independentes e fluxo `branch -> homolog -> main`. |
| **[ADR-004](ADRs/ADR-004-infraestrutura-como-codigo-terraform-e-ambientes.md)** | Provisionamento de Infraestrutura como Código com Terraform e Ambientes | `Aceito` | Infraestrutura / IaC | Automação declarativa de IaC com workspaces/diretórios segregados (`dev`, `hml`, `prd`). |
| **[ADR-005](ADRs/ADR-005-topologia-de-rede-vpc-e-security-groups-descentralizados.md)** | Topologia de Rede VPC Unificada e Security Groups Descentralizados | `Aceito` | Redes / Segurança | Princípio de privilégio mínimo: cada componente provisiona e gerencia seus próprios Security Groups. |
| **[ADR-006](ADRs/ADR-006-orquestracao-de-conteineres-com-kubernetes-aws-eks.md)** | Orquestração de Contêineres com AWS EKS e Escalonamento com HPA | `Aceito` | Computação / EKS | Deploy resiliente no Kubernetes gerenciado com auto-scaling horizontal orientado a carga de CPU. |
| **[ADR-007](ADRs/ADR-007-microsservico-serverless-de-autenticacao-aws-lambda-auth.md)** | Microsserviço Serverless AWS Lambda Auth (Java 21) e JWT Stateless | `Aceito` | Segurança / Serverless | Desacoplamento da autenticação com custo sob demanda e emissão padronizada de JWT por CPF. |
| **[ADR-008](ADRs/ADR-008-ponto-unico-de-entrada-com-aws-api-gateway.md)** | Ponto Único de Entrada com AWS API Gateway (HTTP API v2) | `Aceito` | Ingress / Gateway | Roteamento unificado e transparente entre Lambda Serverless (`/auth/*`) e Pods EKS (`/{proxy+}`). |
| **[ADR-009](ADRs/ADR-009-pilha-de-observabilidade-unificada-opentelemetry.md)** | Pilha de Observabilidade Unificada com OpenTelemetry, Prometheus, Jaeger e Loki | `Aceito` | Observabilidade | Rastreamento distribuído com OTel Java Agent, métricas de negócio e dashboards executivos no Grafana. |
| **[ADR-010](ADRs/ADR-010-esteira-ci-cd-automacao-de-testes-e-quality-gate.md)** | Automação de CI/CD com GitHub Actions, Testcontainers, Trivy e SonarCloud | `Aceito` | CI/CD / Qualidade | Esteira automatizada de build, testes integrados com banco efêmero, scan de CVEs e Quality Gate estrito. |
| **[ADR-011](ADRs/ADR-011-notificacoes-assincronas-e-ambiente-de-emulacao-email.md)** | Notificações de Status de Ordem de Serviço e Interceptação com Mailpit | `Aceito` | Notificações / Testes | Comunicação assíncrona para transições de status da OS e ambiente seguro de emulação de e-mails. |
| **[ADR-012](ADRs/ADR-012-procedimento-seguro-de-destruicao-de-infraestrutura.md)** | Procedimento Controlado de Destruição de Infraestrutura com Safety Gate | `Aceito` | DevOps / Teardown | Proteção contra deleções acidentais via Safety Gate de confirmação (`CONFIRM_DESTROY=true`). |
| **[ADR-013](ADRs/ADR-013-substituicao-do-mailhog-pelo-mailpit-para-testes-de-email.md)** | Substituição do MailHog pelo Mailpit como Servidor SMTP de Testes | `Aceito` | Emulação / SMTP | Solução moderna e ativa em Go para testes de entrega de e-mail e inspeção via UI/API REST. |

#### 📜 Catálogo de RFCs (Requests for Comments)
> 📁 A documentação completa com motivação, alternativas descartadas e propostas técnicas detalhadas está disponível no diretório [`RFCs/`](RFCs/README.md).

| RFC | Proposta Técnica | Status | ADR Vinculada | Domínio / Área |
| :--- | :--- | :---: | :---: | :--- |
| **[RFC-001](RFCs/RFC-001-adocao-kotlin-spring-boot-clean-architecture.md)** | Adoção de Kotlin e Spring Boot com Clean Architecture e DDD | `Encerrada – Aprovada` | [ADR-001](ADRs/ADR-001-linguagem-e-framework-aplicacao-principal.md) | Aplicação / Core |
| **[RFC-002](RFCs/RFC-002-banco-de-dados-relacional-postgresql-rds.md)** | Escolha do Banco de Dados Relacional PostgreSQL via RDS e Flyway | `Encerrada – Aprovada` | [ADR-002](ADRs/ADR-002-banco-de-dados-relacional-postgresql.md) | Dados / Persistência |
| **[RFC-003](RFCs/RFC-003-desmembramento-em-micro-repositorios-e-governanca-git.md)** | Desmembramento da Arquitetura em Múltiplos Repositórios e Governança Git | `Encerrada – Aprovada` | [ADR-003](ADRs/ADR-003-desmembramento-em-micro-repositorios-e-governanca-git.md) | Governança / Repositórios |
| **[RFC-004](RFCs/RFC-004-infraestrutura-como-codigo-terraform-e-ambientes.md)** | Provisionamento de Infraestrutura como Código com Terraform | `Encerrada – Aprovada` | [ADR-004](ADRs/ADR-004-infraestrutura-como-codigo-terraform-e-ambientes.md) | Infraestrutura / IaC |
| **[RFC-005](RFCs/RFC-005-topologia-de-rede-vpc-e-security-groups-descentralizados.md)** | Topologia de Rede VPC Unificada e Security Groups Descentralizados | `Encerrada – Aprovada` | [ADR-005](ADRs/ADR-005-topologia-de-rede-vpc-e-security-groups-descentralizados.md) | Redes / Segurança |
| **[RFC-006](RFCs/RFC-006-orquestracao-de-conteineres-com-kubernetes-aws-eks.md)** | Orquestração de Contêineres com AWS EKS e Escalonamento com HPA | `Encerrada – Aprovada` | [ADR-006](ADRs/ADR-006-orquestracao-de-conteineres-com-kubernetes-aws-eks.md) | Computação / Orquestração |
| **[RFC-007](RFCs/RFC-007-microsservico-serverless-de-autenticacao-aws-lambda-auth.md)** | Isolamento do Serviço de Autenticação em AWS Lambda e JWT Stateless | `Encerrada – Aprovada` | [ADR-007](ADRs/ADR-007-microsservico-serverless-de-autenticacao-aws-lambda-auth.md) | Segurança / Serverless |
| **[RFC-008](RFCs/RFC-008-ponto-unico-de-entrada-com-aws-api-gateway.md)** | Ponto Único de Entrada com AWS API Gateway (HTTP API v2) | `Encerrada – Aprovada` | [ADR-008](ADRs/ADR-008-ponto-unico-de-entrada-com-aws-api-gateway.md) | Ingress / Gateway |
| **[RFC-009](RFCs/RFC-009-pilha-de-observabilidade-unificada-opentelemetry.md)** | Pilha de Observabilidade Unificada com OpenTelemetry, Prometheus e Grafana | `Encerrada – Aprovada` | [ADR-009](ADRs/ADR-009-pilha-de-observabilidade-unificada-opentelemetry.md) | Observabilidade / Telemetria |
| **[RFC-010](RFCs/RFC-010-esteira-ci-cd-automacao-de-testes-e-quality-gate.md)** | Automação de CI/CD com GitHub Actions, Testes, Trivy e SonarCloud | `Encerrada – Aprovada` | [ADR-010](ADRs/ADR-010-esteira-ci-cd-automacao-de-testes-e-quality-gate.md) | CI/CD / Qualidade |
| **[RFC-011](RFCs/RFC-011-notificacoes-assincronas-e-ambiente-de-emulacao-email.md)** | Notificações de Status de OS e Interceptação com Mailpit | `Encerrada – Aprovada` | [ADR-011](ADRs/ADR-011-notificacoes-assincronas-e-ambiente-de-emulacao-email.md) | Notificações / Testes |
| **[RFC-012](RFCs/RFC-012-procedimento-seguro-de-destruicao-de-infraestrutura.md)** | Procedimento Controlado de Destruição de Infraestrutura com Safety Gate | `Encerrada – Aprovada` | [ADR-012](ADRs/ADR-012-procedimento-seguro-de-destruicao-de-infraestrutura.md) | DevOps / Operação |
| **[RFC-013](RFCs/RFC-013-substituicao-do-mailhog-pelo-mailpit-para-testes-de-email.md)** | Substituição do MailHog pelo Mailpit como Servidor SMTP de Testes | `Encerrada – Aprovada` | [ADR-013](ADRs/ADR-013-substituicao-do-mailhog-pelo-mailpit-para-testes-de-email.md) | Emulação / SMTP |
| **[RFC-014](RFCs/RFC-014-dashboard-executivo-grafana-metricas-gerenciais-e-negocio.md)** | Dashboard Executivo e Exposição de Métricas de Negócio com Grafana | `Encerrada – Aprovada` | [ADR-009](ADRs/ADR-009-pilha-de-observabilidade-unificada-opentelemetry.md) | Observabilidade / Negócios |

---

## 🏛️ Arquitetura de Software

### Domain-Driven Design (DDD)

A modelagem do sistema seguiu rigorosamente os preceitos de DDD estratégico e tático, estabelecendo uma Linguagem Ubíqua clara e delimitando os seguintes **Bounded Contexts**:

- **Customer & Vehicle Management (`register`):** Gestão cadastral de clientes (validação de CPF/CNPJ) e seus veículos vinculados (placas, modelos, anos).
- **Service Order Management (`serviceorder`):** Ciclo de vida da Ordem de Serviço, cálculo automático de orçamento, aprovação/recusa de clientes e histórico de transições.
- **Service Execution (`execution`):** Gestão dos serviços individuais aplicados à OS e monitoramento do tempo médio de execução.
- **Inventory & Insumes (`inventory`):** Controle de estoque de peças e insumos com baixa atômica e prevenção de concorrência.
- **Billing & Invoices (`payment`):** Geração de faturas e liquidação financeira da OS.
- **User & Security (`user` / `core`):** Gerenciamento de credenciais e validação de tokens JWT.

<div align="center">
  <img src="docs/delivery/clean_arquiteture/diagrama_entidades_e_contextos.png" alt="Diagrama de Entidades e Contextos DDD" width="850">
  <br>
  <em><small><strong>Figura 1: Diagrama de Entidades e Contextos do Domínio (DDD)</strong></small></em>
  <br><br>
</div>

### Clean Architecture (Módulos de Domínio)

Para assegurar independência de frameworks e isolamento do domínio, cada módulo de negócio no código-fonte ([`src/main/kotlin/com/cao/repairshop/`](src/main/kotlin/com/cao/repairshop/)) é subdividido em três camadas estritas:

```
src/main/kotlin/com/cao/repairshop/
├── core/                       # Configurações globais, segurança, interceptors e utilitários
├── execution/                  # Bounded Context: Execução de Serviços da OS
│   ├── application/            # Use Cases e Interfaces de Gateways
│   ├── domain/                 # Entidades puras, Value Objects e Enums
│   └── infra/                  # REST Controllers, Gateways e Repositórios JPA
├── inventory/                  # Bounded Context: Peças, Insumos e Estoque
├── payment/                    # Bounded Context: Faturamento e Invoices
├── register/                   # Bounded Context: Clientes e Veículos
├── serviceorder/               # Bounded Context: Ordens de Serviço
└── user/                       # Bounded Context: Usuários e Autenticação
```

- **`domain` (Camada Central):** Entidades de negócio puras (`Entity`), objetos de valor (`Value Objects`), regras de cálculo e contratos fundamentais. Livre de anotações de frameworks (como JPA ou Spring).
- **`application` (Camada de Orquestração):** Casos de uso (`Use Cases`) que implementam os fluxos de negócio e definem interfaces de saída (`Gateways`) aplicando o Princípio da Inversão de Dependência (DIP).
- **`infra` (Camada Externa de I/O):** Implementações concretas de Gateways, Controllers REST (`Spring Web`), DTOs com validações Bean Validation e mapeamentos de persistência com `Spring Data JPA` e `Hibernate`.

<div align="center">
  <img src="docs/delivery/clean_arquiteture/diagrama_arquitetura.png" alt="Diagrama de Camadas da Clean Architecture" width="850">
  <br>
  <em><small><strong>Figura 2: Estrutura em Camadas da Clean Architecture no Backend</strong></small></em>
  <br><br>
</div>

### 🔄 Diagrama de Sequência: Autenticação e Abertura de Ordem de Serviço (OS)

O fluxo ponta a ponta abrange desde a validação defensiva de credenciais e emissão do token JWT na borda (AWS Lambda Auth) até a autorização via Spring Security, a execução transacional do caso de uso de abertura de Ordem de Serviço (OS) e a persistência consistente no PostgreSQL RDS, com rastreabilidade distribuída via OpenTelemetry:

<div align="center">
  <img src="docs/delivery/sequence/diagrama_sequencia_auth_abertura_os.drawio.png" alt="Diagrama de Sequência da Autenticação até a Abertura de Ordem de Serviço (OS)" width="950">
  <br>
  <em><small><strong>Figura 3: Diagrama de Sequência: Autenticação (Lambda Java 21) até Abertura de OS (Spring Boot / EKS)</strong></small></em>
  <br><br>
</div>

> 💡 **Arquivos do Diagrama de Sequência no Repositório:**
> - [diagrama_sequencia_auth_abertura_os.drawio.png](docs/delivery/sequence/diagrama_sequencia_auth_abertura_os.drawio.png) *(Imagem PNG de alta resolução para visualização direta)*
> - [diagrama_sequencia_auth_abertura_os.drawio.svg](docs/delivery/sequence/diagrama_sequencia_auth_abertura_os.drawio.svg) *(Renderização vetorial SVG)*
> - [diagrama_sequencia_auth_abertura_os.drawio](docs/delivery/sequence/diagrama_sequencia_auth_abertura_os.drawio) *(Arquivo de modelagem editável no Diagrams.net / Draw.io)*

#### Detalhamento das Fases do Fluxo:
1. **Fase 1 — Autenticação e Emissão de Token JWT (`POST /auth/login`):**
   - **Borda & Roteamento:** O cliente envia CPF e senha ao AWS API Gateway HTTP v2, que roteia a requisição via integração `AWS_PROXY` para a função Serverless **AWS Lambda Auth** (desenvolvida em Java 21 sob os princípios de Clean Architecture).
   - **Validação Defensiva:** O `InputValidator` valida formato e dígitos verificadores do CPF antes de qualquer chamada interna (retornando `400 Bad Request` imediatamente caso inválido).
   - **Autenticação:** O caso de uso `AuthenticateUseCase` aciona o backend via OpenFeign Client (`POST /auth/login`). A aplicação consulta a entidade `User` no PostgreSQL e valida a senha criptografada via BCrypt.
   - **Emissão & Headers OWASP:** O `JwtService` emite o token assinado (HMAC-SHA256, expiração de 24h). A Lambda injeta cabeçalhos de proteção (`HSTS`, `X-Frame-Options`, `X-Content-Type-Options`, `Cache-Control: no-store`) e retorna o payload com `200 OK`.
2. **Fase 2 — Abertura de Ordem de Serviço (OS) Protegida (`POST /service-orders`):**
   - **Roteamento & Proxy:** O cliente envia `POST /service-orders` com cabeçalho `Authorization: Bearer <token>`. O API Gateway intercepta via rota catch-all `ANY /{proxy+}` e encaminha via Load Balancer (NLB) aos Pods da aplicação no EKS (namespace `repairshop`).
   - **Segurança (Spring Security):** O `JwtAuthenticationFilter` intercepta a chamada, valida a assinatura e expiração do JWT via `JwtService` e registra o usuário no `SecurityContextHolder` (retornando `401 Unauthorized` se o token for inválido ou ausente).
   - **Caso de Uso Transacional:** O controller valida o payload (`@Valid CreateServiceOrderRequest`) e invoca `CreateServiceOrderImpl` em contexto `@Transactional`.
   - **Consistência de Domínio:** São validados e recuperados o Cliente (`CustomerGateway`) e o Veículo (`VehicleGateway`). A entidade agregadora `ServiceOrder` é instanciada e os insumos de cada serviço são resolvidos (`InsumeGateway`) e calculados via `order.addExecution`.
   - **Persistência & Notificação:** O status inicial `RECEIVED` é registrado no histórico (`order.recordHistory`), a OS é persistida no PostgreSQL (`ServiceOrderGateway.save`), a notificação ao cliente é disparada (`NotifyCustomer`) e spans de rastreamento W3C são enviados assincronamente ao **OTel Collector**.
   - **Resposta:** A API retorna status HTTP `201 Created` contendo o payload estruturado da Ordem de Serviço.

---

## ☸️ Arquitetura Cloud & Orquestração Kubernetes (`k8s/`)

### Topologia Cloud e Orquestração

A aplicação opera conteinerizada e orquestrada no **Amazon Elastic Kubernetes Service (EKS)**, integrando-se aos recursos gerenciados da AWS através de rede VPC segura.

<div align="center">
  <img src="docs/delivery/infrastructure/repairshop-diagrama-infra-cloud.svg" alt="Diagrama de Infraestrutura Cloud e Orquestração EKS" width="900">
  <br>
  <em><small><strong>Figura 4: Topologia Integrada da Infraestrutura AWS e Orquestração Kubernetes</strong></small></em>
  <br><br>
</div>

### Práticas de Resiliência Cloud-Native

O manifesto [`k8s/deployment.yaml`](k8s/deployment.yaml) incorpora as principais práticas recomendadas pelo **Well-Architected Framework**:

1. **Execução Segura Não-Root:** O container executa sob o usuário não-privilegiado `springuser` (`UID 10001`), com `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true` e remoção total de capacidades (`capabilities.drop: ["ALL"]`).
2. **Health Checks e Probes:**
   - `livenessProbe` em `/actuator/health/liveness` para reinicialização automática de pods travados.
   - `readinessProbe` em `/actuator/health/readiness` para direcionamento de tráfego somente após a inicialização completa do Spring Boot e migrações do Flyway.
3. **Escalonamento Automático com HPA ([`k8s/hpa.yaml`](k8s/hpa.yaml)):** O *Horizontal Pod Autoscaler* gerencia réplicas dinâmicas (mínimo de 1 e máximo de 3 pods) mantendo a utilização média de CPU em 70%.
4. **Deploy Zero-Downtime:** Estratégia de `RollingUpdate` configurada com `maxSurge: 1` e `maxUnavailable: 0`.

### Estrutura dos Manifestos Kubernetes

```
k8s/
├── namespace.yaml              # Namespace dedicado 'repairshop'
├── deployment.yaml             # Deployment Spring Boot com Probes, Recursos e Agente OTel
├── service.yaml                # Service LoadBalancer (NLB AWS porta 8080)
├── hpa.yaml                    # Horizontal Pod Autoscaler (HPA 1-3 réplicas)
├── secret.yaml                 # Template de Secrets para credenciais e JWT
├── mailpit.yaml                # Pod e Service do Mailpit (ambiente dev/local)
├── otel-collector.yaml         # OpenTelemetry Collector Gateway (portas 4317/4318/8889)
├── prometheus.yaml             # Prometheus Server (porta 9090)
├── jaeger.yaml                 # Jaeger UI para Tracing Distribuído (porta 16686)
├── loki.yaml                   # Loki Ingestion para Agregação de Logs (porta 3100)
├── grafana.yaml                # Grafana com Dashboards e Datasources provisionados (porta 3000)
│
├── configs/                    # 📄 Configurações de Observabilidade
│   ├── otel-collector-config.yaml
│   ├── prometheus-config.yaml
│   ├── loki-config.yaml
│   ├── jaeger-config.yaml
│   ├── grafana-datasources-config.yaml
│   ├── grafana-dashboards-provider-config.yaml
│   └── grafana-dashboards-config.yaml
│
└── configmap/                  # ⚙️ ConfigMaps por Ambiente AWS
    ├── configmap-dev.yaml      # ConfigMap DEV (Mailpit ativo, log DEBUG)
    ├── configmap-hml.yaml      # ConfigMap HML (log INFO)
    └── configmap-prd.yaml      # ConfigMap PRD (SMTP real, log INFO)

local/                          # 🚀 Recursos e Configurações Exclusivas do Ambiente Local
├── kustomization.yaml          # Kustomize para o ambiente local (sem duplicar manifestos)
├── configmap-local.yaml        # ConfigMap do ambiente local (Kind / Docker Desktop)
├── postgres-local.yaml         # Pod e Service Postgres para testes locais sem AWS
└── observability/              # Configurações e dashboards do Docker Compose local
```

### Deploy no Kubernetes (EKS e Local)

#### 1. Deploy no Cluster AWS EKS

```bash
# 1. Configurar contexto do EKS via AWS CLI
aws eks update-kubeconfig --region us-east-1 --name repairshop-eks-dev

# 2. Garantir namespace
kubectl apply -f k8s/namespace.yaml

# 3. Aplicar configurações de observabilidade e manifestos principais
kubectl apply -f k8s/configs/
kubectl apply -f k8s/

# 4. Aplicar o ConfigMap correspondente ao ambiente (ex: dev)
kubectl apply -f k8s/configmap/configmap-dev.yaml

# 5. Acompanhar a inicialização
kubectl rollout status deployment/repairshop-app -n repairshop --timeout=5m
```

#### 2. Deploy Local Unificado via Kustomize (Docker Desktop / Kind / Minikube)

O projeto disponibiliza um overlay declarativo no diretório [`local/`](local/) contendo **apenas** o arquivo [`kustomization.yaml`](local/kustomization.yaml). Ele reutiliza diretamente todos os manifestos originais de `k8s/` sem nenhuma duplicação de arquivos, aplicando dinamicamente a imagem local (`repairshop:latest`), `imagePullPolicy: IfNotPresent` e os recursos locais (`configmap-local.yaml` e `postgres-local.yaml`):

```bash
# 1. Realizar o build da imagem Docker localmente
docker build -t repairshop:latest .

# 2. Subir a stack completa reaproveitando k8s/ sem duplicar nenhum arquivo
kubectl kustomize --load-restrictor LoadRestrictionsNone local | kubectl apply -f -

# 3. Acompanhar a inicialização e validação das migrações do Flyway
kubectl rollout status deployment/repairshop-app -n repairshop --timeout=5m
```

> 💡 **Acesso aos Painéis Locais via Port-Forward:**
> ```bash
> kubectl port-forward svc/repairshop-service 8080:8080 -n repairshop  # API / Swagger UI
> kubectl port-forward svc/grafana 3000:3000 -n repairshop             # Grafana Dashboards
> kubectl port-forward svc/jaeger 16686:16686 -n repairshop           # Jaeger Tracing UI
> kubectl port-forward svc/mailpit 8025:8025 -n repairshop            # Mailpit Webmail
> ```
> Para desmontar o ambiente local por completo:
> ```bash
> kubectl kustomize --load-restrictor LoadRestrictionsNone local | kubectl delete -f -
> # ou simplesmente:
> kubectl delete namespace repairshop
> ```

---

## 🔭 Observabilidade e Distributed Tracing (OpenTelemetry Stack)

A aplicação foi projetada com observabilidade de primeira classe baseada no ecossistema **OpenTelemetry (OTel)**:

```
[ RepairShop App (JVM) ]
        │  (opentelemetry-javaagent.jar via OTLP gRPC)
        ▼
[ OTel Collector Gateway (k8s / docker) ]
   ├──▶ Traces  ──▶ [ Jaeger UI (16686) ]
   ├──▶ Métricas──▶ [ Prometheus (9090) ]
   └──▶ Logs    ──▶ [ Loki (3100) ]
                            │
                            ▼
                   [ Grafana Dashboard (3000) ]
```

1. **Auto-Instrumentação JVM:** O [`Dockerfile`](Dockerfile) acopla o `opentelemetry-javaagent.jar` diretamente no entrypoint da JVM, capturando automaticamente métricas de runtime, chamadas JDBC, requisições HTTP e spans de execução.
2. **OpenTelemetry Collector:** Recebe dados via OTLP gRPC (`:4317`) e HTTP (`:4318`), processa e roteia para os backends específicos.
3. **Jaeger Tracing:** Fornece rastreamento distribuído ponta a ponta com visualização da cascata de spans por endpoint.
4. **Prometheus & Actuator:** Coleta contínua de métricas de saúde, JVM, garbage collector e tempos de resposta HTTP.
5. **Grafana Loki & Logback:** Ingestão estruturada de logs emitidos pela aplicação via appender OTel nativo.
6. **Grafana Dashboards:** Painéis pré-provisionados consolidando métricas, logs e traces em uma interface única.

---

## 🚀 Esteiras de Integração e Entrega Contínua (CI/CD GitOps)

### Pipeline Principal de CI/CD (`ci.yml`)

A pipeline do GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) é acionada a cada `push` ou `pull_request` nas branches `main`, `homolog`, `dev` e `develop`, além de permitir disparo manual via `workflow_dispatch` escolhendo o ambiente de destino (`dev`, `hml`, `prd`).

```mermaid
flowchart TD
    classDef triggerStyle fill:#E1F5FE,stroke:#0288D1,stroke-width:2px,color:#01579B
    classDef stepStyle fill:#F3E5F5,stroke:#7B1FA2,stroke-width:2px,color:#4A148C
    classDef gateStyle fill:#FFF9C4,stroke:#FBC02D,stroke-width:2px,color:#F57F17
    classDef deployStyle fill:#E8F5E9,stroke:#388E3C,stroke-width:2px,color:#1B5E20
    classDef reportStyle fill:#ECEFF1,stroke:#455A64,stroke-width:2px,color:#263238

    A["🎯 Gatilho / Trigger\n• Push ou PR (main, homolog, dev)\n• Workflow Dispatch Manual"]:::triggerStyle
    A --> B["⚙️ Setup do Ambiente\n(JDK 24 Temurin + Cache Maven)"]:::stepStyle
    B --> C["🧪 Testes Unitários & Integração\n(Testcontainers Postgres + JaCoCo Coverage)"]:::stepStyle
    C --> D["🔍 Quality Gate SonarCloud\n(Análise Estática > 80% Cobertura)"]:::stepStyle
    D --> E["🐳 Build Docker Multi-Stage\n(eclipse-temurin:24-jre Minimal Image)"]:::stepStyle
    E --> F["🛡️ Security Scan DevSecOps\n(Trivy Vulnerability Scanner)"]:::stepStyle
    F --> G["📦 Autenticação AWS & Push ECR\n(Amazon ECR Private Registry)"]:::stepStyle
    G --> H{"🌿 Branch é 'main' com Push\nou Dispatch Manual?"}:::gateStyle
    
    H -- "✅ Sim (Deploy Produção)" --> I["☸️ Deploy Automatizado AWS EKS\n(kubectl apply / RollingUpdate)"]:::deployStyle
    H -- "🛡️ Não (PR ou Homologação)" --> J["📋 Validação & Simulação\n(Testes e Build Aprovados)"]:::reportStyle
    
    I --> K["📊 GitHub Step Summary & Relatórios"]:::reportStyle
    J --> K
```

#### Detalhamento e Justificativa de Cada Passo da Pipeline

| Passo | Ação Executada | Justificativa Arquitetural |
| :--- | :--- | :--- |
| **1. Checkout Repository** | Baixa o código-fonte na revisão exata do commit. | Assegura que o build e os testes reflitam com fidelidade o estado do código versionado. |
| **2. Set up JDK & Maven Cache** | Instala o OpenJDK 24 com cache de dependências `.m2`. | Acelera o tempo de compilação em até 70%, reduzindo download de dependências externas. |
| **3. Testes com Testcontainers** | Sobe container real do PostgreSQL para os testes de integração. | Garante paridade com o ambiente de produção, testando queries SQL e constraints reais sem mocks frágeis. |
| **4. JaCoCo & SonarCloud** | Calcula cobertura de código e submete métricas ao SonarCloud. | Bloqueia código com débito técnico, vulnerabilidades ou cobertura de testes inferior a 80%. |
| **5. Build Multi-Stage Docker** | Compila a imagem Docker final com runtime minimal JRE. | Minimiza a superfície de ataque e reduz o tamanho final da imagem para menos de 250MB. |
| **6. Security Scan (Trivy)** | Varre a imagem Docker em busca de vulnerabilidades de CVEs. | Conformidade DevSecOps antes que qualquer binário seja promovido para o Container Registry. |
| **7. Push para o Amazon ECR** | Realiza tag semântica e push da imagem no registro privado da AWS. | Garante que imagens imutáveis estejam disponíveis para download seguro pelos nós do EKS. |
| **8. Deploy Contínuo no EKS** | Injeta variáveis/secrets e aplica os manifests Kubernetes (`k8s/`). | Atualiza os Pods com estratégia *RollingUpdate* sem indisponibilidade de serviço (*zero-downtime*). |
| **9. Rollout Status Verification** | Aguarda confirmação de prontidão (`kubectl rollout status`). | Previne que deploys com falhas de inicialização ou crashloop passem despercebidos. |

---

### 🔐 Secrets do GitHub Actions (AWS Academy & Aplicação)

Para que as esteiras de CI/CD executem o build, testes, análise de segurança, push no Amazon ECR e deploy no Amazon EKS com sucesso, o repositório requer as seguintes **Actions Secrets** (*Settings > Secrets and variables > Actions*):

#### 1. Credenciais AWS (Obrigatórias para Deploy no ECR / EKS)
> [!TIP]
> Em contas de estudantes da **AWS Academy**, as credenciais são temporárias (sessões de 3 a 4 horas). Por essa razão, a inclusão do `AWS_SESSION_TOKEN` é mandatória para evitar falhas de `ExpiredToken`.

| Secret | Obrigatório | Descrição |
| :--- | :---: | :--- |
| `AWS_ACCESS_KEY_ID` | **Sim** | Chave de acesso temporária fornecida no painel do AWS Academy. |
| `AWS_SECRET_ACCESS_KEY` | **Sim** | Chave secreta de acesso correspondente. |
| `AWS_SESSION_TOKEN` | **Sim** | Token da sessão temporária (necessário para o `LabRole`). |

💡 *Dica de Automação:* Utilize o script [`update_aws_secrets.ps1`](https://github.com/tech-challenge-fiap-repairshop/tech-challenge-wiki-docs/blob/main/update_aws_secrets.ps1) disponível no repositório `tech-challenge-wiki-docs` para atualizar essas credenciais em todos os 7 repositórios da organização simultaneamente via GitHub CLI.

#### 2. Secrets da Aplicação e Banco de Dados (Opcionais com Fallback Seguro)

| Secret | Obrigatório | Padrão / Fallback | Finalidade |
| :--- | :---: | :--- | :--- |
| `SPRING_DATASOURCE_USERNAME` | Não | `repairshop` | Usuário de autenticação no PostgreSQL RDS. |
| `SPRING_DATASOURCE_PASSWORD` | Não | `repairshop` | Senha de autenticação no PostgreSQL RDS. |
| `JWT_SECRET` | Não | *Chave embutida no secret.yaml* | Chave simétrica HMAC-SHA256 para assinatura e validação de tokens JWT. |

---

### 🌐 Variáveis de Ambiente Necessárias (Pipeline & Kubernetes)

A esteira e os contêineres operam com as seguintes variáveis de configuração:

#### 1. Variáveis de Execução da Pipeline (`env` no `ci.yml`)

| Variável | Valor Padrão | Descrição |
| :--- | :---: | :--- |
| `AWS_REGION` | `us-east-1` | Região AWS onde o EKS, ECR e RDS estão provisionados. |
| `JAVA_VERSION` | `24` | Versão do OpenJDK utilizada na compilação do projeto. |
| `JAVA_DISTRIBUTION` | `temurin` | Distribuição Eclipse Temurin gerenciada pelo GitHub Actions. |

#### 2. Variáveis de Ambiente do Container (`k8s/configmap/` e `deployment.yaml`)

| Variável | Origem | Descrição |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `configmap-{env}.yaml` | Perfil de execução Spring Boot ativo (`dev`, `hml`, `prd`). |
| `SPRING_DATASOURCE_URL` | Injeção dinâmica no deploy | URL JDBC resolvida dinamicamente para o RDS (`jdbc:postgresql://<rds-host>:5432/repairshop`). |
| `OTEL_SERVICE_NAME` | `k8s/deployment.yaml` | Identificador do serviço na telemetria (`repairshop-app`). |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `k8s/deployment.yaml` | Endpoint do OpenTelemetry Collector interno (`http://otel-collector:4317`). |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `k8s/deployment.yaml` | Protocolo de transporte de telemetria (`grpc`). |
| `OTEL_LOGS_EXPORTER` / `TRACES` / `METRICS` | `k8s/deployment.yaml` | Definidos como `otlp` para despacho unificado de observabilidade. |

---

### 💥 Pipeline de Destruição Controlada (`destroy.yml`)

Para prevenir cobranças desnecessárias na conta AWS e liberar as interfaces de rede (ENIs) associadas aos Load Balancers antes de destruir o cluster ou a VPC, o repositório inclui a esteira de teardown automatizada [`.github/workflows/destroy.yml`](.github/workflows/destroy.yml):

- **Gatilho:** Manual via `workflow_dispatch`.
- **Safety Gate:** Exige a digitação explícita de `DESTRUIR` na confirmação para evitar exclusões acidentais.
- **Ação Executada:** Remove de forma limpa os recursos do namespace `repairshop` (Deployments, Services NLB, ConfigMaps e Observabilidade) e aguarda 30 segundos para liberação das ENIs na AWS.

---

## 🔀 Governança de Branches e Ciclo de Promoção (Git Flow)

A governança do repositório segue isolamento estrito com aprovação controlada para promoção de ambientes:

```mermaid
flowchart LR
    classDef branchDev fill:#E3F2FD,stroke:#1E88E5,stroke-width:2px,color:#0D47A1
    classDef branchHml fill:#FFF3E0,stroke:#FB8C00,stroke-width:2px,color:#E65100
    classDef branchMain fill:#E8F5E9,stroke:#43A047,stroke-width:2px,color:#1B5E20
    classDef gateStyle fill:#FFEBEE,stroke:#E53935,stroke-width:2px,color:#B71C1C

    Dev["🌿 Feature / Fix / Chore\n(feat/*, fix/*, chore/*)"]:::branchDev
    PR_HML{"Pull Request\npara homolog"}:::gateStyle
    HML["🛡️ Branch homolog\n(Ambiente hml / Validação)"]:::branchHml
    PR_MAIN{"Pull Request\npara main"}:::gateStyle
    Main["🚀 Branch main\n(Deploy em Produção)"]:::branchMain

    Dev -->|"Abertura de PR"| PR_HML
    PR_HML -->|"Validação & Merge"| HML
    HML -->|"Abertura de PR de Promoção"| PR_MAIN
    PR_MAIN -->|"Aprovação Manual Obrigatória"| Main
```

> ⚠️ **Regra de Governança:** É expressamente proibido commit ou push direto na branch `main`. Toda alteração deve passar pelo pipeline de validação e aprovação formal.

---

## 🗄️ Banco de Dados e Persistência (PostgreSQL 16)

### 1. Justificativa Formal da Escolha do PostgreSQL 16 (SGBD Relacional)

A escolha do **PostgreSQL 16** como banco de dados relacional foi fundamentada nas características do domínio de negócio de uma Oficina Mecânica:

| Critério Arquitetural | Justificativa Técnica no Domínio de Oficina |
| :--- | :--- |
| **Conformidade ACID Rigorosa** | O ciclo de vida da Ordem de Serviço envolve transições financeiras e contratuais críticas (`RECEIVED` $\rightarrow$ `IN_DIAGNOSIS` $\rightarrow$ `APPROVED` $\rightarrow$ `FINALIZED` $\rightarrow$ `PAID`), faturamento e emissão de notas fiscais. Operações com dinheiro e garantias legais exigem atomicidade e consistência estritas. |
| **Integridade Relacional Forte** | O domínio possui entidades fortemente interconectadas (Cliente $\rightarrow$ Veículo $\rightarrow$ Ordem de Serviço $\rightarrow$ Execuções $\rightarrow$ Peças/Insumos $\rightarrow$ Fatura). A garantia de integridade referencial via chaves estrangeiras (`FOREIGN KEY`) e constraints é indispensável para evitar dados inconsistentes. |
| **Controle Concorrente de Estoque** | A baixa e reserva de peças/insumos durante o diagnóstico de serviços concomitantes exige níveis estritos de isolamento transacional (*Read Committed* / *Repeatable Read* com *Row-Level Locking* / `SELECT FOR UPDATE`) para evitar concorrência desordenada (*lost updates* ou estoque negativo). |
| **Evolução Homogênea com Flyway** | Migrations determinísticas versionadas em código (`src/main/resources/db/migration/`) garantem rastreabilidade, repetibilidade e evolução uniforme entre os ambientes de `dev`, `hml` e `prd`. |

### 2. Dicionário de Dados, Entidades e Cardinalidades

```mermaid
erDiagram
    tb_customer ||--o{ tb_vehicle : "possui (1:N)"
    tb_customer ||--o{ tb_service_order : "solicita (1:N)"
    tb_customer ||--o{ tb_invoice : "faturado para (1:N)"
    tb_vehicle ||--o{ tb_service_order : "recebe manutencao (1:N)"
    tb_service_order ||--o{ tb_service_order_history : "auditoria de status (1:N)"
    tb_service_order ||--o{ tb_execution : "composta por (1:N)"
    tb_service_order ||--|| tb_invoice : "gera (1:1)"
    tb_execution ||--o{ tb_execution_history : "auditoria de execucao (1:N)"
    tb_execution ||--|{ tb_execution_insume : "utiliza (1:N)"
    tb_insume ||--|{ tb_execution_insume : "consumido em (1:N)"
    tb_user {
        UUID id_tb_user PK
        VARCHAR name
        VARCHAR function
        VARCHAR cpf UK
        VARCHAR email UK
        VARCHAR password
    }
```

- **`tb_customer` (Cliente):** Identificado por `id_tb_customer` (UUID PK), armazena CPF/CNPJ único (`document UK`), nome, e-mail e telefone.
- **`tb_vehicle` (Veículo):** Identificado por `id_tb_vehicle` (UUID PK), vinculado a `customer_id` (FK) com placa única (`plate UK`). Cardinalidade: **1 Cliente para N Veículos ($1:N$)**.
- **`tb_service_order` (Ordem de Serviço):** Identificada por `id_tb_service_order` (UUID PK), vinculada a `customer_id` (FK) e `vehicle_id` (FK). Controla o status da OS, valor total e prazos. Cardinalidade: **1 Veículo para N Ordens de Serviço ($1:N$)**.
- **`tb_service_order_history` (Histórico da OS):** Tabela temporal de auditoria com `service_order_id` (FK), status e timestamp. Permite o cálculo do tempo médio de permanência em cada status. Cardinalidade: **1 OS para N Históricos ($1:N$)**.
- **`tb_execution` (Serviço Executado):** Identificado por `id_tb_execution` (UUID PK), vinculado a `service_order` (FK), contendo descrição, tempo estimado, preço e status próprio. Cardinalidade: **1 OS para N Execuções ($1:N$)**.
- **`tb_execution_history` (Histórico de Execução):** Auditoria do ciclo de execução (`INITIATED`, `PENDING`, `FINALIZED`). Cardinalidade: **1 Execução para N Históricos ($1:N$)**.
- **`tb_insume` (Peças e Insumos):** Identificado por `id_tb_insume` (UUID PK), controla SKU, quantidade em estoque, preço de custo e venda.
- **`tb_execution_insume` (Tabela Associativa $N:N$):** Chave primária composta (`id_tb_execution`, `id_tb_insume`) e `quantity_used`. Vincula peças consumidas a cada serviço executado.
- **`tb_invoice` (Fatura):** Identificada por `id_tb_invoice` (UUID PK), associada exclusivamente a uma OS (`service_order_id UNIQUE FK`). Cardinalidade: **1 OS para 1 Fatura ($1:1$)**.
- **`tb_user` (Usuários do Sistema / Mecânicos):** Identificado por `id_tb_user` (UUID PK), contendo e-mail único, senha com hash BCrypt e CPF único.

### 3. Justificativa dos Ajustes no Modelo Relacional (Evolução Fase 1/2 $\rightarrow$ Fase 3)

1. **Inclusão da Coluna `cpf` em `tb_user` (`V3__add_cpf_to_tb_user.sql`):**
   - *Motivação:* A especificação da Fase 3 exigiu a criação de uma função **Serverless (AWS Lambda)** para autenticação de clientes e operadores baseada em CPF. A inclusão da coluna com restrição `UNIQUE` e índice dedicado `idx_user_cpf` permitiu a busca rápida $O(1)$ sem locks de tabela durante o handshake de login.
2. **Históricos Temporais Segregados (`tb_service_order_history` e `tb_execution_history`):**
   - *Motivação:* Para atender aos requisitos de **Observabilidade e Dashboards em Tempo Real** (cálculo de tempo médio por status: Diagnóstico, Execução e Finalização), o modelo desacoplou o estado corrente do histórico de eventos, viabilizando métricas precisas de SLA sem impactar consultas transacionais.
3. **Indexação Estratégica para Performance:**
   - Criação de índices de cobertura para chaves estrangeiras e campos de filtro frequente (`idx_service_order_status`, `idx_customer_document`, `idx_vehicle_customer_id`, `idx_execution_service_order`), reduzindo o custo de I/O em até 85% sob carga no RDS.

<div align="center">
  <img src="docs/delivery/infrastructure/database-er-diagram.png" alt="Diagrama de Entidade e Relacionamento (ERD)" width="850">
  <br>
  <em><small><strong>Figura 4: Diagrama de Entidade e Relacionamento do Banco PostgreSQL (ERD)</strong></small></em>
  <br><br>
</div>

---

### 4. Justificativa da Escolha do Flyway para Versionamento do Banco de Dados

A evolução do banco de dados relacional foi implementada via **Flyway Migration** (`src/main/resources/db/migration/`), estabelecendo uma divisão de responsabilidades limpa:

1. **Separação de Responsabilidades com a Infraestrutura (SoC):**
   - O **Terraform** (`tech-challenge-repairshop-infra-db-rds`) gerencia unicamente a infraestrutura física gerenciada da AWS (instância RDS, discos gp3, sub-redes privadas e Security Groups).
   - O **Flyway** (`tech-challenge-repairshop-app`) gerencia a evolução lógica do esquema (tabelas, colunas, chaves estrangeiras e índices). Isso previne que modificações de infraestrutura gerem locks ou `DROP TABLE` acidentais na base de produção.
2. **Ciclo de Vida Acoplado ao Domínio:**
   - As migrações executam automaticamente na inicialização dos pods da aplicação no EKS, assegurando que o código Kotlin/Spring Boot sempre opere sobre a versão exata do schema requerido pelas entidades e Value Objects.
3. **Integridade Garantida via Checksums:**
   - A tabela `flyway_schema_history` audita cada script SQL executado com checksums SHA-256 e timestamps, garantindo idempotência e reprodutibilidade idêntica no Docker Compose (local), nos Testcontainers (CI) e no RDS (AWS).

---

## 📖 Documentação da API e Endpoints

A documentação interativa da API está disponível via **Swagger UI / OpenAPI 3.0** no endereço:

```
http://localhost:8080/swagger-ui/index.html
```

### Principais Endpoints da API

| Bounded Context | Método | Rota | Descrição |
| :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/auth/login` | Autenticação e emissão de token JWT |
| | `POST` | `/auth/register` | Cadastro de novos usuários (demonstrativo) |
| **Customers** | `POST` | `/customers` | Cadastro de novo cliente (valida CPF/CNPJ) |
| | `GET` | `/customers` | Listagem paginada de clientes |
| | `GET` | `/customers/{id}` | Consulta de cliente por ID |
| | `PUT` | `/customers/{id}` | Atualização de dados cadastrais |
| | `DELETE` | `/customers/{id}` | Remoção de cliente |
| **Vehicles** | `POST` | `/vehicles` | Cadastro de veículo vinculado ao cliente |
| | `GET` | `/vehicles` | Listagem paginada de veículos |
| | `GET` | `/vehicles/{id}` | Consulta de veículo por ID |
| | `PUT` | `/vehicles/{id}` | Atualização de dados do veículo |
| | `DELETE` | `/vehicles/{id}` | Remoção de veículo |
| **Insumes (Estoque)**| `POST` | `/insumes` | Cadastro de peça/insumo |
| | `GET` | `/insumes` | Listagem de insumos e saldo de estoque |
| | `GET` | `/insumes/{id}` | Consulta de insumo por ID |
| | `PUT` | `/insumes/{id}` | Atualização de insumo/estoque |
| | `DELETE` | `/insumes/{id}` | Remoção de insumo |
| **Service Orders** | `POST` | `/service-orders` | Abertura de OS com cálculo de orçamento |
| | `GET` | `/service-orders` | Listagem paginada de Ordens de Serviço |
| | `GET` | `/service-orders/{id}` | Consulta pública de acompanhamento da OS |
| | `PATCH` | `/service-orders/{id}/status` | Avanço do status da OS |
| | `POST` | `/service-orders/{id}/approve` | Aprovação ou recusa do orçamento pelo cliente |
| | `GET` | `/service-orders/metrics` | Métricas consolidadas das Ordens de Serviço |
| | `GET` | `/service-orders/executions/metrics` | Métricas de tempo médio de execução |
| **Executions** | `POST` | `/service-orders/{id}/executions` | Inclusão de serviço na OS |
| | `POST` | `/service-orders/{id}/executions/batch` | Inclusão em lote de serviços na OS |
| | `GET` | `/service-orders/{id}/executions/{execId}` | Detalhes da execução do serviço |
| | `PATCH` | `/service-orders/{id}/executions/{execId}/status` | Atualização do status da execução |
| | `DELETE` | `/service-orders/{id}/executions/{execId}` | Remoção de serviço da OS |
| **Invoices** | `POST` | `/invoices` | Faturamento e emissão de nota da OS |
| | `GET` | `/invoices` | Listagem paginada de faturas |
| | `GET` | `/invoices/{id}` | Consulta de fatura por ID |

### ⚙️ Máquina de Estados da Ordem de Serviço (OS)

O ciclo de vida da OS é protegido por uma máquina de estados com transições estritas:

```mermaid
flowchart TD
    %% Estilos de Estados
    classDef initialStyle fill:#ECEFF1,stroke:#607D8B,stroke-width:2px,color:#263238
    classDef stepStyle fill:#E3F2FD,stroke:#1E88E5,stroke-width:2px,color:#0D47A1
    classDef gateStyle fill:#FFF9C4,stroke:#FBC02D,stroke-width:2px,color:#F57F17
    classDef successStyle fill:#E8F5E9,stroke:#43A047,stroke-width:2px,color:#1B5E20
    classDef cancelStyle fill:#FFEBEE,stroke:#E53935,stroke-width:2px,color:#B71C1C

    RECEIVED["📥 RECEIVED\n(Ordem Aberta)"]:::initialStyle -->|"Início do Diagnóstico"| IN_DIAGNOSIS["🔍 IN_DIAGNOSIS\n(Diagnóstico Técnico)"]:::stepStyle
    IN_DIAGNOSIS -->|"Envio para Aprovação"| WAITING_APPROVAL{"⏳ WAITING_APPROVAL\n(Decisão do Cliente)"}:::gateStyle
    
    WAITING_APPROVAL -->|"POST /service-orders/{id}/approve (APPROVED)"| APPROVED["✅ APPROVED\n(Orçamento Aceito)"]:::successStyle
    WAITING_APPROVAL -->|"POST /service-orders/{id}/approve (REFUSED)"| REFUSED["❌ REFUSED\n(Cliente Recusou)"]:::cancelStyle
    
    APPROVED -->|"Início dos Reparos"| IN_EXECUTION["🛠️ IN_EXECUTION\n(Serviço em Andamento)"]:::stepStyle
    REFUSED -->|"Sem Reparos"| CANCELED["🚫 CANCELED\n(OS Cancelada)"]:::cancelStyle
    
    IN_EXECUTION -->|"Término dos Serviços"| FINALIZED["🏁 FINALIZED\n(Pronto para Faturamento)"]:::stepStyle
    FINALIZED -->|"POST /invoices (Fatura Gerada)"| PAID["💰 PAID\n(OS Quitada e Entregue)"]:::successStyle
```

> ⚠️ **Regra de Negócio:** A transição a partir de `WAITING_APPROVAL` não pode ser realizada via `PATCH /status`. É mandatório chamar o endpoint específico `POST /service-orders/{id}/approve` enviando a decisão explícita do cliente (`APPROVED` ou `REFUSED`). Para atingir o status final `PAID`, a fatura deve ser gerada via `POST /invoices`.

<div align="center">
  <img src="docs/delivery/infrastructure/status_chain.png" alt="Máquina de Estados da Ordem de Serviço" width="850">
  <br>
  <em><small><strong>Figura 5: Máquina de Estados do Ciclo de Vida da Ordem de Serviço</strong></small></em>
  <br><br>
</div>

### 🛠️ Máquina de Estados da Execução de Serviço

Cada serviço individual atrelado à OS possui ciclo de progresso próprio:

```mermaid
flowchart LR
    classDef initStyle fill:#E1F5FE,stroke:#0288D1,stroke-width:2px,color:#01579B
    classDef pendStyle fill:#FFF9C4,stroke:#FBC02D,stroke-width:2px,color:#F57F17
    classDef doneStyle fill:#E8F5E9,stroke:#43A047,stroke-width:2px,color:#1B5E20

    INIT["🏁 INITIATED\n(Serviço Iniciado)"]:::initStyle -->|"Aguardando Insumo / Pausa"| PEND["⏸️ PENDING\n(Pendente / Pausado)"]:::pendStyle
    PEND -->|"Retomada e Conclusão"| DONE["✅ FINALIZED\n(Item Finalizado)"]:::doneStyle
    INIT -->|"Execução Direta"| DONE
```

---

## 💻 Como Executar Localmente

### Stack Completa via Docker Compose

O projeto possui um [`docker-compose.yml`](docker-compose.yml) completo que inicializa a aplicação, banco de dados PostgreSQL 16, servidor de e-mail mock (Mailpit) e toda a infraestrutura de observabilidade (OTel Collector, Prometheus, Jaeger, Loki e Grafana):

```bash
# 1. Clone o repositório
git clone https://github.com/fiap-postech-repairshop/tech-challenge-repairshop-app.git
cd tech-challenge-repairshop-app

# 2. (Opcional) Executar testes e build localmente via Maven Wrapper
./mvnw clean test

# 3. Subir todos os serviços com build automático do container
docker compose up --build -d

# 4. Acompanhar a inicialização e logs do backend
docker compose logs -f app

# 5. Para parar e desmontar os serviços
docker compose down
```

> [!TIP]
> **Base Limpa / Migrações do Flyway:** Se você executou versões anteriores localmente e encontrar erro de incompatibilidade de checksum nas migrações do Flyway (`Migration checksum mismatch`), suba com volumes limpos executando:
> ```bash
> docker compose down -v
> docker compose up --build -d
> ```

### 🌐 Portas e Painéis de Acesso

| Serviço | URL | Credenciais / Notas |
| :--- | :--- | :--- |
| **Swagger UI (OpenAPI 3.0)** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | Interface interativa de testes dos endpoints |
| **API REST (Base)** | [http://localhost:8080](http://localhost:8080) | Porta principal da aplicação Spring Boot |
| **Grafana (Observabilidade)** | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` (Auto-login anônimo habilitado) |
| **Jaeger Tracing UI** | [http://localhost:16686](http://localhost:16686) | Rastreamento distribuído de traces OTLP |
| **Prometheus Server** | [http://localhost:9090](http://localhost:9090) | Painel de métricas e consultas PromQL |
| **Mailpit (Web Mailbox)** | [http://localhost:8025](http://localhost:8025) | Interceptador de e-mails disparados pela aplicação |
| **PostgreSQL Database** | `localhost:5432` | `repairshop` / `repairshop` (Database: `repairshop`) |

### 🚀 Fluxo de Primeiro Uso (Swagger UI / Postman)

A maior parte dos endpoints requer autenticação via token JWT com a role `ATTENDANT`. Na **Fase 3**, a autenticação foi padronizada em torno do **CPF** como credencial de identificação primária (integrada com a função Serverless AWS Lambda).

> 💡 **Coleção Postman Pronta:** Utilize os arquivos disponíveis no repositório:
> - [Ambiente Local do Postman](docs/postman/Local.postman_environment.json)
> - [Collection Completa do Postman](docs/postman/Tech_Challenge_Fiap_-_Completo.postman_collection.json)

1. **Cadastrar Usuário Atendente:** No Swagger UI ou Postman, acesse `POST /auth/register`:
   ```json
   {
     "name": "Administrador",
     "function": "ATTENDANT",
     "cpf": "52998224725",
     "email": "admin@shop.com",
     "phone": "+55 11 99999-9999",
     "password": "SecurePass123!"
   }
   ```
2. **Obter Token JWT:** Acesse `POST /auth/login` enviando o CPF e a senha cadastrados:
   ```json
   {
     "cpf": "52998224725",
     "password": "SecurePass123!"
   }
   ```
3. **Autorizar no Swagger:** Copie o token retornado na propriedade `token`, clique no botão **Authorize** (ícone do cadeado no canto superior direito do Swagger) e cole o token. A partir deste momento, todas as requisições incluirão automaticamente o header `Authorization: Bearer <token>`.

---

## 🛡️ Qualidade de Código, Testes e Segurança

### Suíte de Testes Automatizados

A base de código conta com uma ampla suíte de testes unitários e de integração:

```bash
# Executar a suíte completa de testes
./mvnw clean verify
```

- **Testes Unitários de Domínio (MockK & JUnit 5):** Validação de Value Objects (CPF, CNPJ, Placas), invariantes de agregados, cálculo de orçamentos e regras da máquina de estados.
- **Testes de Integração (Testcontainers & Spring Boot Test):** Validação de repositórios JPA, migrações do Flyway e endpoints REST (`MockMvc`) utilizando container real do PostgreSQL.
- **Cobertura de Código (JaCoCo):** Cobertura superior a **80%** nos pacotes críticos de negócio.

### Segurança e DevSecOps

- **Análise Estática (SonarCloud):** Relatórios de qualidade, débito técnico e cobertura integrados ao Quality Gate da pipeline (evidências em [`docs/delivery/sonar/`](docs/delivery/sonar/)).
- **Scan de Vulnerabilidades de Containers (Trivy):** Análise automatizada de vulnerabilidades conhecidas (CVEs) em bibliotecas e camadas do SO na esteira CI/CD.
- **Dynamic Application Security Testing (OWASP ZAP):** Relatório de auditoria DAST para verificação de vulnerabilidades web (disponível em [`docs/delivery/owaspzap/2026-05-01-ZAP-Report-.html`](docs/delivery/owaspzap/2026-05-01-ZAP-Report-.html)).
- **Testes de Carga HPA:** Scripts de estresse com Locust disponíveis em [`docs/delivery/hpa_stress/locustfile.py`](docs/delivery/hpa_stress/locustfile.py).

---

## 📚 Documentação DDD e Artefatos Complementares

- 🏛️ **[Architecture Decision Records (ADRs)](ADRs/README.md):** Catálogo com 13 decisões arquiteturais formais no padrão de Michael Nygard abrangendo domínio, nuvem AWS, segurança e CI/CD.
- 📜 **[Requests for Comments (RFCs)](RFCs/README.md):** Catálogo com 14 propostas técnicas que nortearam as discussões e alternativas arquiteturais antes da implementação na Fase 3.
- 🔄 **[Diagrama de Sequência (Autenticação e Abertura de OS)](docs/delivery/sequence/):** Fluxo temporal ponta a ponta disponível nos formatos [`.png`](docs/delivery/sequence/diagrama_sequencia_auth_abertura_os.drawio.png) *(imagem)*, [`.svg`](docs/delivery/sequence/diagrama_sequencia_auth_abertura_os.drawio.svg) *(vetorial)* e [`.drawio`](docs/delivery/sequence/diagrama_sequencia_auth_abertura_os.drawio) *(editável)*.
- 📄 **[Dicionário de Linguagem Ubíqua](docs/delivery/domain_driven_design/dicionario-linguagem-ubiqua.md):** Glossário oficial dos termos e conceitos do domínio da oficina.
- 🗺️ **[Artefatos de Domain-Driven Design](docs/delivery/domain_driven_design/):** Diagramas exportados do Event Storming, Storytelling e fluxos de negócio.
- 👥 **[Especificações por Papel SDD (Software Design Document)](https://github.com/fiap-postech-repairshop/tech-challenge-wiki-docs/tree/main/sdd):** Diretrizes, responsabilidades e *Definition of Done* por papel técnico (Arquiteto, DevSecOps, Tech Lead, QA e PO), centralizadas no repositório [`tech-challenge-wiki-docs`](https://github.com/fiap-postech-repairshop/tech-challenge-wiki-docs).

---

## 👥 Autores

**Grupo CAO** — Pós-Graduação **POSTECH 15SOAT** (FIAP)

| Autor | RM | GitHub |
| :--- | :--- | :--- |
| **Alexandre** | RM 374016 | [@Alexandre-AGAMIN](https://github.com/Alexandre-AGAMIN) |
| **Otávio Luiz** | RM 370552 | [@otaviolms](https://github.com/otaviolms) |
