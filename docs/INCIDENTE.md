# Análise de incidente — linha do tempo dos processos parou de ser atualizada

| | |
|---|---|
| **Severidade** | Alta — funcionalidade degradada para todos os usuários, sem perda de dados |
| **Componente** | `OutboxRelay` (publicação dos eventos de domínio no Kafka) |
| **Sintoma** | Alterações salvas com sucesso, mas a linha do tempo de **todos** os processos deixa de refletir as mudanças |
| **Causa raiz** | O relay interrompia o ciclo na primeira falha; um único evento impossível de publicar bloqueava todos os eventos atrás dele, indefinidamente |
| **Gatilho** | Evento de 1,6 MB gravado no outbox por uma carga de dados legados — acima do limite de 1 MB do Kafka |
| **Estado** | Resolvido — correção, remediação e ações preventivas aplicadas e verificadas |

> **Sobre este cenário.** O defeito analisado é real: foi encontrado durante o desenvolvimento, na primeira versão do relay. Para esta análise, a versão defeituosa foi reconstituída numa cópia isolada do código e **executada contra a stack real** (PostgreSQL + Kafka), com um gatilho controlado. Todos os logs, consultas e medições abaixo são **saídas reais** dessa execução, em 26/09/2026 (horários em UTC). Os arquivos completos estão em [`incidente/logs/`](incidente/logs/).

---

## 1. Resumo

Um script de carga de processos legados gravou diretamente na tabela `outbox_evento` um evento de 1,6 MB (um anexo em Base64) para o processo **A**. O produtor Kafka recusa mensagens acima de 1 MB (`RecordTooLargeException`). A primeira versão do `OutboxRelay` publicava os pendentes em ordem e **parava o ciclo na primeira falha** — então tentava o mesmo evento a cada segundo, falhava, e nunca chegava aos eventos seguintes.

A API continuou respondendo normalmente (201/200), o que tornou o problema silencioso: os usuários salvavam alterações, mas a linha do tempo de **qualquer** processo — inclusive os que não tinham relação com a carga — parou de ser atualizada.

## 2. Impacto

| Aspecto | Efeito |
|---|---|
| Linha do tempo | Congelada para todos os processos enquanto o evento problemático estivesse no outbox (na versão defeituosa, **indefinidamente**) |
| Operações do usuário | Não afetadas: cadastro, edição e mudança de status continuaram respondendo com sucesso |
| Dados | **Nenhuma perda**: os eventos retidos ficaram gravados no outbox e foram entregues após a correção — o padrão Transactional Outbox cumpriu seu papel |
| Na reprodução | Processo C ficou **1 min 56 s** sem o evento de cadastro na linha do tempo; a suspensão do processo B, idem |
| Efeito colateral | O conteúdo do evento (com dados de processo) foi gravado no log de erro a cada tentativa |

## 3. Linha do tempo (UTC)

