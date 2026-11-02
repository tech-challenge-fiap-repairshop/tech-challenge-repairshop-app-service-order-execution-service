# language: pt
Funcionalidade: Gestão do Ciclo de Vida da Ordem de Serviço (OS) e Saga Pattern
  Como atendente ou mecânico da oficina mecânica
  Eu quero gerenciar o status da Ordem de Serviço, seus serviços e insumos
  Para garantir que o fluxo operacional e as compensações ocorram com integridade

  Contexto:
    Dado que uma nova Ordem de Serviço é criada com status inicial "IN_DIAGNOSIS"

  Cenário: Fluxo Nominal - Adicionar serviços, enviar para aprovação e aprovar orçamento
    Quando o atendente adiciona o serviço "Troca de Óleo e Filtro" com valor de 150.00
    E adiciona o insumo "Óleo Sintético 5W30" com valor unitário de 40.00 e quantidade 4
    Então o valor total da Ordem de Serviço deve ser 310.00
    Quando a Ordem de Serviço avança para o status "WAITING_APPROVAL"
    E o cliente aprova o orçamento
    Então o status da Ordem de Serviço deve ser "APPROVED"
    E o histórico deve conter o registro de transição para "APPROVED"

  Cenário: Fluxo de Compensação (Rollback) - Cliente recusa o orçamento
    Quando o atendente adiciona o serviço "Revisão Geral de Suspensão" com valor de 500.00
    E a Ordem de Serviço avança para o status "WAITING_APPROVAL"
    Quando o cliente recusa o orçamento
    Então o status da Ordem de Serviço deve ser "REFUSED"
    E a Ordem de Serviço não deve permitir avançar para "IN_EXECUTION"

  Cenário: Bloqueio de Integridade - Não permitir avançar para aguardando aprovação sem serviços
    Dado que a Ordem de Serviço não possui nenhum serviço cadastrado
    Quando o atendente tenta avançar a Ordem de Serviço para "WAITING_APPROVAL"
    Então uma exceção de transição inválida deve ser lançada informando que não há serviços

  Cenário: Conclusão da Execução Mecânica e Finalização da OS
    Quando o atendente adiciona o serviço "Alinhamento e Balanceamento" com valor de 120.00
    E a Ordem de Serviço avança para o status "WAITING_APPROVAL"
    E o cliente aprova o orçamento
    E o pagamento é confirmado e a Ordem de Serviço avança para "IN_EXECUTION"
    E o mecânico finaliza todas as tarefas do serviço
    Quando a Ordem de Serviço é verificada para conclusão
    Então o status da Ordem de Serviço deve ser "FINALIZED"
    E a data de finalização deve ser registrada

