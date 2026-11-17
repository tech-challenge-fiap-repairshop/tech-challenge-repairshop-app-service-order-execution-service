# 🏛️ ADR-023: Padronização de Nomenclatura de Repositórios Git — Taxonomia '-app-' e '-infra-'

- **Status**: `Aceito`
- **Data**: 2026-10-25
- **Autores**: Arquiteto de Software e Cloud / Grupo CAO (Tech Challenge SOAT Fase 4)
- **Decisores**: Equipe de Engenharia e Arquitetura de Software

---

## 📌 Contexto

Na evolução da **Fase 4** do Tech Challenge FIAP, a arquitetura monolítica original foi decomposta em um ecossistema poliglota e desacoplado de microsserviços, componentes de persistência distribuída e pipelines de mensageria assíncrona.

Com essa transição, a organização GitHub (`tech-challenge-fiap-repairshop`) passou a albergar dezenas de repositórios com naturezas e ciclos de vida operacionais fundamentalmente distintos:
1. **Infraestrutura como Código (IaC)**: Provisionamento de recursos AWS gerenciados (RDS, DynamoDB, ElastiCache Redis, SQS Queues/DLQs, VPC e EKS).
2. **Aplicações de Negócio (Microsserviços)**: Lógica de domínio, Use Cases em Clean Architecture, endpoints REST, listeners de mensageria, Dockerfiles e manifestos Kubernetes (`k8s/`).
3. **Funções Serverless Autônomas**: Lambdas isoladas para autenticação e autorização via API Gateway.
4. **Governança e Documentação Centralizada**: Central de especificações, decisões arquiteturais (ADRs), docker-compose integrado para desenvolvimento local e memórias técnicas (RAG).

Inicialmente, os repositórios de infraestrutura receberam a taxonomia com o infixo `-infra-` (ex: `tech-challenge-repairshop-infra-sqs`, `tech-challenge-repairshop-infra-redis`, `tech-challenge-repairshop-infra-db-dynamo`). Em contrapartida, os serviços de aplicação mantiveram nomenclaturas diretas como `tech-challenge-repairshop-register-service` ou `tech-challenge-repairshop-service-order-execution-service`.

Essa assimetria gerava:
- Dificuldade de identificação imediata do propósito do repositório em ferramentas de governança, inventários de catálogo e dashboards de CI/CD.
- Ambiguidade em scripts de automação (como `update_aws_secrets.ps1` e pipelines do SonarQube) que precisam aplicar regras de análise estática e thresholds de cobertura distintos para código de aplicação versus código de infraestrutura.
- Ausência de um padrão canônico corporativo para a organização dos repositórios locais e remotos.

---

## 🎯 Decisão

Fica formalmente estabelecida e implementada a **Taxonomia Unificada de Nomenclatura de Repositórios Git** para toda a organização `tech-challenge-fiap-repairshop` e seus respectivos diretórios de desenvolvimento local:

```
[organização]-[produto]-[camada/papel]-[componente]
```

### 1. Camada de Aplicação / Negócio: Infixo `-app-`
Todos os repositórios que contêm código de domínio, lógica de negócio, servidores de aplicação (Spring Boot / Kotlin), empacotamento em containers e manifestos de implantação em runtime adotam obrigatoriamente o prefixo/infixo **`-app-`**:

| Repositório Antigo | Novo Nome Padronizado (`-app-`) | Responsabilidade Central |
| :--- | :--- | :--- |
| `tech-challenge-repairshop-register-service` | **`tech-challenge-repairshop-app-register-service`** | Cadastro de Usuários, Clientes, Veículos e autenticação |
| `tech-challenge-repairshop-service-order-execution-service` | **`tech-challenge-repairshop-app-service-order-execution-service`** | Orquestração da Saga, Abertura/Execução de OS e DynamoDB |
| `tech-challenge-repairshop-payment-service` | **`tech-challenge-repairshop-app-payment-service`** | Integração Mercado Pago, Webhook de Checkout e DynamoDB |
| `tech-challenge-repairshop-notification-service` | **`tech-challenge-repairshop-app-notification-service`** | Disparo de Notificações, Interação com Cliente e Aprovação |

### 2. Camada de Infraestrutura como Código (IaC): Infixo `-infra-`
Repositórios responsáveis pelo provisionamento e governança de recursos em nuvem via Terraform mantêm o padrão **`-infra-`**:

