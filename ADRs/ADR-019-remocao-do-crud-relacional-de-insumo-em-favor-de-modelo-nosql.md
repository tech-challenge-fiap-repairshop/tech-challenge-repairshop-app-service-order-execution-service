# ADR-019: Remoção do CRUD Relacional de Insumos em Favor de Modelo NoSQL Single-Table

**Data:** 2026-10-25  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

Nas fases anteriores (monólito relacional), a aplicação continha uma tabela SQL dedicada (`tb_insume`) e um conjunto completo de operações CRUD de estoque (criação, edição, exclusão e dedução de saldo de insumos como óleos, filtros e pastilhas de freio) expostos na API do `register-service`.

Com a evolução da arquitetura na Fase 4 para uma esteira distribuída sob **Single-Table Design no Amazon DynamoDB** (`tb-service_order`), os insumos utilizados em um reparo técnico passaram a ser embutidos diretamente dentro da lista de tarefas técnicas do documento NoSQL da OS (`item_execution.insumes`).

Essa dualidade gerava:
1. **Redundância e Inconsistência de Dados**: O mesmo insumo existia como registro relacional no PostgreSQL e como documento desnormalizado dentro do DynamoDB.
2. **Complexidade Transacional Desnecessária**: Qualquer lançamento de peça na OS exigia sincronização bidirecional entre o `service-order-execution-service` e o `register-service`.
3. **Escopo Impróprio no `register-service`**: O `register-service` foi desenhado com o propósito estrito de gerenciar a identidade de usuários, mecânicos, clientes e veículos, não devendo acumular responsabilidades de almoxarifado ou estoque de autopeças.

---

## Decisão

**Eliminar integralmente o CRUD relacional de insumos do `register-service`** e consolidar a especificação de insumos e peças como documentos embutidos no **DynamoDB Single-Table Design** do `service-order-execution-service`:

```
[ANTES - FASE 3]
PostgreSQL (RDS) ── tb_insume (Tabela Relacional)
       │
       ▼
CRUD no Monólito / register-service
       ▲
       │
Sincronização redundante com a OS


[DEPOIS - FASE 4 (ADR-019)]
PostgreSQL (RDS): Limpo e Focado (Apenas tb_user, tb_customer, tb_vehicle)
       │
       ▼
DynamoDB (tb-service_order - Single-Table Design):
  PK: order#<uuid>
  SK: EXEC#<timestamp>#<task_id>
  Atributos:
    - task_name: "Troca de Pastilhas de Freio"
    - insumes: [
        {
          "name": "Pastilha de Freio Dianteira",
          "brand": "Cobreq",
          "sku_id": "BRK-PAD-01",
          "quantity": 2,
          "unity_price": 120.00
        }
      ]
```

### Ações Executadas:
1. **Limpeza de Schema SQL**: Removida a tabela `tb_insume` e seus inserts de seed da migration Flyway `V1__init_register_schema.sql` do `register-service`.
2. **Exclusão de Código Obsoleto**: Deletado o pacote `inventory` completo (Use Cases, Gateways, DTOs, Controllers REST, Repositórios e Entidades JPA) em [tech-challenge-repairshop-app-register-service](file:///c:/Users/Alexandre-AGAMIN/Projetos-%20FIAP/github-organizations-projects/tech-challenge-repairshop-app-register-service).
3. **Ajuste de Segurança**: Removidas as regras de autorização de rotas `/insumes/**` do `SecurityConfig.kt`.
4. **Governança NoSQL (PF-07)**: Como a quantidade média de insumos por ordem de serviço em uma oficina mecânica não ultrapassa poucas dezenas de itens, o tamanho total do item `item_execution` permanece bem abaixo do teto de 400 KB do DynamoDB, garantindo máxima performance de leitura atômica sem impacto de custos.

---

## Consequências

### Positivas
- **Simplificação Arquitetural Significativa**: Eliminação de um domínio inteiro que exigiria sincronização distribuída, conciliação de estoque e transações de compensação na Saga.
- **Redução de Custo e Overhead no RDS**: O PostgreSQL do `register-service` opera mais leve, com menor pegada de memória e menor consumo de conexões.
- **Performance de Leitura na OS**: Ao carregar a Ordem de Serviço pelo DynamoDB, todas as tarefas e seus respectivos insumos são obtidos em uma única operação `GetItem` ou `Query` no mesmo partition key, sem necessidade de joins ou chamadas HTTP adicionais.

### Desafios e Mitigações
- **Controle Central de Almoxarifado/Estoque Global**: A contabilidade global de reposição de estoque deixa de ser centralizada pelo sistema da oficina neste momento da Fase 4. Para o escopo do Tech Challenge, o requisito de negócio exige focar na execução e faturamento da OS; sistemas legados de ERP de autopeças podem ser integrados no futuro via barramento assíncrono caso necessário.

---

## Referências
- Alex DeBrie, *The DynamoDB Book: Denormalization and Complex Attributes*.
- Eric Evans, *Domain-Driven Design: Tackling Complexity in the Heart of Software* (Bounded Context Boundaries).