| Horário | Evento |
|---|---|
| 14:40:40 | Processos **A** e **B** cadastrados; eventos publicados e registrados na linha do tempo em ~200 ms (operação normal) |
| **14:40:45** | **Gatilho:** carga de legados grava evento de 1,6 MB para o processo A (`correlation_id = carga-legado-lote-0917`) |
| 14:40:46 | Primeira falha: `RecordTooLargeException`. O relay para o ciclo e registra apenas `erro=Send failed` |
| 14:40:48 | Usuário cadastra o processo **C** (201) e suspende o **B** (200) — os dois eventos ficam presos atrás do evento problemático |
| 14:41:13 | Estado: 3 eventos pendentes, 35 tentativas falhas, linha do tempo de C vazia, suspensão de B ausente |
| **14:42:12** | **Correção implantada** (relay que bloqueia só o processo afetado). Evento problemático entra em espera; eventos de C e B publicados no mesmo ciclo |
| 14:42:18 | Nova edição no processo A — retida atrás do evento problemático, por projeto (ordem por processo) |
| 14:42:44 | Consumidor recebe as partições (32 s após a subida — ver [seção 8](#8-achado-secundário-pausa-no-consumo-após-encerramento-forçado)); linha do tempo de C e B atualizada |
| **14:42:58** | **Remediação:** evento problemático preservado em arquivo e retirado do outbox |
| 14:42:59 | Edição do processo A publicada 1 s depois; métricas do outbox voltam a zero |
| 14:46:18 | **Verificação preventiva:** o mesmo gatilho é recusado pelo banco (nova constraint) |

---

## 4. Evidências comentadas

### 4.1 Logs da versão com defeito

Arquivo completo: [`01-versao-com-defeito.log`](incidente/logs/01-versao-com-defeito.log). Trechos abreviados (removidos PID e nome da aplicação).

**Operação normal antes do gatilho:**

```
14:40:40.744 INFO  [cid:inc-cadastro-processo-a] CadastrarProcessoService : Processo cadastrado [processoId=e8daf768-..., numeroCnj=0004821-33.2026.8.26.0100]
14:40:41.060 INFO  [cid:inc-cadastro-processo-a] OutboxRelay              : Evento publicado [eventoId=18653175-..., tipo=CADASTRADO, processoId=e8daf768-..., particao=0, offset=1]
14:40:41.080 INFO  [cid:inc-cadastro-processo-a] RegistrarHistoricoService: Historico registrado [eventoId=18653175-..., tipo=CADASTRADO]
```

> 💬 O mesmo `cid` (correlation id) aparece na requisição HTTP, na publicação pelo relay e no registro pelo consumidor — é o que permite seguir uma operação de ponta a ponta. Latência cadastro → linha do tempo: ~340 ms.

**A falha, repetida a cada segundo:**

```
14:40:46.099 ERROR [cid:carga-legado-lote-0917] LoggingProducerListener : Exception thrown when sending a message with key='e8daf768-...'
                    and payload='{"origem":"carga-legado","anexoBase64":"QUJDQUJDQUJDQUJDQUJDQUJD...' to topic processos.eventos:
org.apache.kafka.common.errors.RecordTooLargeException: The message is 1600268 bytes when serialized which is larger than 1048576,
                    which is the value of the max.request.size configuration.
14:40:46.126 ERROR [cid:carga-legado-lote-0917] OutboxRelay : Falha ao publicar evento, nova tentativa no proximo ciclo
                    [eventoId=b131363a-..., tentativas=1, erro=Send failed]
14:40:47.153 ERROR [cid:carga-legado-lote-0917] OutboxRelay : Falha ao publicar evento, nova tentativa no proximo ciclo
                    [eventoId=b131363a-..., tentativas=2, erro=Send failed]
14:40:48.179 ERROR [cid:carga-legado-lote-0917] OutboxRelay : ... tentativas=3, erro=Send failed]
```

> 💬 **Causa imediata:** a mensagem tem 1.600.268 bytes; o produtor aceita até 1.048.576 (`max.request.size`). É uma falha **permanente** — tentar de novo nunca vai funcionar.
>
> 💬 **O `cid` identifica a origem:** `carga-legado-lote-0917` não é uma requisição de usuário, é o lote da carga de legados. Sem o correlation id, a investigação começaria sem pista de quem gravou o evento.
>
> 💬 **Diagnóstico mascarado:** o relay registrou só `erro=Send failed` — a mensagem da exceção que *embrulha* a causa real. A causa (`RecordTooLargeException`) só aparece no log do Spring. Quem consultasse a coluna `ultimo_erro` no banco também veria apenas `KafkaException: Send failed`.
>
> 💬 **Dado sensível no log:** o `LoggingProducerListener` do Spring escreve o conteúdo da mensagem (`payload='...'`). Em eventos de processo, isso inclui nomes de partes e valores — dados pessoais indo para o log a cada tentativa, com as implicações de LGPD de retenção e acesso a logs.

**Operações do usuário durante o incidente — sucesso na API, nada na linha do tempo:**

```
14:40:48.601 INFO  [cid:inc-cadastro-processo-c] CadastrarProcessoService : Processo cadastrado [processoId=382a06df-..., numeroCnj=0012550-29.2026.8.26.0224]
14:40:48.606 INFO  [cid:inc-cadastro-processo-c] http.access              : POST /api/v1/processos -> 201 (12 ms)
14:40:48.691 INFO  [cid:inc-suspensao-processo-b] AlterarStatusProcessoService : Status do processo alterado [processoId=4fd7aa28-..., de=ATIVO, para=SUSPENSO]
14:40:48.695 INFO  [cid:inc-suspensao-processo-b] http.access              : PATCH /api/v1/processos/4fd7aa28-.../status -> 200 (17 ms)
14:40:49.204 ERROR [cid:carga-legado-lote-0917] OutboxRelay : Falha ao publicar evento, nova tentativa no proximo ciclo [... tentativas=4 ...]
```

> 💬 Não há nenhum `Evento publicado` para `inc-cadastro-processo-c` nem para `inc-suspensao-processo-b` — em toda a janela capturada, o relay só tenta o evento da carga. **Nenhum log de erro menciona os processos B ou C**: o sintoma só aparece para quem olha a linha do tempo.

### 4.2 Estado do outbox e métricas (14:41:13)

```sql
SELECT sequencia, tipo, left(agregado_id::text, 8) AS processo, correlation_id, tentativas,
       octet_length(payload) AS bytes, left(ultimo_erro, 70) AS ultimo_erro
FROM outbox_evento WHERE publicado_em IS NULL ORDER BY sequencia;
```

```
 sequencia |      tipo       | processo |      correlation_id      | tentativas |  bytes  |         ultimo_erro
-----------+-----------------+----------+--------------------------+------------+---------+-----------------------------
         4 | ATUALIZADO      | e8daf768 | carga-legado-lote-0917   |         35 | 1600042 | KafkaException: Send failed
         5 | CADASTRADO      | 382a06df | inc-cadastro-processo-c  |          0 |     243 |
         6 | STATUS_ALTERADO | 4fd7aa28 | inc-suspensao-processo-b |          0 |     267 |
```

```
outbox_eventos_com_falha 1.0
outbox_eventos_pendentes 3.0
```

> 💬 A tabela conta a história inteira: o evento da `sequencia` 4 tem 35 tentativas e 1,6 MB; os eventos 5 e 6 (de **outros** processos, gravados depois) têm **zero** tentativas — o relay nunca chegou a eles. Eventos típicos têm ~250 bytes: o evento problemático é ~6.400 vezes maior.
>
> 💬 As duas métricas já existiam e **mostravam o problema** (`com_falha > 0`, `pendentes` sem zerar), mas não havia alerta sobre elas — foram vistas só na investigação.

### 4.3 O que o usuário via

```
GET /processos/{A}/historico → CADASTRADO
GET /processos/{B}/historico → CADASTRADO                    (falta STATUS_ALTERADO)
GET /processos/{C}/historico → (vazio)                        (falta CADASTRADO)
```

---

## 5. Diagnóstico e causa raiz

### Cinco porquês

1. **Por que a linha do tempo parou?** Porque nenhum evento novo chegava ao Kafka.
2. **Por que nenhum evento chegava?** Porque o relay tentava sempre o mesmo evento e nunca avançava na fila.
3. **Por que não avançava?** Porque, na primeira falha, o relay interrompia o ciclo (`break`) — tratava toda falha como "tente de novo mais tarde", sem distinguir falha temporária de falha permanente.
4. **Por que um evento falhava permanentemente?** Porque tinha 1,6 MB, acima do limite do Kafka, gravado por um processo (carga de legados) que escreve direto na tabela, sem passar pela aplicação.
5. **Por que isso não foi detectado antes?** Porque não havia limite de tamanho na gravação, o erro registrado escondia a causa (`Send failed`) e as métricas existentes não tinham alerta.

### O defeito no código

Primeira versão do relay:

```java
public void publicarPendentes() {
    for (var evento : repository.findTop500ByPublicadoEmIsNullOrderBySequenciaAsc()) {
        if (!publicar(evento)) {
            break;   // qualquer falha interrompe o ciclo — um evento problemático bloqueia todos os seguintes
        }
    }
}
```

A intenção do `break` era correta — **preservar a ordem** dos eventos (não publicar um evento posterior antes de um anterior). O erro foi o escopo: a ordem só precisa ser garantida **dentro de cada processo** (a chave da mensagem no Kafka é o id do processo), mas o bloqueio era **global**.

### Fatores contribuintes

| Fator | Efeito |
|---|---|
| Falha permanente tratada como temporária | Nova tentativa a cada 1 s, para sempre, sem espera crescente |
| Gravação sem limite de tamanho | Qualquer escritor podia gravar um evento impossível de publicar |
| Causa raiz mascarada | `ultimo_erro` e o log do relay registravam `Send failed`, não `RecordTooLargeException` |
| Métricas sem alerta | `outbox.eventos.com.falha` e `outbox.eventos.pendentes` mostravam o problema, mas ninguém era avisado |
| Conteúdo da mensagem no log | Dados de processo gravados em log de erro a cada tentativa |

---

## 6. Correção

### 6.1 Relay que bloqueia só o processo afetado

[`OutboxRelay.java`](../backend/src/main/java/br/com/attus/processos/adapter/out/messaging/OutboxRelay.java):

```java
public void publicarPendentes() {
    var agora = Instant.now(clock);
    Set<UUID> processosBloqueados = new HashSet<>();
    for (var evento : repository.findTop500ByPublicadoEmIsNullOrderBySequenciaAsc()) {
        if (processosBloqueados.contains(evento.getAgregadoId())) {
            continue;
        }
        if (evento.aguardandoBackoff(agora)) {
            processosBloqueados.add(evento.getAgregadoId());
            continue;
        }
        switch (publicar(evento)) {
            case PUBLICADO -> { }
            case FALHA_DO_EVENTO -> processosBloqueados.add(evento.getAgregadoId());
            case BROKER_INDISPONIVEL -> {
                return;
            }
        }
    }
}
```

| Situação | Comportamento |
|---|---|
| Falha **do evento** (ex.: tamanho) | Só o processo dono do evento espera; nova tentativa com backoff exponencial (1 s, 2 s, 4 s… até 5 min). Os demais processos seguem. |
| **Broker indisponível** (timeout, erro retentável) | Interrompe o ciclo — aí sim todos os envios falhariam. |

**Evidência** — arquivo completo: [`02-versao-corrigida-e-remediacao.log`](incidente/logs/02-versao-corrigida-e-remediacao.log):

```
14:42:12.414 INFO  ProcessosApiApplication : Started ProcessosApiApplication in 7.422 seconds
14:42:12.765 ERROR [cid:carga-legado-lote-0917] OutboxRelay : Falha ao publicar evento; processo bloqueado ate nova tentativa
                    [eventoId=b131363a-..., processoId=e8daf768-..., tentativas=76, proximaTentativa=2026-09-26T14:47:12Z, ...]
14:42:12.794 INFO  [cid:inc-cadastro-processo-c]  OutboxRelay : Evento publicado [tipo=CADASTRADO, processoId=382a06df-..., particao=2, offset=0]
14:42:12.806 INFO  [cid:inc-suspensao-processo-b] OutboxRelay : Evento publicado [tipo=STATUS_ALTERADO, processoId=4fd7aa28-..., particao=0, offset=3]
```

> 💬 No **mesmo ciclo** (41 ms), o evento problemático falha, é colocado em espera até 14:47:12 (teto de 5 min, pois já acumulava 75 falhas) e os eventos de C e B — presos há quase 1 minuto e meio — são publicados.

```
14:42:18.336 INFO  [cid:inc-edicao-processo-a] AtualizarProcessoService : Processo atualizado [processoId=e8daf768-..., versao=1]
```

> 💬 A nova edição do processo **A** fica retida — **por projeto**: publicá-la antes do evento anterior do mesmo processo quebraria a ordem da linha do tempo. O raio de impacto do evento problemático caiu de "todos os processos" para "somente o processo A".

### 6.2 Ações corretivas desta análise

| Ação | Onde | Verificação |
|---|---|---|
| **Causa raiz no diagnóstico:** `ultimo_erro` e o log do relay passam a registrar a exceção mais específica (`RecordTooLargeException: The message is 1600268 bytes…`) em vez de `Send failed` | [`OutboxRelay.descrever`](../backend/src/main/java/br/com/attus/processos/adapter/out/messaging/OutboxRelay.java) | Teste de regressão + execução real (abaixo) |
| **Barreira na gravação:** constraint limita o evento a 512 KB — metade do limite do Kafka. Vale para qualquer escritor, inclusive scripts que não passam pela aplicação | [`V3__limita_tamanho_do_evento_no_outbox.sql`](../backend/src/main/resources/db/migration/V3__limita_tamanho_do_evento_no_outbox.sql) | Testes de persistência + execução real (abaixo) |
| **Sem dados pessoais no log:** o listener de erros do produtor Kafka deixa de gravar chave e conteúdo da mensagem | [`KafkaConfig.kafkaProducerListenerSemDadosPessoaisNoLog`](../backend/src/main/java/br/com/attus/processos/config/KafkaConfig.java) | Execução real (abaixo) |
| **Teste de regressão do incidente:** reproduz a falha síncrona `KafkaException("Send failed")` embrulhando `RecordTooLargeException`, com outro processo na fila | [`OutboxRelayTest.incidenteEventoGrandeDemais_...`](../backend/src/test/java/br/com/attus/processos/adapter/out/messaging/OutboxRelayTest.java) | Suíte de testes |

**O mesmo gatilho, com a barreira aplicada** — [`03-acao-preventiva.txt`](incidente/logs/03-acao-preventiva.txt):

```
14:46:13 INFO  DbMigrate : Migrating schema "public" to version "3 - limita tamanho do evento no outbox"
ERROR:  new row for relation "outbox_evento" violates check constraint "ck_outbox_evento_payload_tamanho"
outbox_eventos_com_falha 0.0
outbox_eventos_pendentes 0.0
```

> 💬 A carga de legados recebe o erro **no momento da gravação**, com a causa explícita. O outbox, o relay e os demais processos nem tomam conhecimento — a falha volta para quem a causou, que é quem pode corrigi-la.

**Log do produtor depois da correção** (evento grande forçado em teste, com a constraint desligada temporariamente):

```
14:48:37.246 ERROR [cid:verificacao-log-sem-dados] LoggingProducerListener : Exception thrown when sending a message to topic processos.eventos:
14:48:37.311 ERROR [cid:verificacao-log-sem-dados] OutboxRelay : Falha ao publicar evento; processo bloqueado ate nova tentativa
                    [eventoId=0174728b-..., tentativas=1, proximaTentativa=2026-09-26T14:48:38Z,
                     erro=RecordTooLargeException: The message is 1200283 bytes when serialized which is larger than 1048576, ...]
```

> 💬 Sem `key=` e sem `payload=` no log; e o relay agora registra a causa real.

---

## 7. Remediação operacional (runbook)

Quando o alerta `OutboxEventoComFalha` disparar:

**1. Identificar o evento retido e a causa**

```sql
SELECT id, agregado_id, tipo, correlation_id, tentativas, octet_length(payload) AS bytes,
       proxima_tentativa_em, ultimo_erro, criado_em
FROM outbox_evento
WHERE publicado_em IS NULL AND tentativas > 0;
```

O `correlation_id` indica a origem (requisição de usuário, lote de carga etc.); o `ultimo_erro` indica a causa.

**2. Decidir pela causa**

| Causa | Ação |
|---|---|
| Transitória (broker, rede) | Nenhuma: o relay retoma sozinho com o backoff |
| Evento corrigível (ex.: conteúdo que pode ser reduzido) | Corrigir o `payload` e zerar a espera: `UPDATE outbox_evento SET proxima_tentativa_em = NULL WHERE id = '<id>'` |
| Evento inválido na origem (caso deste incidente) | Preservar e retirar do outbox (passos 3 e 4) e acionar o responsável pela origem |

**3. Preservar o evento para análise**

```sql
\copy (SELECT * FROM outbox_evento WHERE id = '<id>') TO '/tmp/evento-retido-<id>.csv' WITH CSV HEADER
```

**4. Retirar do outbox**

```sql
DELETE FROM outbox_evento WHERE id = '<id>' AND publicado_em IS NULL;
```

**Execução real:**

```
id                                   | agregado_id                          | tipo       | correlation_id         | tentativas | bytes
b131363a-c0c5-482d-889c-a3569f1d1425 | e8daf768-cbda-4e00-bc7c-53fdf60b3bf4 | ATUALIZADO | carga-legado-lote-0917 |         76 | 1600042

COPY 1                     → /tmp/evento-retido-b131363a-....csv (1.600.375 bytes)
DELETE 1                   → 14:42:58

14:42:59.126 INFO [cid:inc-edicao-processo-a] OutboxRelay               : Evento publicado [tipo=ATUALIZADO, processoId=e8daf768-...]
14:42:59.127 INFO [cid:inc-edicao-processo-a] RegistrarHistoricoService : Historico registrado [tipo=ATUALIZADO, processoId=e8daf768-...]

outbox_eventos_com_falha 0.0
outbox_eventos_pendentes 0.0
```

> 💬 A edição do processo A, retida desde 14:42:18, é publicada **1 segundo** após a remoção — na ordem correta.

---

## 8. Achado secundário: pausa no consumo após encerramento forçado

Na troca de versão, a instância defeituosa foi encerrada à força (`SIGKILL`). A nova instância subiu às 14:42:12, mas o consumidor da linha do tempo só recebeu as partições às 14:42:44:

```
14:42:12.414 INFO  ProcessosApiApplication        : Started ProcessosApiApplication in 7.422 seconds
14:42:44.815 INFO  KafkaMessageListenerContainer  : historico-processos: partitions assigned: [processos.eventos-0, processos.eventos-1, processos.eventos-2]
```

O Kafka só remove do grupo um consumidor que morreu sem avisar depois do `session.timeout.ms` (45 s por padrão); até lá, o novo consumidor espera o rebalanceamento. Medição na mesma stack:

| Encerramento da versão anterior | Consumo retomado em |
|---|---|
| Forçado (`SIGKILL`) | **32,4 s** |
| Gracioso (`SIGTERM`) — o consumidor avisa que está saindo do grupo | **3,1 s** |

**Recomendação:** implantar sempre com encerramento gracioso (padrão do `docker stop` e do Kubernetes, com `terminationGracePeriodSeconds` suficiente) e evitar `kill -9`/`docker rm -f` em deploys. Para encerramentos inevitáveis (OOM, falha de nó), avaliar `session.timeout.ms` menor ao custo de rebalanceamentos mais frequentes.

---

## 9. Prevenção

| Ação | Tipo | Estado |
|---|---|---|
| Relay bloqueia só o processo afetado, com backoff por evento | Correção | ✅ Implementado |
| Distinção entre falha do evento e broker indisponível | Correção | ✅ Implementado |
| Causa raiz registrada em `ultimo_erro` e no log | Diagnóstico | ✅ Implementado |
| Constraint de tamanho do evento (512 KB) no banco | Barreira | ✅ Implementado |
| Log do produtor sem dados pessoais | Segurança / LGPD | ✅ Implementado |
| Teste de regressão do incidente | Teste | ✅ Implementado |
| Métricas `outbox.eventos.com.falha` e `outbox.eventos.pendentes` | Observabilidade | ✅ Já existiam |
| **Alertas** sobre as métricas do outbox, lag do consumidor e DLT | Observabilidade | 📋 Proposto: [`alertas-prometheus.yml`](incidente/alertas-prometheus.yml) |
| Runbook de remediação | Operação | ✅ Seção 7 |
| Cargas de dados passam pela API (ou por um caso de uso de importação) em vez de gravar direto nas tabelas | Processo | 📋 Proposto |
| Anexos fora do evento: guardar o arquivo em storage e publicar só a referência (*claim check*) | Arquitetura | 📋 Proposto |
| Deploy com encerramento gracioso | Operação | 📋 Recomendado (seção 8) |

---

## 10. Lições aprendidas

1. **O outbox funcionou como devia:** nenhuma alteração foi perdida. O problema estava em *como* os eventos saíam dele, não em guardá-los.
2. **Preservar ordem tem escopo.** A garantia necessária era "por processo", e o bloqueio global transformou a falha de um processo em indisponibilidade de todos.
3. **Falha permanente não é falha temporária.** Tentar de novo a cada segundo, para sempre, não é resiliência — é um laço. Backoff e classificação do erro são parte da correção.
4. **Diagnóstico precisa da causa raiz.** `Send failed` atrasa qualquer investigação; `RecordTooLargeException: The message is 1600268 bytes` resolve em segundos.
5. **Métrica sem alerta é só um gráfico.** As métricas certas já existiam e mostravam o problema.
6. **O correlation id pagou seu custo:** `carga-legado-lote-0917` levou direto à origem do evento.
7. **Barreiras devem ficar onde nenhum escritor escapa.** Validar só na aplicação não protegeria contra o script de carga; a constraint no banco protege.
