# Testes da API pelo Postman

## Importação e configuração

Importe `Repair Shop (Tech Challenge FIAP).postman_collection.json` e o ambiente
`Local.postman_environment.json`. Selecione **Local** e preencha as quatro URLs
base antes de executar a coleção. Use URLs acessíveis a partir da máquina onde
o Postman está rodando, sem barra final:

| Variável | Aplicação / rotas |
| --- | --- |
| `service_order_url` | Ordem de serviço, execução, estoque e faturas (`/service-orders`, `/insumes`, `/invoices`) |
| `register_url` | Identidade/autenticação, clientes e veículos (`/auth`, `/customers`, `/vehicles`) |
| `payment_url` | Pagamentos (`/payments`) |
| `notification_url` | Notificações e decisão do cliente (`/notifications`) |

O ambiente não contém credenciais nem endereços de infraestrutura. A coleção
valida que as URLs estejam preenchidas e usem `http://` ou `https://`; os campos
podem incluir um prefixo de caminho da API, mas não devem terminar em `/`.

## Fluxo principal

Execute os requests dentro de **01 - Fluxo principal (ordem cronológica)** em
ordem. A sequência cria atendente e cliente, autentica, cadastra cliente e
veículo, cria um insumo, abre uma OS, adiciona uma execução e solicita aprovação.
Os scripts validam os códigos HTTP e campos importantes e guardam token e IDs
para os requests seguintes. CPF, e-mail, telefone e placa são gerados para cada
execução; os recursos criados permanecem nos serviços.

Depois de **Solicitar aprovação do orçamento**, aguarde o processamento do
callback assíncrono da fila SQS antes de executar **Continuar após callback
SQS**. A resposta imediata da API de notificações confirma que a decisão foi
enviada, não que a OS já mudou de estado. Se a consulta ainda não retornar
`APPROVED`, aguarde o consumidor e execute novamente a consulta antes de
continuar. A sequência então avança a OS e a execução até `FINALIZED` e consulta
a fatura.

O consumidor de callback deste repositório grava `IN_PROGRESS`, enquanto a
transição da API de OS espera `APPROVED`. Se a consulta retornar `IN_PROGRESS`
ou permanecer em `WAITING_APPROVAL`, a asserção da coleção evidencia esse
desalinhamento; não avance a OS manualmente para mascará-lo.

O envio da OS para pagamento e a confirmação de pagamento também usam SQS. A
criação de pagamento pode ser feita pelo consumidor da fila; por isso, os
requests diretos de criação, consulta, confirmação e webhook estão em
**CRUDs manuais > payments** e não são executados automaticamente no fluxo
principal. Evite criar um segundo pagamento para uma OS que já tenha sido
processada pela saga. `POST /payments/webhook` é o endpoint implementado no
serviço para teste; não substitui a validação de assinatura de um webhook real
do Mercado Pago.

## CRUDs manuais

A pasta **CRUDs manuais** fica separada do fluxo e reúne operações para cadastro,
veículos, estoque, OS, execuções, faturas, pagamentos e notificações. Execute
apenas as operações necessárias; os requests usam as variáveis capturadas pelo
fluxo e podem exigir que você edite IDs ou corpos antes do envio. As operações
diretas de aprovação da OS e de aprovação pelo serviço de notificações são
alternativas: não as use em duplicidade para a mesma OS.

`Tech_Challenge_Fiap_-_Completo.postman_collection.json` é mantida como cópia
compatível da coleção principal. Importe apenas uma das duas para evitar
duplicatas no Postman.