- `tech-challenge-repairshop-infra-network` (VPC, Subnets, Gateways)
- `tech-challenge-repairshop-infra-eks` (Cluster Kubernetes EKS, NodeGroups, Ingress Controller)
- `tech-challenge-repairshop-infra-sqs` (Filas SQS e Dead Letter Queues)
- `tech-challenge-repairshop-infra-redis` (Cluster Redis / ElastiCache para cache de autenticação)
- `tech-challenge-repairshop-infra-db-dynamo` (Tabelas DynamoDB de OS e Pagamentos)
- `tech-challenge-repairshop-infra-services` (Recursos complementares de apoio e banco relacional RDS)

### 3. Camada Serverless: Infixo `-lambda-`
Funções de computação orientada a eventos isoladas mantêm o padrão **`-lambda-`**:
- `tech-challenge-repairshop-lambda-auth` (Lambda Authorizer para validação de JWTs no API Gateway)

### 4. Camada de Governança e Documentação: Sufixo `-docs`
Repositório central de documentação viva, ADRs e orquestração integrada para desenvolvimento:
- `tech-challenge-wiki-docs`

---

## 🛠️ Implementação Técnica e Ações Executadas

1. **Renomeação dos Repositórios Remotos no GitHub**:
   - Utilização da GitHub CLI (`gh repo rename`) para atualizar o nome canônico de cada repositório sob a organização `tech-challenge-fiap-repairshop`.
   - Preservação integral do histórico de commits, tags, pull requests e regras de branch protection do Git Flow (`develop` ➔ `homolog` ➔ `main`).
2. **Migração dos Diretórios Locais de Trabalho**:
   - Renomeação dos diretórios locais no workspace para espelhar exatamente os nomes remotos.
   - Atualização das URLs de upstream e origin em cada repositório local (`git remote set-url origin`).
3. **Atualização das Configurações de Orquestração Local**:
   - Ajuste nos caminhos de contexto de build do Docker Compose unificado (`tech-challenge-wiki-docs/docker-compose.yml`):
     ```yaml
     services:
       register-service:
         build:
           context: ../tech-challenge-repairshop-app-register-service
       service-order-execution-service:
         build:
           context: ../tech-challenge-repairshop-app-service-order-execution-service
       payment-service:
         build:
           context: ../tech-challenge-repairshop-app-payment-service
       notification-service:
         build:
           context: ../tech-challenge-repairshop-app-notification-service
     ```
4. **Atualização de Scripts e Automações**:
   - Ajuste do script de governança de segredos da AWS (`deploy/update_aws_secrets.ps1`) com os novos nomes de repositórios.
5. **Atualização de Documentação e Checklists**:
   - Sincronização dos checklists da Fase 4 e dos READMEs internos de cada projeto.

---

## 📈 Consequências

### Benefícios Positivos
- **Segregação Visual e Cognitiva Clara**: Qualquer engenheiro ou avaliador distingue instantaneamente se um repositório trata de lógica aplicacional (`-app-`) ou infraestrutura em nuvem (`-infra-`).
- **Automação Facilitada de CI/CD e Scanners de Segurança**: Simplifica a criação de scripts globais e GitHub Workflows reutilizáveis que filtram por repositórios contendo `-app-*` para compilação Maven/Gradle, testes e Quality Gates no SonarQube, ou `-infra-*` para `terraform plan/apply` e linters tflint/checkov.
- **Conformidade com o AWS Well-Architected Framework**: Fortalece o pilar de *Excelência Operacional* ao estabelecer padrões previsíveis de governança de código e infraestrutura.

### Mitigação de Impactos e Compatibilidade
- O GitHub mantém automaticamente o redirecionamento permanente de tráfego HTTP e SSH a partir dos URLs anteriores para os novos nomes, evitando quebra imediata de clones remotos enquanto a sincronização ocorre.
- As branches e regras de proteção configuradas continuam inalteradas.

---

## 📚 Referências

- [AWS Well-Architected Framework — Operational Excellence Pillar](https://docs.aws.amazon.com/wellarchitected/latest/operational-excellence-pillar/)
- [Martin Fowler — Microservice Prerequisites & Repository Organization](https://martinfowler.com/articles/microservices.html)
- [Diretrizes de Governança de Branches e Git Flow da Organização](file:///c:/Users/Alexandre-AGAMIN/Projetos-%20FIAP/github-organizations-projects/tech-challenge-wiki-docs/checklist_atividades_FASE_4.md)
