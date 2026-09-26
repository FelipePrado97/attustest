# Nota técnica — decisões, trade-offs e melhorias futuras

Este documento registra **por que** o sistema é como é. O código não tem comentários de propósito: a intenção fica nos nomes (classes, métodos, constantes), nos testes e aqui.

---

## 1. Arquitetura

### 1.1 Hexagonal pragmática

**Decisão.** Domínio puro no centro, casos de uso na aplicação, adapters nas bordas; dependências sempre para dentro.

- `domain` não importa Spring, JPA, Jackson nem Kafka — só Java.
- `application` define **portas de entrada** (um contrato por caso de uso) e **portas de saída** (repositório, publicador de eventos, histórico).
- `adapter.in` (REST, consumidor Kafka) chama portas de entrada; `adapter.out` (JPA, outbox) implementa portas de saída.
- As fronteiras são testadas com **ArchUnit** (`ArquiteturaHexagonalTest`): o build quebra se o domínio importar um framework, se um controller depender de uma implementação de caso de uso, se um adapter de entrada conhecer um de saída etc.

**Trade-off.** As implementações dos casos de uso usam `@Transactional` (anotação do `spring-tx`). A alternativa "pura" seria um decorator transacional com `TransactionTemplate` para cada caso de uso — mais código e indireção para ganho quase nulo neste contexto. Os casos de uso continuam POJOs **sem** `@Service`/`@Component` e são testados com `new`.

### 1.2 Um controller e um caso de uso por operação

Cada operação (cadastrar, listar, buscar, atualizar, alterar status, excluir, consultar histórico) tem seu controller, sua porta e sua implementação. Cada classe tem **uma razão para mudar** (SRP) e cada adapter depende só da interface de que precisa (ISP/DIP).

### 1.3 Composition root

`config/UseCaseConfig` é o único lugar que conhece as implementações e as monta (`new CadastrarProcessoService(...)`). O `@Transactional` continua valendo porque o proxy do Spring é aplicado a qualquer bean, inclusive os criados via `@Bean` — o teste de integração comprova isso (o adapter do outbox exige `Propagation.MANDATORY` e falharia sem transação).

---

## 2. Modelagem do domínio

| Decisão | Motivo |
|---|---|
| **Agregado `Processo`** valida tudo **antes** de qualquer mutação (via `DadosValidados`) | Uma operação rejeitada nunca deixa o agregado pela metade. |
| **`NumeroCnj` como value object** que valida no construtor | Não existe instância inválida — nem criada diretamente com `new`. Entrada com ou sem máscara é normalizada; igualdade por valor. |
| **Máquina de estados no enum `StatusProcesso`** | As transições vivem num único lugar, em vez de `if/else` espalhados. |
| **Valor da causa rejeitado (não arredondado)** com mais de 2 casas | Dinheiro não se arredonda em silêncio. O limite de 13 dígitos inteiros espelha a coluna `NUMERIC(15,2)` — sem isso, um valor grande estouraria no banco como erro 500. |
| **Data de distribuição**: ≥ 01/01/1900, ≥ ano de ajuizamento (`AAAA` do CNJ), ≤ hoje | A distribuição acontece depois do ajuizamento, cujo ano está no próprio número. O piso de 1900 barra erros de digitação como o ano `0004`. |
| **Regras de data não são reaplicadas na reconstituição** (`DadosValidados.jaGravados`) | "Hoje" muda com o tempo; um registro válido quando foi gravado não pode ficar ilegível depois. |
| **`Clock` injetado, fuso `America/Sao_Paulo`** | Testes determinísticos e "data futura" avaliada no calendário brasileiro (perto da meia-noite, UTC já é o dia seguinte). Instantes são gravados em UTC. |
| **Exceções de domínio numa hierarquia selada** (`DadoInvalido`, `RecursoNaoEncontrado`, `Conflito`, `RegraNegocio`) | O adapter web mapeia cada categoria para um status HTTP com um `switch` exaustivo; o domínio não conhece HTTP. |
| **Eventos de domínio registrados pelo agregado** e extraídos pela aplicação | O agregado decide *o que* aconteceu; a aplicação decide *como* publicar. |

---

## 3. Consistência e concorrência

- **Controle otimista em duas camadas:** a API exige a `versao` lida (checada no caso de uso, erro claro para o usuário) e a entidade tem `@Version` (protege a corrida entre duas requisições que leram a mesma versão). O teste de integração dispara duas edições simultâneas e verifica que exatamente uma vence.
- **Exclusão também protegida:** o adapter remove pela entidade carregada (não pelo id), para o `@Version` valer no `DELETE`.
- **Tradução de erros de infraestrutura no adapter de persistência:** violação da constraint `uk_processo_numero_cnj` vira `ConflitoException.numeroDuplicado` e `OptimisticLockingFailureException` vira `ConflitoException.versaoDesatualizada`. O `ApiExceptionHandler` não conhece JPA. Qualquer outra violação de integridade é bug e responde 500 — não é mascarada como 409.

