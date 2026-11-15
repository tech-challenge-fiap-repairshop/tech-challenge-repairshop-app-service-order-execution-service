# ADR-021: Orquestração Multi-Serviço no Mesmo Cluster EKS, Namespaces Compartilhados e Autoscaling HPA

**Data:** 2026-10-25  
**Status:** Aceito  
**Autor:** Grupo CAO (POSTECH 15SOAT)  

---

## Contexto

A decomposição da oficina mecânica resultou em 4 microsserviços autônomos (`register-service`, `service-order-execution-service`, `payment-service`, `notification-service`). Para provisionar a infraestrutura de computação em nuvem, a equipe de arquitetura precisava definir o modelo de orquestração no Amazon EKS:
1. **Clusters Isolados vs Cluster Único Compartilhado**: Criar um cluster EKS separado para cada microsserviço acarretaria um custo proibitivo (taxa de controle do EKS por cluster + instâncias EC2 subutilizadas) e aumentaria exponencialmente a complexidade operacional.
2. **Topologia de Rede e Descoberta de Serviços**: Os microsserviços precisam comunicar-se de forma eficiente (ex: `register-service` chamando `service-order-execution-service`) sem necessidade de expor portas para a internet pública.
3. **Escala Elástica e Gestão por Ambiente**: Cada microsserviço tem padrões de carga distintos e precisa suportar deploys independentes nos ambientes de desenvolvimento (`dev`), homologação (`hml`) e produção (`prd`).

---

## Decisão

Adotar a estratégia de **Cluster EKS Compartilhado por Ambiente (`repairshop-eks-{env}`)**, com todos os microsserviços convivendo no mesmo **Namespace (`repairshop`)**, comunicação interna privada via CoreDNS e autoscaling elástico via **Horizontal Pod Autoscaler (HPA v2)**:

```
┌────────────────────────────────────────────────────────────────────────┐
│                      Cluster AWS EKS: repairshop-eks-{env}             │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                     Namespace: repairshop                        │  │
│  │                                                                  │  │
│  │  ┌─────────────────────────┐        ┌─────────────────────────┐  │  │
│  │  │ register-service        │        │ service-order-execution │  │  │
│  │  │ - Port: 8081            │───────▶│ - Port: 8080            │  │  │
│  │  │ - HPA (1 a 3 pods)      │ (DNS)  │ - HPA (1 a 3 pods)      │  │  │
│  │  └─────────────────────────┘        └─────────────────────────┘  │  │
│  │                                                                  │  │
│  │  ┌─────────────────────────┐        ┌─────────────────────────┐  │  │
│  │  │ payment-service         │        │ notification-service    │  │  │
│  │  │ - Port: 8082            │        │ - Port: 8083            │  │  │
│  │  │ - HPA (1 a 3 pods)      │        │ - HPA (1 a 3 pods)      │  │  │
│  │  └─────────────────────────┘        └─────────────────────────┘  │  │
│  │                                                                  │  │
│  │  ┌────────────────────────────────────────────────────────────┐  │  │
│  │  │ OpenTelemetry Collector (DaemonSet / Service)              │  │  │
│  │  │ http://otel-collector.repairshop.svc.cluster.local:4317    │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### 1. Descoberta de Serviços Interna (Kubernetes Service Discovery)
- Os serviços se comunicam internamente utilizando nomes DNS nativos do Kubernetes:
  - `http://service-order-execution-service:8080`
  - `http://register-service:8081`
  - `http://payment-service:8082`
  - `http://notification-service:8083`
- Elimina latência de saltos externos de internet e custos de NAT Gateway para tráfego entre serviços.

### 2. Dimensionamento e Política de Autoscaling (HPA v2)
Todos os serviços implementam HPA padronizado com os seguintes parâmetros:
- **Métrica-Alvo:** 70% de utilização de CPU média dos pods.
- **Limites de Réplicas:** Mínimo de 1 pod (para contenção de custos em repouso) e Máximo de 3 pods (absorção de picos de atendimento da oficina).
- **Políticas de Estabilização:**
  - Scale-Up suave: Janela de estabilização de 20s (permite escalar rapidamente em caso de onda de requisições).
  - Scale-Down conservador: Janela de estabilização de 60s (evita oscilação de *flapping* ao término de um lote de mensagens).

### 3. ConfigMaps Segregados por Ambiente
- Cada repositório armazena seus ConfigMaps segregados em `k8s/configmap/`:
  - `configmap-dev.yaml`
  - `configmap-hml.yaml`
  - `configmap-prd.yaml`
- O pipeline de CI/CD detecta o branch de destino (`develop` ➔ `dev`, `homolog` ➔ `hml`, `main` ➔ `prd`) e aplica dinamicamente apenas o arquivo correspondente (`kubectl apply -f k8s/configmap/configmap-${ENV}.yaml`).

---

## Consequências

### Positivas
- **Eficiência de Recursos e Otimização de Custos (FinOps)**: Um único cluster EKS por ambiente compartilha nodes EC2 entre os 4 serviços, minimizando custos fixos.
- **Isolamento Lógico e Governança**: O namespace `repairshop` agrupa os recursos do ecossistema, facilitando aplicação de NetworkPolicies e quotas de recursos.
- **Deploys Independentes**: Cada serviço possui seu próprio ciclo de deploy via GitHub Actions sem bloquear os demais serviços do cluster.

### Desafios e Mitigações
- **Possível Conflito de Nomes em Recursos Compartilhados**: Mitigado através de convenção rígida de prefixação em todos os manifestos (`metadata.name: <service-name>`, `<service-name>-config`, `<service-name>-secret`, `<service-name>-hpa`).

---

## Referências
- Kubernetes Documentation, *Horizontal Pod Autoscaler (HPA)*.
- AWS Well-Architected Framework, *Reliability & Cost Optimization Pillars*.
