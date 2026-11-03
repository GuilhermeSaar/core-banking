# Core Banking

API REST de contas digitais em **Java 21, Spring Boot e PostgreSQL**, com criação de contas, consulta de saldo, depósitos, saques, transferências e extratos.

O cenário central do projeto é uma disputa pelo mesmo saldo: uma conta com **R$ 100,00** recebe duas requisições simultâneas de transferência de **R$ 80,00**. A implementação usa transações e bloqueio pessimista, e o teste de integração verifica que apenas uma operação é concluída, deixando **R$ 20,00** na origem.

O projeto segue o [desafio de contas digitais](docs/desafio_backend_contas_digitais.pdf), com movimentações na mesma moeda, BRL.

[Executar localmente](#executar-localmente) · [Explorar a API](#explorar-a-api) · [Testes](#testes) · [Organização](#organização) · [Escopo atual](#escopo-atual)

## Destaques técnicos

| Aspecto | Implementação |
| --- | --- |
| Atomicidade | Serviços com `@Transactional` agrupam a alteração dos saldos, a operação, os lançamentos e o registro de idempotência na mesma transação. |
| Concorrência | [`PESSIMISTIC_WRITE`](src/main/java/com/corebaking/account/repository/AccountRepository.java) nas contas movimentadas para coordenar operações que disputam o mesmo saldo. |
| Valores monetários | `BigDecimal`, colunas `NUMERIC(18, 2)` e regras de valor positivo e até duas casas decimais no [`MonetaryAmount`](src/main/java/com/corebaking/shared/vo/MonetaryAmount.java). |
| Idempotência | Chave UUID persistida, restrição de unicidade no banco e comparação de hash SHA-256 dos parâmetros da requisição. |
| Histórico financeiro | Transferências geram débito na origem e crédito no destino. [Triggers no PostgreSQL](src/main/resources/db/migration/V2__protect_financial_history.sql) rejeitam `UPDATE` e `DELETE` em operações e lançamentos. |
| Contrato HTTP | Documentação OpenAPI/Swagger e [tratamento global de exceções](src/main/java/com/corebaking/shared/handler/GlobalExceptionHandler.java) com respostas estruturadas. |

## Executar localmente

**Pré-requisitos:** Docker em execução, Docker Compose e portas `8080` e `5432` disponíveis. O build da aplicação é feito dentro do container com Java 21 e o Maven Wrapper do projeto.

```bash
git clone https://github.com/GuilhermeSaar/core-banking.git
cd core-banking
docker compose up --build -d
```

O [Docker Compose](compose.yaml) inicia **a API e o PostgreSQL 16 juntos**. A API aguarda o banco ficar pronto e se conecta pelo endereço interno `postgres:5432`. O [Dockerfile](Dockerfile) compila o projeto em uma etapa com JDK 21 e executa o JAR em outra com JRE 21, usando um usuário sem privilégios de root.

O Flyway aplica as migrações e o Hibernate valida o schema na inicialização. Para acompanhar a aplicação:

```bash
docker compose logs -f api
```

| Recurso | Endereço |
| --- | --- |
| API | `http://localhost:8080/api/v1` |
| Swagger UI | [localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |
| Especificação OpenAPI | [localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

### Contas prontas para demonstração

A [migração V3](src/main/resources/db/migration/V3__demo_accounts.sql) cria duas contas ativas com IDs fixos, já utilizados na coleção do Insomnia:

| Conta | ID | Saldo inicial |
| --- | --- | --- |
| Conta Demo Origem | `a7edac6c-8295-4b4a-86eb-104c5729b092` | R$ 1.000,00 |
| Conta Demo Destino | `32a03fb7-c06e-45e2-86b0-417919e69f67` | R$ 1.000,00 |

A migração é aplicada uma única vez e não redefine os saldos ao reiniciar a aplicação. Contas criadas pela API começam com saldo zero e status `ACTIVE`.

Para experimentar sem montar as requisições, importe a [coleção do Insomnia](insomnia/core-baking.json). Ela contém URLs, IDs e headers preenchidos, além de casos com valores inválidos, conta inexistente e saldo insuficiente. As requisições de criação de conta são exemplos adicionais de cadastro; as contas de demonstração já são inseridas pelo Flyway.

Para parar a API e o banco, execute `docker compose stop`.

## Explorar a API

Todas as rotas abaixo usam o prefixo `/api/v1`. O `{id}` identifica uma **conta**.

| Método | Rota | Entrada / retorno |
| --- | --- | --- |
| `POST` | `/accounts` | Recebe `name`; retorna os dados da conta e o UUID gerado. |
| `GET` | `/accounts/{id}` | Retorna `name`, `status` e `balance`. |
| `POST` | `/transaction/{id}/deposits` | Recebe `amount` e credita a conta. |
| `POST` | `/transaction/{id}/withdraw` | Recebe `amount` e debita a conta, verificando o saldo disponível. |
| `POST` | `/transfer` | Recebe `sourceAccountId`, `destinationAccountId` e `amount`. |
| `GET` | `/{id}/ledger` | Lista os lançamentos com operação, tipo, valor, natureza e data. |

Depósitos, saques e transferências exigem `Content-Type: application/json` e o header `Idempotency-Key` com um UUID. Use uma chave exclusiva para cada nova operação e mantenha a mesma chave ao reenviar a mesma requisição.

### Transferir e consultar o resultado

Com as contas de demonstração em seu estado inicial, envie R$ 25,00 da origem para o destino:

```bash
curl -i -X POST http://localhost:8080/api/v1/transfer \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 50000000-0000-4000-8000-000000000001' \
  -d '{
    "sourceAccountId": "a7edac6c-8295-4b4a-86eb-104c5729b092",
    "destinationAccountId": "32a03fb7-c06e-45e2-86b0-417919e69f67",
    "amount": 25.00
  }'
```

A resposta de sucesso contém `transferId`, `status`, `amount`, `sourceAccountId` e `destinationAccountId`. Nesse cenário, o status é `COMPLETED` e os saldos passam a R$ 975,00 e R$ 1.025,00.

Consulte a conta de origem:

```bash
curl http://localhost:8080/api/v1/accounts/a7edac6c-8295-4b4a-86eb-104c5729b092
```

```json
{
  "name": "Conta Demo Origem",
  "status": "ACTIVE",
  "balance": 975.00
}
```

Consulte os lançamentos das duas contas:

```bash
curl http://localhost:8080/api/v1/a7edac6c-8295-4b4a-86eb-104c5729b092/ledger
curl http://localhost:8080/api/v1/32a03fb7-c06e-45e2-86b0-417919e69f67/ledger
```

O extrato inclui `accountId`, `transferId`, `entryType`, `amount`, `nature` e `createdAt`. O débito da origem e o crédito do destino compartilham o mesmo `transferId`.

Ao repetir sequencialmente a transferência com a mesma chave e os mesmos parâmetros, o serviço retorna a operação persistida. Se os parâmetros mudarem, a comparação de hash resulta em `409 Conflict`.

### Criar outra conta

```bash
curl -i -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"name":"Conta pessoal"}'
```

A resposta contém `id`, `name`, `status`, `balance` e `createdAt`. O nome deve ser diferente dos já cadastrados. Para movimentar essa nova conta, use o `id` retornado na URL ou no corpo da transferência.

### Tratamento de erros

O `GlobalExceptionHandler`, implementado com `@RestControllerAdvice`, centraliza as exceções de domínio e os erros de leitura e validação mapeados. Essas respostas incluem `timestamp`, `status`, `error`, `path` e uma lista `errors` com `field` e `message`.

| Status | Situações tratadas |
| --- | --- |
| `400 Bad Request` | Regras de valor monetário, saldo insuficiente, chave ausente e erros de corpo ou validação. |
| `403 Forbidden` | Conta bloqueada em depósitos e saques. |
| `404 Not Found` | Conta não encontrada na consulta de saldo ou na movimentação. |
| `409 Conflict` | Nome de conta duplicado, chave com parâmetros incompatíveis e transferência para a mesma conta ou envolvendo conta bloqueada. |

## Testes

Os testes unitários ficam em `src/test/java`, seguem o pacote do código testado e usam o sufixo `Test`. Os testes de integração ficam no pacote `com.corebaking.integration` e usam o sufixo `IT`. O Maven Surefire executa os unitários com `test`; o Maven Failsafe executa os testes de integração com `verify`.

O teste [`TransferServiceIT`](src/test/java/com/corebaking/integration/service/TransferServiceIT.java) inicia a API em uma porta aleatória e usa **PostgreSQL 16 real em um container isolado**, gerenciado pelo Testcontainers.

| Cenário automatizado | O que o teste verifica |
| --- | --- |
| Disputa pelo mesmo saldo | Duas requisições HTTP de R$ 80,00 partem simultaneamente de uma conta com R$ 100,00. O teste verifica um único HTTP `200`, saldo final de R$ 20,00 na origem, crédito em apenas um destino e uma única operação persistida. |
| Chave entre depósito e saque | Reutilizar a chave do depósito de R$ 100,00 no saque de R$ 100,00 da mesma conta deve retornar HTTP `409`, mantendo apenas o depósito e seu lançamento. |
| Proteção do histórico | Tentativas de `UPDATE` e `DELETE` em `transfers` e `ledger_entries` são rejeitadas pelo banco e os registros permanecem com os valores originais. |
| Documentação da API | OpenAPI expõe as seis rotas e documenta o header de idempotência como obrigatório; o Swagger UI responde com HTTP `200`. |
| Carregamento do contexto | `CoreBakingApplicationIT` verifica a inicialização do contexto Spring com a configuração da aplicação. |

Para executar somente os testes unitários pelo Maven Wrapper:

```bash
./mvnw -Dtest=TransferServiceTest test
```

Para executar somente a integração de transferências, tenha JDK 21 e Docker ativo:

```bash
./mvnw -Dit.test=TransferServiceIT verify
```

No Windows, use `.\mvnw.cmd` no lugar de `./mvnw`.

<details>
<summary>Executar a suíte completa, incluindo integração e carregamento do contexto</summary>

O teste `CoreBakingApplicationIT.contextLoads` utiliza a configuração padrão da aplicação e precisa de um datasource. Para executar a suíte completa, incluindo os testes Testcontainers, tenha Docker ativo e configure o banco local. Em Bash:

```bash
docker compose up -d postgres
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydatabase \
SPRING_DATASOURCE_USERNAME=postgres \
./mvnw verify
```

O Compose configura o banco `mydatabase`, usuário `postgres` e autenticação sem senha para desenvolvimento local. `TransferServiceIT` usa a conexão de seu próprio container, definida por `@DynamicPropertySource`.

</details>

## Organização

Os pacotes agrupam cada domínio com seus controllers, serviços, DTOs, entidades e repositórios. Os controllers recebem as requisições, os serviços executam as regras de negócio e os repositórios acessam o banco.

```text
src/main/java/com/corebaking/
├── account/       # Cadastro, status e consulta de saldo
├── transaction/   # Depósitos e saques
├── transfers/     # Transferências entre contas
├── ledger/        # Lançamentos financeiros e extratos
├── idempotency/   # Chaves e operações associadas
└── shared/        # Exceções, respostas de erro, valor monetário e hash
```

A entidade `Transfer` registra as operações de depósito, saque e transferência. Os lançamentos em `ledger_entries` vinculam essas operações às contas. O schema é versionado pelo Flyway em [três migrações](src/main/resources/db/migration): criação das tabelas, proteção do histórico e contas de demonstração.

### Tecnologias utilizadas

| Tecnologia | Uso |
| --- | --- |
| Java 21 e Spring Boot 4.1.2 | Aplicação e endpoints REST com Spring MVC. |
| Spring Data JPA e Hibernate | Persistência, transações e bloqueio pessimista. |
| PostgreSQL 16 e Flyway | Dados relacionais, triggers e migrações versionadas. |
| Springdoc OpenAPI 3.2.0 | Especificação da API e Swagger UI. |
| JUnit Jupiter e Testcontainers 1.21.4 | Testes de integração com banco em container. |
| Docker Compose e Maven Wrapper | Ambiente local, build e execução. |
| Lombok | Geração de construtores e métodos das entidades. |

As dependências estão declaradas no [`pom.xml`](pom.xml).

## Escopo atual

- O hash de depósitos e saques usa conta e valor, sem incluir o tipo da operação. A mesma chave não deve ser compartilhada entre esses endpoints.
- A cobertura automatizada está concentrada nos três cenários de integração descritos e no carregamento do contexto. 

## Autor

[GuilhermeSaar](https://github.com/GuilhermeSaar) · Desenvolvimento backend com Java.