---

## 4. Mensageria

### 4.1 Transactional Outbox (em vez de *dual write*)

Publicar no Kafka dentro da transação do banco cria dois cenários ruins: o commit falha depois da mensagem enviada (evento de algo que não aconteceu) ou o envio falha depois do commit (alteração sem evento). A alteração e o evento são gravados **na mesma transação** (`outbox_evento`); o `OutboxRelay` publica depois. Entrega *at-least-once*, compensada pela idempotência do consumidor.

### 4.2 Relay resiliente

| Situação | Comportamento |
|---|---|
| Ordem dos eventos | Chave da mensagem = id do processo (mesma partição). Eventos saem na ordem da coluna `sequencia` — **não** de `criado_em`, que empatava no mesmo milissegundo e fazia `EXCLUIDO` sair antes de `CADASTRADO` (bug pego pelos testes). |
| Evento com falha própria (ex.: mensagem maior que o limite do broker) | Só **o processo afetado** espera, com backoff exponencial por evento (1 s, 2 s, 4 s… até 5 min). Os demais processos seguem — a versão inicial dava `break` e um único evento travava a fila inteira. |
| Broker indisponível (timeout / erro retentável) | O ciclo é interrompido: todos os envios falhariam. `max.block.ms` curto evita que o `send()` fique 60 s bloqueado esperando metadados. |
| Crescimento da tabela | `OutboxLimpeza` expurga diariamente os eventos publicados há mais de 7 dias. |
| Alertas | Métricas `outbox.eventos.pendentes` e `outbox.eventos.com.falha`. |

### 4.3 Consumidor, retry e DLT

- **Idempotente:** o `eventoId` é a chave primária do histórico — a constraint garante um único registro mesmo com entregas repetidas ou simultâneas.
- **Não engole exceções:** quem decide retry é o `DefaultErrorHandler` (backoff exponencial). Engolir a exceção faria o offset ser commitado e a mensagem se perder em silêncio.
- **Falhas que nunca vão passar** (JSON inválido, regra de domínio) vão **direto** para o DLT, sem retry.
- **Destino do DLT explícito** (`<tópico>.DLT`, mesma partição de origem): no spring-kafka 4 o sufixo padrão mudou para `-dlt`, e as mensagens estavam indo para um tópico "fantasma" criado automaticamente (bug pego pelo teste de integração). No `docker-compose.yml` a criação automática de tópicos está **desligada**, para que um nome errado falhe alto.
- **Linha do tempo ordenada por sequência de consumo**, pela mesma razão do outbox: timestamps empatam; a ordem de consumo é a ordem causal (mesma partição por processo).

---

## 5. API

- **Problem Details (RFC 9457)** em todos os erros, com `correlationId` — inclusive nos erros gerados pelo próprio Spring (404 de rota, 405, 415). O ponto de extensão correto é `createResponseEntity`: em `handleExceptionInternal` o corpo dessas exceções ainda é nulo (bug pego no teste de fumaça contra o ambiente real).
- **500 genérico** sem vazar detalhes internos; o stack trace fica no log.
- **`X-Correlation-Id` recebido só é aceito se for seguro para log** (`[A-Za-z0-9-]{8,64}`); caso contrário é gerado um novo — evita *log injection*.
- **Validação em duas camadas:** Bean Validation nos DTOs (feedback de todos os campos de uma vez) e invariantes no domínio (fonte da verdade, protege qualquer outro chamador da porta). Os limites dos DTOs usam as constantes do domínio — sem números mágicos duplicados.
- **Paginação estável:** ordenação por `atualizadoEm` com desempate por `id`; sem o desempate, itens com o mesmo timestamp repetiam ou sumiam entre páginas.
- **Busca por CNJ sem máscara:** coluna `numero_cnj_digitos`. Um termo "com cara de número" (só dígitos e pontuação da máscara) busca nela, inclusive parcialmente; um termo textual como "IPTU 2024" não casa com todo processo de 2024. Curingas do `LIKE` (`%`, `_`) são tratados como texto literal.
- **Logs em ASCII, mensagens ao usuário em pt-BR com acento:** logs são pesquisados com `grep` e atravessam terminais e coletores com codificações diversas.

---

## 6. Front-end

