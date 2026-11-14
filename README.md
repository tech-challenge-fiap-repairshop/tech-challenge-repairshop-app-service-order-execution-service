# Serviço de Execução de Ordens de Serviço

Backend do projeto **RepairShop**, desenvolvido em Kotlin com Spring Boot e Java 24. Este repositório reúne a API e seus manifestos de implantação para o domínio de ordens de serviço e execução de serviços.

> **POSTECH 15SOAT — Tech Challenge — Fase 4 — Grupo CAO**
>
> Registros e entregas das Fases 1, 2 e 3 podem aparecer nos documentos históricos vinculados abaixo. Eles descrevem o contexto e decisões anteriores, não necessariamente o estado atual deste serviço.

## O que o serviço oferece

- Cadastro e consulta de clientes e veículos, ordens de serviço, execuções, insumos e faturas.
- Fluxo de estados de ordens de serviço e métricas operacionais/de negócio expostas pela aplicação.
- Autenticação própria na API: `POST /auth/register` e `POST /auth/login` emitem JWT; o Spring Security valida os tokens e aplica autorização por perfil. As senhas são armazenadas com BCrypt.
- Persistência relacional com PostgreSQL, Spring Data JPA e migrações Flyway. O código também contém integração com DynamoDB e SQS para operações e eventos assíncronos.
- Aplicação organizada por contextos de negócio (`serviceorder`, `execution`, `inventory`, `register`, `payment` e `user`), com camadas `domain`, `application` e `infra`.

O módulo de faturas não implementa emissão fiscal. A baixa de estoque deve ser entendida conforme a implementação atual; não se presume uso de `SELECT FOR UPDATE`.

## API

A especificação OpenAPI e a interface Swagger ficam disponíveis, quando a aplicação estiver em execução, em:

`http://localhost:8080/swagger-ui/index.html`

Rotas principais incluem `/auth`, `/customers`, `/vehicles`, `/service-orders`, `/insumes` e `/invoices`. As rotas de ordens de serviço exigem autenticação: consultas `GET` aceitam perfis `ATTENDANT` e `CUSTOMER`; operações de escrita são autorizadas conforme as regras do Spring Security. Consulte o Swagger para parâmetros, payloads e respostas.

## Executar localmente

O caminho abaixo inicia a API com PostgreSQL e o emulador AWS LocalStack. Requer **Java 24**, **Maven**, **Docker Desktop** com Docker Compose e, no Windows, PowerShell. O repositório não inclui Maven Wrapper.

1. Inicie o PostgreSQL 16:

   ```powershell
   docker run --name repairshop-postgres `
     -e POSTGRES_DB=repairshop `
     -e POSTGRES_USER=repairshop `
     -e POSTGRES_PASSWORD=repairshop `
     -p 5432:5432 -d postgres:16
   ```

2. Inicie o LocalStack definido no Compose:

   ```powershell
   docker compose up -d localstack
   ```

3. Na raiz do repositório, configure as conexões locais e inicie a aplicação:

   ```powershell
   $env:DB_URL = "jdbc:postgresql://localhost:5432/repairshop"
   $env:DB_USERNAME = "repairshop"
   $env:DB_PASSWORD = "repairshop"
   $env:AWS_ENDPOINT_URL = "http://localhost:4566"
   $env:AWS_ACCESS_KEY_ID = "test"
   $env:AWS_SECRET_ACCESS_KEY = "test"
   $env:AWS_REGION = "us-east-1"
   $env:SQS_HEALTH_CHECK_ENABLED = "false"
   mvn spring-boot:run
   ```

   O Flyway aplica as migrações do PostgreSQL na inicialização. A configuração acima desativa somente o health check do SQS; o listener continua tentando consumir mensagens. Depois, acesse o Swagger no endereço acima.

**Limites do ambiente local:** `docker-compose.yml` não representa a stack completa: define LocalStack e a aplicação, mas não sobe PostgreSQL, Mailpit nem observabilidade, e não inicializa as filas ou a tabela do DynamoDB. Para exercitar os fluxos assíncronos, crie no LocalStack as filas `approve-notification-queue-dev`, `callback-approve-notification-queue-dev`, `create-payment-order-queue-dev` e `callback-payment-order-queue-dev`; a tabela configurada por padrão é `tb-service_order-dev` e o código consulta o índice `pending-dispatch-index`. O serviço de e-mail também precisa de um servidor SMTP, como Mailpit, quando as operações acionarem notificações. Sem esses recursos, o Compose sozinho não fornece um ambiente funcional completo.

Para encerrar os containers iniciados pelo Compose, execute `docker compose down`; para parar/remover o PostgreSQL iniciado acima, use `docker stop repairshop-postgres` e `docker rm repairshop-postgres`.

## Persistência, segurança e operação

- O PostgreSQL é a persistência relacional principal; o schema é versionado em `src/main/resources/db/migration/`.
- A aplicação possui configuração stateless com JWT e Spring Security. O segredo padrão de desenvolvimento não é apropriado para produção: configure um `JWT_SECRET` forte e exclusivo por ambiente.
- A aplicação expõe Actuator Health e métricas Prometheus. Há manifestos Kubernetes de aplicação, HPA e componentes de observabilidade em `k8s/`; a presença dos manifestos não implica que todos os recursos estejam implantados ou disponíveis neste repositório.
- `local/` contém configurações para execução local em Kubernetes, mas não é o caminho de inicialização simplificado descrito acima. O Compose atual também não configura uma stack integral de observabilidade.

## Build, testes e automação

O projeto usa Maven. Para compilar e executar a suíte de testes:

```powershell
mvn clean verify
```

O `pom.xml` configura JaCoCo com limite mínimo de 80% de cobertura de linhas; isso é um critério configurado, não uma afirmação de cobertura medida atual. Os testes usam JUnit, MockK e Testcontainers.

O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) define etapas de compilação, testes, análise SonarCloud, build de imagem, verificação Trivy e publicação/implantação AWS, sujeitas às condições do próprio workflow e à configuração de secrets. A análise SonarCloud é ignorada quando `SONAR_TOKEN` não está configurado. Consulte o arquivo para os gatilhos, branches e condições efetivamente aplicados.

## Documentação do projeto

- [ADRs — decisões arquiteturais](ADRs/README.md): catálogo das decisões registradas, incluindo decisões de fases anteriores.
- [RFCs — propostas técnicas](RFCs/README.md): catálogo das propostas e discussões técnicas.
- [Dicionário de linguagem ubíqua](docs/delivery/domain_driven_design/dicionario-linguagem-ubiqua.md).
- [Diagramas de sequência](docs/delivery/sequence/): autenticação e abertura de ordem de serviço.
- [Diagramas de infraestrutura e dados](docs/delivery/infrastructure/).
- [Coleções e ambiente Postman](docs/postman/).
