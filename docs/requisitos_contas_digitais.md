# Requisitos do Sistema de Contas Digitais

## 1. Objetivo do documento

Este documento estabelece os requisitos para o desenvolvimento e a manutenção do sistema de contas digitais. Ele orienta a implementação da API backend responsável por movimentações financeiras, saldos e históricos de operações.

Os seguintes riscos devem ser mitigados pela implementação:

- Uma transferência debitar uma conta sem creditar a conta de destino.
- Uma requisição repetida executar a mesma transferência duas vezes.
- Duas transferências simultâneas consumirem o mesmo saldo disponível.
- O histórico financeiro ficar inconsistente com os saldos das contas.

A implementação deve preservar a integridade financeira mesmo diante de falhas e requisições concorrentes.

## 2. Requisitos funcionais

### RF01 - Criar conta

- O sistema deve permitir a abertura de uma conta digital.
- Cada conta deve possuir um identificador único.
- A conta deve iniciar com saldo zero.
- A conta deve possuir status, como `ACTIVE` ou `BLOCKED`.
- Uma conta bloqueada não pode realizar movimentações.

### RF02 - Consultar conta e saldo

- O sistema deve permitir consultar os dados de uma conta e seu saldo atual.

### RF03 - Realizar depósito

- O sistema deve permitir adicionar fundos a uma conta ativa.
- O valor deve ser positivo.
- A operação deve gerar um registro financeiro.
- A alteração do saldo e o registro do lançamento devem ser atômicos.

### RF04 - Realizar saque

- O sistema deve permitir retirar fundos de uma conta ativa.
- O valor deve ser positivo.
- Não pode haver saldo negativo.
- A operação deve gerar um lançamento de débito.

### RF05 - Realizar transferência

- O sistema deve permitir transferir dinheiro entre duas contas.
- A conta de origem e a conta de destino devem existir e estar ativas.
- As contas devem ser diferentes.
- O valor deve ser positivo.
- A conta de origem deve possuir saldo suficiente.
- O débito e o crédito devem ser concluídos juntos ou revertidos juntos.
- Cada transferência deve possuir um identificador único.

### RF06 - Consultar extrato

- O sistema deve permitir consultar os lançamentos financeiros de uma conta, incluindo tipo, valor, data, identificação da operação e natureza do lançamento.

### RF07 - Idempotência

- O sistema deve aceitar uma chave de idempotência nas operações financeiras que alteram o saldo.
- Se o cliente enviar novamente a mesma requisição com a mesma chave, a operação não poderá ser executada novamente.
- Se a mesma chave for reutilizada com parâmetros diferentes, a API deverá rejeitar a requisição.

## 3. Requisitos não funcionais

| ID | Requisito |
| --- | --- |
| RNF01 | Utilizar Java 21 e Spring Boot. |
| RNF02 | Utilizar PostgreSQL como banco de dados. |
| RNF03 | Utilizar `BigDecimal` para valores monetários. |
| RNF04 | Garantir atomicidade das movimentações financeiras. |
| RNF05 | Tratar concorrência e impedir gastos acima do saldo disponível. |
| RNF06 | Implementar testes unitários e de integração. |
| RNF07 | Disponibilizar ambiente executável via Docker Compose. |
| RNF08 | Documentar a API com OpenAPI/Swagger. |
| RNF09 | Registrar operações de forma rastreável, sem permitir a alteração arbitrária do histórico financeiro. |

## 4. Regras de negócio obrigatórias

As regras a seguir são invariantes do sistema e devem ser respeitadas independentemente da arquitetura escolhida.

- Uma conta não pode possuir saldo negativo.
- Um depósito aumenta o saldo exatamente pelo valor depositado.
- Um saque reduz o saldo exatamente pelo valor sacado.
- Uma transferência de valor `V` deve reduzir o saldo da origem em `V` e aumentar o saldo do destino em `V`.
- Uma transferência não pode ser executada parcialmente.
- Uma operação aceita não pode ser aplicada mais de uma vez devido à repetição da requisição.
- O histórico financeiro não pode ser apagado ou editado para corrigir uma movimentação; as correções devem ser registradas como novas operações compensatórias.
- O sistema deve manter a consistência mesmo quando duas ou mais operações são executadas simultaneamente.

> **Escopo:** todas as contas utilizam a moeda BRL. Não devem ser implementados juros, tarifas, câmbio, Pix real ou integração com bancos externos.

## 5. Contrato inicial da API

Os endpoints abaixo constituem o contrato inicial para a implementação.

| Método | Endpoint | Finalidade |
| --- | --- | --- |
| `POST` | `/api/v1/accounts` | Criar conta |
| `GET` | `/api/v1/accounts/{id}` | Consultar conta e saldo |
| `POST` | `/api/v1/accounts/{id}/deposits` | Depositar |
| `POST` | `/api/v1/accounts/{id}/withdrawals` | Sacar |
| `POST` | `/api/v1/transfers` | Transferir |
| `GET` | `/api/v1/accounts/{id}/ledger` | Consultar extrato |

### Exemplo de requisição para transferência

```http
POST /api/v1/transfers
Content-Type: application/json
Idempotency-Key: 8f75d80a-2d71-4bb5-a8f0-13f9c21e4c00

{
  "sourceAccountId": "uuid-da-conta-origem",
  "destinationAccountId": "uuid-da-conta-destino",
  "amount": 150.00
}
```

### Exemplo de resposta de sucesso

```json
{
  "transferId": "uuid-da-transferencia",
  "status": "COMPLETED",
  "amount": 150.00,
  "sourceAccountId": "uuid-da-conta-origem",
  "destinationAccountId": "uuid-da-conta-destino"
}
```

Os identificadores do exemplo são ilustrativos.

## 6. Validação de concorrência financeira

O comportamento concorrente deve ser validado considerando o seguinte cenário:

- A conta A possui R$ 100,00.
- A conta B possui R$ 0,00.
- A conta C possui R$ 0,00.
- Duas requisições tentam transferir R$ 80,00 da conta A simultaneamente: uma para B e outra para C.

### Estado inicial

| Conta | Saldo |
| --- | --- |
| Conta A | R$ 100,00 |
| Conta B | R$ 0,00 |
| Conta C | R$ 0,00 |

São realizadas duas transferências simultâneas de R$ 80,00.

**Resultado permitido:** uma transferência é concluída e a outra é rejeitada por saldo insuficiente. A ordem pode variar.

**Resultado proibido:** as duas transferências são concluídas, fazendo a conta A gastar R$ 160,00 apesar de possuir apenas R$ 100,00.

Esse cenário deve ser demonstrado por meio de um teste de integração com requisições concorrentes contra PostgreSQL real, e não apenas por testes unitários com mocks.