| Decisão | Motivo |
|---|---|
| Angular 22 standalone, signals, `OnPush`, rotas lazy | Padrões atuais do framework; bundle inicial abaixo de 500 kB. |
| **URL como fonte da verdade** da lista (termo, status, página) | Voltar, recarregar e compartilhar o link reproduzem o mesmo resultado. |
| Validações espelhando o backend (CNJ com dígito verificador, datas, valores) | Feedback imediato; o backend continua sendo a fonte da verdade. |
| Erros do servidor aplicados no campo correspondente | CNJ duplicado aparece no próprio campo, não num aviso genérico. |
| **Adapter de datas próprio** (`DataBrasileiraAdapter`) | O adapter nativo delega ao `Date.parse`, que não entende `dd/mm/aaaa` e transforma o ano `0004` em `1904` sem avisar. |
| **Diretiva de máscara registrada na fase de captura** | Roda antes dos listeners do Angular Forms e do datepicker, que assim nunca leem uma letra que a máscara vai remover. Preserva a posição do cursor. |
| Providers do calendário declarados no componente do formulário | Carregados sob demanda: colocá-los no `app.config` estourava o orçamento do bundle inicial. |
| **Sincronização da linha do tempo sem estado** (`historicoPendente`) | Toda alteração gera um evento com `ocorridoEm ≥ atualizadoEm`. Enquanto nenhum evento cobre a última alteração, a tela consulta de novo (até 6 vezes, 1 por segundo). Serve para cadastro, edição e mudança de status sem flags de navegação. |
| **Gerador de CNJ** no cadastro | Conveniência para quem ainda não tem o número (na prática, ele é atribuído pelo tribunal). Usa o mesmo algoritmo de dígito verificador da validação e o ano corrente. |
| Nginx com proxy de `/api` | Front e API na mesma origem: sem CORS e sem URL de backend embutida no bundle. |

---

## 7. Testes e entrega

- **Pirâmide:** muitos testes unitários de domínio e de casos de uso, `@WebMvcTest`/`@DataJpaTest` nas bordas e poucos testes de integração ponta a ponta.
- **H2 em modo PostgreSQL + Kafka embarcado** nos testes de integração: rodam sem Docker e em segundos. *Trade-off:* H2 não é PostgreSQL — ver melhorias (Testcontainers).
- **Travas de cobertura** no build (JaCoCo e Vitest): 90% de linhas e 85% de branches.
- **Schema autodocumentado:** toda tabela e coluna tem `COMMENT ON` nas migrations — a descrição fica no catálogo do banco e aparece em qualquer ferramenta (psql, DBeaver, IDE). O `SchemaDocumentadoTest` quebra o build se uma tabela ou coluna nova ficar sem descrição.
- **Tudo roda com Docker**, inclusive as suítes de teste (profile `test`). Imagens multi-stage, execução sem root, JRE Alpine.
- Particularidades de ambiente tratadas: no Windows, o NIO cria sockets locais no diretório temporário do usuário e caminhos com acento quebravam o Kafka embarcado (`jdk.net.unixdomain.tmpdir` aponta para `target/`); o Vitest usa poucos workers para ser estável em containers com CPU e I/O limitados.

---

## 8. Melhorias futuras

| Tema | Melhoria |
|---|---|
| Segurança | Autenticação e autorização (OAuth2/OIDC, ex.: Keycloak) e auditoria de **quem** alterou cada processo (usuário no evento e na linha do tempo). |
| Testes | Testcontainers com PostgreSQL e Kafka reais nos testes de integração; testes end-to-end do front (Playwright). |
| Outbox em múltiplas instâncias | Hoje, duas instâncias da API podem publicar o mesmo evento (o consumidor idempotente absorve). Para escalar: `SELECT ... FOR UPDATE SKIP LOCKED` na leitura dos pendentes, ou CDC com Debezium no lugar do relay. |
| Tempo real | Server-Sent Events/WebSocket para atualizar a linha do tempo em vez de consulta periódica. |
| Observabilidade | OpenTelemetry (tracing distribuído substituindo o correlation id manual), dashboards e alertas (Prometheus/Grafana) sobre as métricas do outbox e o tamanho do DLT, reprocessamento assistido do DLT. |
| Consultas | JPA Metamodel para critérios *type-safe*; índice trigram (`pg_trgm`) para a busca por texto em grande volume. |
| Domínio | Exclusão lógica em vez de física; mais entidades do contexto (prazos, partes, movimentações). |
| Entrega | Pipeline de CI (GitHub Actions) executando as duas suítes, as travas de cobertura e o build das imagens. |
