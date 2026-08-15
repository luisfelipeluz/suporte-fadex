# Central de Chamados com Triagem Inteligente

Help desk interno em que o solicitante **não escolhe categoria nem prioridade**: o próprio
sistema classifica a solicitação a partir do texto, e a equipe de suporte é notificada
**na hora** quando surge algo urgente.

Solução para o desafio técnico da **FADEX** — vaga de Analista de Desenvolvimento.

```
┌─────────────┐   POST /api/chamados    ┌──────────────┐   TriageProvider   ┌───────────────┐
│  Solicitante│ ──────────────────────► │ ChamadoService│ ─────────────────► │  Heurística   │
│  (título +  │                         │              │ ◄───────────────── │  ou Gemini    │
│  descrição) │                         └──────┬───────┘  categoria/priori. └───────────────┘
└─────────────┘                                │
                                               │ evento (após commit)
                                               ▼
                                     ┌────────────────────┐   SSE    ┌──────────────┐
                                     │ EventoRealtime     │ ───────► │  Dashboard   │
                                     │ Publisher          │          │  do ADMIN    │
                                     └────────────────────┘          └──────────────┘
```

---

## Sumário

- [Como executar](#como-executar)
- [Credenciais de teste](#credenciais-de-teste)
- [Tecnologias](#tecnologias)
- [Testando a API](#testando-a-api)
- [Triagem por IA](#triagem-por-ia-como-funciona-e-por-quê)
- [Detecção de duplicados](#detecção-de-chamados-duplicados)
- [Fluxo do chamado e quadro Kanban](#fluxo-do-chamado-e-quadro-kanban)
- [Tempo real](#tempo-real)
- [Arquitetura](#arquitetura)
- [Modelo de dados](#modelo-de-dados)
- [Regras de negócio](#regras-de-negócio)
- [Testes automatizados](#testes-automatizados)
- [Decisões de projeto](#decisões-de-projeto)
- [Segurança](#segurança)

---

## Como executar

### Opção 1 — Docker (recomendado)

Sobe banco, API e interface com um comando:

```bash
git clone https://github.com/luisfelipeluz/suporte-fadex.git
cd suporte-fadex
docker compose up --build
```

Não é preciso criar `.env` nem instalar Java, Node ou MySQL: só Docker.

| Recurso | Endereço |
|---|---|
| Aplicação | <http://localhost> |
| API | <http://localhost/api> |
| Swagger UI | <http://localhost/swagger-ui.html> |

> **Primeira execução:** o MySQL inicializa o data dir do zero, o que pode levar **2 a 4 minutos**
> (bem mais em Docker sobre WSL2). Os `healthcheck` do compose encadeiam a subida — o backend só
> parte quando o banco aceita conexões, e a interface só é publicada quando a API responde. Quando
> o log parar, está pronto. As execuções seguintes sobem em segundos.

> **A stack publica uma única porta no host: a 80.** MySQL e backend conversam pela rede interna do
> compose, e o nginx do frontend serve a API e o Swagger na mesma origem. Isso é proposital: cada
> porta publicada a mais é uma chance de o `docker compose up` falhar numa máquina que já usa a
> 3306 ou a 8080. Para expor MySQL ou backend no host, descomente o bloco `ports` do serviço
> correspondente no `docker-compose.yml`.

### Opção 2 — Execução local

**Pré-requisitos:** JDK 21, Maven 3.9+, Node 20+, e um MySQL 8 acessível.

```bash
# 1. Só o banco em container, publicado no host em localhost:3307
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d mysql

# 2. Backend  → http://localhost:8080
cd backend
mvn spring-boot:run

# 3. Frontend → http://localhost:5173
cd ../frontend
npm install
npm run dev
```

O servidor de desenvolvimento do Vite faz proxy de `/api` para `localhost:8080`, então não há
CORS no fluxo do dia a dia.

> **Por que o `-f` duplicado.** A stack completa publica só a porta 80; rodando o backend fora do
> Docker, o banco precisa estar alcançável no host. Em vez de publicar a porta para todo mundo, o
> [`docker-compose.local.yml`](docker-compose.local.yml) adiciona **apenas** esse mapeamento, e só
> para quem pede. A porta padrão é a 3307 (`MYSQL_PORT` no `.env` muda), porque a 3306 do host
> costuma já estar ocupada por um MySQL instalado localmente.

> **O `.env` não vale aqui.** Ler `.env` é comportamento do Docker Compose, não do Spring nem do
> Vite — `mvn spring-boot:run` ignora o arquivo por completo. Nesta opção, o backend usa os padrões
> do `application.yml` (que já apontam para `localhost:3307`); para mudar qualquer coisa —
> credenciais, provider de IA — exporte no ambiente do processo:
> ```powershell
> $env:TRIAGE_PROVIDER='gemini'; $env:GEMINI_API_KEY='sua-chave'; mvn spring-boot:run
> ```

### Variáveis de ambiente

Todas têm padrão de desenvolvimento e o projeto sobe sem configuração. Para personalizar:

```bash
cp .env.example .env
```

O `.env` está no `.gitignore`. Os valores do `.env.example` são os mesmos padrões já embutidos no
`docker-compose.yml`, então copiá-lo sem editar não altera o comportamento da stack.

### Problemas comuns

<details>
<summary><strong>A porta 80 já está em uso</strong></summary>

Mensagem do tipo `bind: address already in use` ao subir o frontend — normalmente IIS, Apache ou
Skype segurando a 80. Publique a interface em outra porta:

```bash
echo "FRONTEND_PORT_PUBLICO=8081" >> .env
docker compose up --build     # aplicação em http://localhost:8081
```
</details>

<details>
<summary><strong>500 Internal Server Error ao buscar a imagem (Docker Desktop)</strong></summary>

`unable to get image 'suporte-fadex-frontend': request returned 500 Internal Server Error` — o
armazenamento de imagens do Docker Desktop ficou inconsistente. Não vem do projeto; o caminho
mais curto é reiniciar o engine de verdade:

```bash
docker compose down --remove-orphans
# sair do Docker Desktop pela bandeja (Quit) e, no Windows: wsl --shutdown
# reabrir o Docker Desktop e esperar "Engine running"
docker compose up --build
```

Se persistir, apague a imagem quebrada e reconstrua:
`docker image rm -f suporte-fadex-frontend && docker compose build --no-cache frontend`.
</details>

<details>
<summary><strong>O backend não autentica no MySQL</strong></summary>

Usuário e senha do MySQL só são gravados quando o volume é criado, na primeira subida. Se você
mudou `MYSQL_USER`/`MYSQL_PASSWORD` depois disso, recrie o volume — o banco é recriado pelas
migrations e pelo seed:

```bash
docker compose down -v && docker compose up --build
```
</details>

<details>
<summary><strong>Começar do zero</strong></summary>

```bash
docker compose down -v --remove-orphans
docker compose up --build
```
</details>

---

## Credenciais de teste

Criadas pela migration de seed (`V2__seed_usuarios.sql`). A senha é armazenada **apenas como
hash BCrypt** — a senha em texto puro não existe no banco nem no repositório.

| Papel | E-mail | Senha |
|---|---|---|
| **ADMIN** | `ana.souza@fadex.org.br` | `suporte123` |
| **SOLICITANTE** | `joao.pereira@fadex.org.br` | `suporte123` |

A tela de login traz as duas contas em um atalho, para agilizar a avaliação.

<details>
<summary>Demais usuários do seed (mesma senha)</summary>

**ADMIN:** `maria.lima@`, `rafael.melo@`, `camila.reis@`
**SOLICITANTE:** `beatriz.rocha@`, `carlos.dias@`, `lucas.prado@`, `fernanda.alves@`

</details>

O banco já sobe com **12 chamados de demonstração**, cobrindo todos os status, todas as
prioridades e as duas origens de classificação — o painel abre populado.

---

## Tecnologias

| Camada | Stack | Por quê |
|---|---|---|
| Backend | **Java 21 + Spring Boot 3.5** | Stack pedida; recursos de linguagem (records, switch expressions) deixam o domínio mais enxuto |
| Persistência | **MySQL 8 + Spring Data JPA** | Banco relacional aceito pelo desafio |
| Migrations | **Flyway** | Schema versionado; nenhuma tabela criada à mão |
| Segurança | **Spring Security + JWT (jjwt)** | API stateless, sem sessão no servidor |
| Documentação | **springdoc-openapi (Swagger UI)** | API navegável e testável pelo navegador |
| Tempo real | **Server-Sent Events** | Fluxo unidirecional servidor → painel, que é exatamente o caso de uso |
| Frontend | **React 19 + Vite + TypeScript** | Tipagem casa o contrato com os DTOs do backend |
| Testes | **JUnit 5 + MockMvc + H2** | `mvn test` roda sem Docker e sem MySQL instalado |

---

## Testando a API

### Swagger UI

<http://localhost/swagger-ui.html> — autentique em `POST /api/auth/login`, copie o `token`,
clique em **Authorize**, cole, e todos os endpoints ficam testáveis pelo navegador.

### Coleção Postman

[`docs/central-chamados.postman_collection.json`](docs/central-chamados.postman_collection.json) —
importe no Postman e rode **Autenticação → Login (ADMIN)**: o token é salvo automaticamente e
as demais requisições já o utilizam.

### curl

<details open>
<summary><strong>Fluxo completo em 7 comandos</strong></summary>

```bash
# 1. Login como SOLICITANTE (guarda o token)
TOKEN=$(curl -s -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao.pereira@fadex.org.br","senha":"suporte123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 2. Abrir um chamado — sem informar categoria nem prioridade
curl -s -X POST http://localhost/api/chamados \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"titulo":"Servidor de arquivos inacessível",
       "descricao":"O compartilhamento de rede não abre em nenhuma máquina do setor e ninguém consegue trabalhar."}'
# → categoria REDE, prioridade ALTA, origem IA, com a justificativa da decisão

# 3. Listar com filtros
curl -s "http://localhost/api/chamados?status=ABERTO&prioridade=ALTA" \
  -H "Authorization: Bearer $TOKEN"

# 4. Indicadores
curl -s http://localhost/api/dashboard/metricas -H "Authorization: Bearer $TOKEN"

# 5. Acompanhar eventos em tempo real (deixe rodando e abra um chamado ALTA em outro terminal)
ADMIN=$(curl -s -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.souza@fadex.org.br","senha":"suporte123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)
curl -N "http://localhost/api/eventos/stream?token=$ADMIN"

# 6. ADMIN corrige a classificação da IA
curl -s -X PATCH http://localhost/api/chamados/1/triagem \
  -H "Authorization: Bearer $ADMIN" -H "Content-Type: application/json" \
  -d '{"categoria":"REDE","prioridade":"MEDIA"}'

# 7. Detecção de duplicados — relate o mesmo incidente com outras palavras
curl -s -X POST http://localhost/api/chamados \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"titulo":"Compartilhamento de rede fora do ar",
       "descricao":"O servidor de arquivos não abre em nenhuma máquina do setor e ninguém consegue trabalhar."}'
# → possiveisDuplicados aponta o chamado do passo 2, com a similaridade em
#   percentual e os termos que aproximaram os dois textos
```

</details>

### Endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/auth/registrar` | Cadastro (sempre SOLICITANTE) | público |
| `POST` | `/api/auth/login` | Autentica e emite JWT | público |
| `GET` | `/api/auth/eu` | Usuário da sessão | autenticado |
| `POST` | `/api/chamados` | Abre chamado **com triagem automática** | autenticado |
| `GET` | `/api/chamados` | Lista com filtros e paginação | autenticado¹ |
| `GET` | `/api/chamados/{id}` | Detalhe + triagem + histórico + comentários + duplicados | autenticado¹ |
| `GET` | `/api/chamados/{id}/similares` | Chamados que possivelmente são o mesmo incidente | autenticado¹ |
| `PUT` | `/api/chamados/{id}` | Edita título e descrição | autenticado¹ |
| `DELETE` | `/api/chamados/{id}` | Cancela (lógico) | autenticado¹ |
| `PATCH` | `/api/chamados/{id}/status` | Avança o status | **ADMIN** |
| `PATCH` | `/api/chamados/{id}/responsavel` | Atribui / reatribui | **ADMIN** |
| `POST` | `/api/chamados/{id}/triagem/aceitar` | Aceita a sugestão da IA | **ADMIN** |
| `PATCH` | `/api/chamados/{id}/triagem` | Corrige a classificação | **ADMIN** |
| `POST` | `/api/chamados/{id}/comentarios` | Adiciona comentário | autenticado¹ |
| `GET` | `/api/chamados/{id}/comentarios` | Lista em ordem cronológica | autenticado¹ |
| `GET` | `/api/dashboard/metricas` | Indicadores | autenticado² |
| `GET` | `/api/eventos/stream` | Fluxo SSE | autenticado² |
| `GET` | `/api/categorias` · `/api/prioridades` · `/api/status` | Listas de referência | autenticado |
| `GET` | `/api/responsaveis` | Possíveis responsáveis | **ADMIN** |

¹ SOLICITANTE só acessa os próprios chamados (403 caso contrário).
² O escopo acompanha o papel: ADMIN vê a operação toda, SOLICITANTE só o que é seu.

---

## Triagem por IA: como funciona e por quê

Ao abrir um chamado, o texto passa por um `TriageProvider`, que devolve **categoria,
prioridade, confiança e uma justificativa legível**. A justificativa aparece na interface —
o usuário entende *por que* o chamado foi classificado daquele jeito.

### Por que uma heurística é o provider padrão

O desafio admite explicitamente heurística própria bem justificada, e afirma que o critério é
*"a solução funcionar e estar bem explicada, não a sofisticação do modelo"*. A heurística foi
escolhida como padrão porque:

- **não depende de rede nem de chave de API** — o avaliador reproduz o comportamento localmente,
  sem cadastro em serviço externo (vários exigem cartão de crédito);
- **é determinística** — a mesma entrada produz sempre a mesma saída, o que a torna
  genuinamente testável (23 testes cobrem categoria, prioridade, confiança e robustez);
- **é explicável** — devolve os termos que motivaram a decisão, em vez de um veredito opaco;
- **responde em microssegundos** — abrir um chamado não fica preso esperando terceiros.

### Como classifica

1. O texto é normalizado (minúsculas, sem acentuação) — "não" e "nao" viram o mesmo termo.
2. Cada categoria acumula pontos pelos termos do seu léxico encontrados. **Ocorrência no título
   pesa 3, na descrição pesa 1**, porque o título tende a nomear o problema.
3. A prioridade vem de três famílias de sinais — *bloqueio total*, *degradação* e *rotina* — e
   termos de amplitude ("setor inteiro", "ninguém consegue") **elevam um nível**, já que o mesmo
   defeito é mais grave quando atinge muita gente.
4. A confiança decorre da pontuação e da distância para a segunda colocada: vitória folgada gera
   confiança alta, empate técnico gera confiança baixa.

> A busca é por **limite de palavra**, e não substring: com substring simples o termo `ip` casava
> dentro de "equ**ip**e" e classificava qualquer texto como Rede. Esse bug foi encontrado pelos
> testes e está registrado no histórico do Git.

### Trocando por uma IA de verdade

O domínio depende apenas da interface `TriageProvider` — nunca de um fornecedor concreto:

```java
public interface TriageProvider {
    TriageResult classificar(TriageRequest requisicao);
    String nome();
}
```

Há **duas implementações** no repositório: `HeuristicTriageProvider` e `GeminiTriageProvider`.
Alternar entre elas é mudança de variável de ambiente, sem tocar em nenhuma linha do domínio:

```bash
TRIAGE_PROVIDER=gemini
GEMINI_API_KEY=sua-chave
GEMINI_MODEL=gemini-3.6-flash
```

Se o provider externo falhar (timeout, cota, rede fora), o `TriageService` **cai automaticamente
na heurística local** — a abertura de um chamado nunca falha por indisponibilidade da IA.

> **O fallback é silencioso, por projeto.** Isso protege a abertura do chamado, mas significa que
> uma IA mal configurada não se anuncia: o chamado abre normalmente, classificado pela heurística.
> Para saber o que está realmente em uso, três lugares dizem a verdade — a linha
> `Triagem automática usando o provider '...'` no log de startup (qual provider foi **escolhido**),
> o campo `triagem.provedor` de cada chamado (quem **de fato** classificou) e o indicador
> **Motor da triagem** no dashboard, que agrega os dois no tempo. Quando o provider escolhido não
> aparece na contagem, houve falha e queda para a heurística; o motivo sai no `WARN` do
> `TriageService`.

### Automático ≠ IA

São duas perguntas diferentes, e o painel responde as duas separadamente:

| Campo | Valores | Responde |
|---|---|---|
| `porOrigem` | `IA` / `MANUAL` | foi classificado sozinho, ou um ADMIN corrigiu na mão? |
| `porProvedorTriagem` | `heuristic` / `gemini` / … | **qual mecanismo** produziu a sugestão? |

A distinção não é cosmética. `Chamado.aplicarTriagem()` grava origem `IA` em toda classificação
automática, **inclusive quando quem classificou foi a heurística local** — que não usa modelo
nenhum. Um painel que só lê `porOrigem` anuncia "93% classificados pela IA" mesmo que nenhuma
chamada a um modelo tenha acontecido. Por isso o texto do dashboard diz "classificados
automaticamente", e a contagem por mecanismo aparece ao lado:

```
93% dos chamados foram classificados automaticamente · 2 ajustados manualmente
Motor da triagem:  [Heurística local 23 (85%)]  [Gemini 4 (15%)]
```

**Duas armadilhas de configuração que valem o aviso:**

1. **`mvn spring-boot:run` não lê o `.env`.** Esse arquivo é convenção do Docker Compose, não do
   Spring. Rodando o backend direto no host, exporte as variáveis no ambiente do processo antes:
   ```powershell
   $env:TRIAGE_PROVIDER='gemini'; $env:GEMINI_API_KEY='sua-chave'; mvn spring-boot:run
   ```
   Via `docker compose up`, o `.env` é lido automaticamente e nada disso é necessário.

2. **Nomes de modelo expiram.** Um modelo aposentado devolve `404 NOT_FOUND` e a triagem cai na
   heurística. Pior: um modelo pode **aparecer na listagem** e ainda assim recusar a chamada
   (`"no longer available to new users"`), então listar não prova disponibilidade — só uma chamada
   real prova:
   ```bash
   curl -s -X POST "https://generativelanguage.googleapis.com/v1beta/models/SEU_MODELO:generateContent" \
     -H "x-goog-api-key: SUA_CHAVE" -H "Content-Type: application/json" \
     -d '{"contents":[{"parts":[{"text":"ok"}]}]}'
   ```

---

## Detecção de chamados duplicados

Diferencial previsto no desafio ("detecção de chamados duplicados/similares"). Quando um serviço
cai, a central recebe o mesmo incidente várias vezes em poucos minutos — e a equipe precisa
perceber isso **antes** de abrir três frentes de trabalho para a mesma causa.

Ao abrir um chamado, e no detalhe de qualquer chamado, a API devolve em `possiveisDuplicados` os
registros que provavelmente relatam o mesmo problema:

```json
"possiveisDuplicados": [
  {
    "id": 12,
    "titulo": "Impressora do 3º andar não imprime",
    "status": "EM_ANDAMENTO",
    "solicitante": { "nome": "João Pereira" },
    "similaridade": 78,
    "termosEmComum": ["impressora", "andar", "imprimir"]
  }
]
```

### Como a similaridade é calculada

1. título e descrição são normalizados (minúsculas, sem acentuação) e quebrados em termos;
2. **stopwords** de português e o jargão de abertura de chamado ("bom dia", "solicito", "por
   favor") são descartados — sem isso, dois chamados sem nada em comum ficariam próximos apenas
   por começarem igual;
3. cada termo recebe um peso: **título vale 3, descrição vale 1** — a mesma ponderação da triagem,
   pela mesma razão (o título nomeia o incidente, a descrição o contextualiza);
4. a similaridade é o **cosseno** entre os dois vetores de termos, de 0 a 1. Acima de **0,35** o
   chamado é apresentado como possível duplicata, no máximo 5 por consulta.

O cosseno foi preferido ao índice de Jaccard porque normaliza pelo tamanho: um relato de uma linha
e outro de um parágrafo sobre o mesmo problema continuam próximos, em vez de serem penalizados
pela diferença de extensão.

### Por que não embeddings

Embeddings dariam mais alcance em paráfrases, ao custo de uma dependência externa em um caminho
que roda **a cada abertura de chamado**. A escolha segue a mesma lógica da triagem heurística:
determinístico, sem rede, sem chave de API, testável de verdade — e suficiente para o caso que
realmente importa aqui, que é o mesmo incidente relatado por várias pessoas com praticamente as
mesmas palavras. Trocar por embeddings é substituir `SimilaridadeTextual`, sem tocar no domínio.

### Recorte de visibilidade

A sugestão respeita **exatamente a mesma autorização da listagem**: o ADMIN compara com todos os
chamados, o SOLICITANTE apenas com os próprios. Fosse diferente, a detecção viraria um canal
lateral para ler o título e o autor de chamados de terceiros — a funcionalidade não pode furar a
autorização que o resto do sistema aplica. Há teste automatizado para isso.

Chamados **cancelados** ficam de fora (não há trabalho a consolidar com eles) e a busca é limitada
aos últimos **60 dias**: um chamado de três meses atrás não é duplicata, é histórico.

---

## Fluxo do chamado e quadro Kanban

```
ABERTO  ⇄  EM_ANDAMENTO  ⇄  RESOLVIDO  →  FECHADO
   └───────────┴──────────────┴────────────┘
              encerramento direto
```

O chamado anda **uma etapa por vez, nos dois sentidos**. O retorno existe porque "resolvido" é
uma afirmação que pode se provar falsa: quando o atendimento não resolveu o que foi pedido, o
chamado volta para `EM_ANDAMENTO` em vez de ser fechado ou reaberto como duplicata.

Fora da sequência há **uma única saída**: qualquer chamado não encerrado pode ir direto para
`FECHADO`. É a válvula de escape da operação — chamado aberto por engano, duplicata já tratada em
outro registro, solicitação que perdeu o objeto. Obrigar esses casos a percorrer "em andamento" e
"resolvido" só produziria histórico falso.

Duas fronteiras continuam fechadas, e é o que impede tudo isso de virar um "desfazer" geral:

- **de `FECHADO` não há volta** — seria exatamente a reabertura que a regra proíbe (`409`);
- **não se pula etapa no meio do fluxo** — `ABERTO → RESOLVIDO` e `RESOLVIDO → ABERTO` são
  recusados igualmente. Fechar é a exceção, e é de ida.

### Quem move o quê

`RESOLVIDO` não é sinônimo de `FECHADO`: é o estado que **aguarda o solicitante**. É ele quem diz
se o problema acabou, e por isso tem duas ações — e só essas duas, e só no próprio chamado:

| Papel | Pode |
|---|---|
| **ADMIN** | todo o fluxo: avançar, retornar e encerrar direto de qualquer etapa |
| **SOLICITANTE** | no **próprio** chamado em `RESOLVIDO`: confirmar a resolução (`FECHADO`) ou informar que o problema continua (`EM_ANDAMENTO`) |

Qualquer outra combinação devolve `403`. A autorização é **por transição, não por papel puro**, e
mora no `ChamadoService` — não no controller: um SOLICITANTE que chame a API direto tentando mover
um chamado de `ABERTO` recebe 403 igual.

> Isso também fecha uma armadilha silenciosa: a regra "quem move para EM_ANDAMENTO sem responsável
> assume o chamado" só vale para o ADMIN. Sem essa ressalva, o solicitante que reabre o atendimento
> viraria responsável por ele — e responsável é sempre alguém da equipe de suporte.

### O log de cada chamado

Toda movimentação vira um evento em `evento_historico`, com **autor, tipo, descrição e instante**.
É esse registro que o detalhe do chamado exibe como timeline, e as duas direções são eventos
distintos — um retorno não se confunde com mais um avanço na leitura do histórico:

| Movimento | Tipo do evento | Etiqueta | Descrição registrada |
|---|---|---|---|
| avanço | `STATUS_ALTERADO` | — | `Ana Souza moveu o chamado para Resolvido` |
| retorno | `STATUS_RETROCEDIDO` | `RETORNO` | `Ana Souza retornou o chamado de Resolvido para Em andamento` |
| encerramento direto | `STATUS_ALTERADO` | `ENCERRAMENTO DIRETO` | `Ana Souza encerrou o chamado direto de Aberto` |
| solicitante confirma | `STATUS_ALTERADO` | `CONFIRMADO` | `João Pereira confirmou a resolução e encerrou o chamado` |
| solicitante contesta | `STATUS_RETROCEDIDO` | `RETORNO` | `João Pereira informou que o problema continua; o atendimento foi reaberto` |

O nome de quem moveu entra na descrição **e** no campo `autor` do evento: quem audita não depende
do texto para saber quem fez a mudança. As etiquetas existem para que os casos que merecem atenção
— um retorno, um encerramento fora do fluxo — saltem da timeline em vez de se diluírem entre os
avanços de rotina.

### O quadro

O dashboard abre um **Kanban em modal** (botão *Kanban*, no topo à direita) com uma coluna por
etapa. Arrastar um cartão chama o mesmo `PATCH /api/chamados/{id}/status` da tela de detalhe — o
quadro opera o fluxo, não tem regras próprias. Durante o arraste, só as colunas vizinhas ficam
ativas: a regra do domínio aparece na interface em vez de ser descoberta por `409`. Cada cartão
também tem os botões `←` e `→`, porque drag-and-drop nativo não funciona em toque nem por teclado.

Três detalhes de comportamento:

- **O solicitante também vê o quadro**, com os próprios chamados e sem poder arrastar. O recorte
  não é da tela: vem da mesma listagem que já filtra por solicitante no backend.
- **Encerrar nunca acontece por arraste.** A coluna `FECHADO` não aceita cartão de qualquer lugar;
  o encerramento direto é um botão no cartão, com diálogo de confirmação. Fechar não tem volta, e
  um solte acidental é fácil demais.
- **Alternador "Sob minha responsabilidade"** para a equipe, que transforma o quadro na fila de
  quem atende. É filtro de visualização, não papel novo — o "operador" do dia a dia é o ADMIN
  designado como `responsavel` do chamado.

---

## Tempo real

**Server-Sent Events** (`GET /api/eventos/stream`). O tráfego é unidirecional (servidor → painel),
que é exatamente o caso de uso: WebSocket exigiria broker STOMP e cliente adicional para o mesmo
resultado, e polling curto desperdiçaria requisições.

| Evento | Quando | Quem recebe |
|---|---|---|
| `conectado` | logo após conectar, já com os indicadores | quem conectou |
| `metricas` | a cada alteração de chamado | ADMIN (global) e o solicitante (próprios) |
| `chamado-criado` | abertura de chamado | ADMIN e o solicitante |
| `chamado-atualizado` | status, responsável, classificação, comentário | ADMIN e o solicitante |
| `alerta-alta` | **abertura de chamado ALTA** | somente ADMIN |

Dois cuidados que valem menção:

- **A entrega respeita a autorização.** Cada inscrição guarda o papel do usuário; um SOLICITANTE
  recebe eventos apenas dos próprios chamados. Tempo real não pode virar atalho para contornar
  o controle de acesso.
- **Os eventos são publicados após o commit** (`@TransactionalEventListener(AFTER_COMMIT)`).
  Notificar durante a transação faria o painel exibir um chamado que ainda pode sofrer rollback.

> O `EventSource` do navegador não envia cabeçalhos personalizados, então **este endpoint — e
> apenas ele** — também aceita o token por query param, validado pelo mesmo `JwtService`.
> É um trade-off consciente; nenhuma outra rota lê token da query string.

---

## Arquitetura

```
backend/src/main/java/br/org/fadex/chamados/
├── config/       SecurityConfig · OpenApiConfig · propriedades (JWT, CORS, triagem)
├── domain/       Usuario · Chamado · Comentario · EventoHistorico + enums
├── repository/   Spring Data JPA + Specifications de filtro
├── service/      AuthService · ChamadoService · ComentarioService
│                 HistoricoService · MetricasService
├── security/     JwtService · JwtAuthenticationFilter · UsuarioDetailsService
├── triage/       TriageProvider ← HeuristicTriageProvider · GeminiTriageProvider
├── duplicados/   SimilaridadeTextual · DetectorDuplicados
├── texto/        normalização e tokenização compartilhadas pela triagem e pelos duplicados
├── realtime/     SseEmitterRegistry · EventoRealtimePublisher
├── web/          Controllers + DTOs
└── exception/    GlobalExceptionHandler · ApiError

frontend/src/
├── api/          client (JWT, erros) · servicos · tipos     ← única camada que fala com a rede
├── auth/         AuthContext
├── realtime/     RealtimeContext (SSE)
├── components/   design system (badges, estados, toasts, modal, quadro Kanban)
├── layout/       AppShell · CentralNotificacoes · AlertaAltaPrioridade
└── pages/        Login · Dashboard · ListaChamados · NovoChamado · DetalheChamado
```

O fluxo é `Controller → Service → Repository → Banco`. **Nenhum componente React chama `fetch`
diretamente**: tudo passa por `src/api/`, então trocar a rota de um endpoint é uma alteração local.

---

## Modelo de dados

```
usuario ──1:N──► chamado ──1:N──► comentario
   ▲                 │
   └──── responsável │──1:N──► evento_historico
```

O `chamado` guarda a classificação em **dois conjuntos de campos**, e essa separação é o que
torna o requisito de revisão possível:

| Sugestão da IA (imutável) | Classificação final (mutável) |
|---|---|
| `categoria_sugerida` | `categoria` |
| `prioridade_sugerida` | `prioridade` |
| `confianca_ia`, `justificativa_ia`, `provedor_triagem` | `origem_classificacao`, `classificacao_revisada` |

Assim a interface exibe **"Sugestão da IA" e "Classificação final" lado a lado**, e fica auditável
quando o suporte discordou da IA.

---

## Regras de negócio

Todas aplicadas **no backend**, não apenas escondendo botões na interface:

| Regra | Resposta |
|---|---|
| Título e descrição obrigatórios | `400` com os campos inválidos |
| E-mail único (verificação + constraint no banco) | `409` |
| **Chamado FECHADO não pode ser reaberto** | `409` |
| Fluxo sequencial ABERTO ↔ EM_ANDAMENTO ↔ RESOLVIDO → FECHADO | `409` ao pular etapas, em qualquer direção |
| Encerramento direto para FECHADO a partir de qualquer etapa não encerrada | permitido; de FECHADO segue `409` |
| SOLICITANTE não acessa chamado de terceiro | `403` |
| Só ADMIN conduz o fluxo, atribui responsável e revisa a IA | `403` |
| SOLICITANTE só confirma ou contesta o **próprio** chamado em RESOLVIDO | `403` em qualquer outra transição |
| Responsável precisa ser da equipe de suporte | `409` |
| Requisição sem token válido | `401` |

Toda resposta de erro usa o mesmo formato `ApiError`
(`timestamp`, `status`, `erro`, `mensagem`, `caminho`, `campos[]`).

---

## Testes automatizados

```bash
cd backend && mvn test
```

**121 testes**, rodando em **H2 no modo MySQL** — sem Docker e sem MySQL instalado. A suíte executa
as **mesmas migrations Flyway** do ambiente real, com `ddl-auto=validate`: qualquer divergência
entre uma coluna e o campo JPA correspondente derruba o build.

| Suíte | Cobre |
|---|---|
| `AuthControllerTest` | cadastro, hash BCrypt, login, e-mail duplicado, token inválido/forjado, 401 |
| `ChamadoControllerTest` | CRUD, filtros, paginação, permissões, fluxo de status nos dois sentidos, reabertura, cancelamento, revisão da IA, comentários, detecção de duplicados |
| `HeuristicTriageProviderTest` | categoria, prioridade, incidente de segurança, confiança, determinismo, acentuação, texto vazio |
| `MetricasETempoRealTest` | indicadores, escopo por papel, publicação de eventos, alerta ALTA, autenticação do SSE |
| `StatusChamadoTest` | avanço e retorno de etapa, salto proibido, estados terminais |
| `SimilaridadeTextualTest` | pontuação, simetria, normalização, stopwords, termos em comum |
| `SeedDemonstracaoTest` | valida a carga de demonstração em banco isolado |

---

## Decisões de projeto

**Cancelamento é lógico.** `DELETE /api/chamados/{id}` move o chamado para `CANCELADO` em vez de
apagá-lo. O desafio pede "excluir/**cancelar**", e preservar o histórico de um chamado cancelado
é mais útil para uma central de suporte do que remover a linha.

**Categoria é enum, exposto pela API.** O documento não fixa uma lista de categorias. Um enum
servido por `GET /api/categorias` deixa a interface consumindo a lista da API — sem criar um CRUD
de categorias que ampliaria o escopo sem necessidade.

**Enums em ASCII na API.** Trafega `MEDIA`; o rótulo acentuado (`MÉDIA`) vai junto para exibição.
Evita problemas de encoding entre banco, JSON e navegador.

**Dados de demonstração ficam fora dos testes.** Os 12 chamados de exemplo vivem em
`db/demo`, uma *location* Flyway separada que o perfil de teste não carrega — a suíte roda sobre
uma base limpa, sem depender de dados de vitrine.

**O papel vem sempre do banco.** O JWT carrega o papel por conveniência do cliente, mas a
autorização recarrega o usuário a cada requisição: alterar o claim no cliente não concede nada.

---

## Segurança

- Senhas apenas como **hash BCrypt**; nenhum endpoint devolve o hash.
- **Cadastro público sempre cria SOLICITANTE** — aceitar o papel no corpo da requisição permitiria
  autopromoção a ADMIN. Há teste cobrindo essa tentativa.
- API **stateless**, sem sessão no servidor.
- CORS restrito às origens configuradas, não liberado para qualquer origem.
- **Nenhum segredo versionado**: `.env` está no `.gitignore`; o `.env.example` traz só as chaves.
  O `JWT_SECRET` tem um padrão explicitamente marcado como de desenvolvimento e o serviço
  **recusa** segredo com menos de 32 bytes.
- O container do backend roda como usuário não-root.

---

## Documentos do desafio

- [Enunciado oficial (PDF)](docs/Desafio_Analista_Desenvolvimento_Fadex.pdf)
- [Coleção Postman](docs/central-chamados.postman_collection.json)
