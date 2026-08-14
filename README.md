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

| Recurso | Endereço |
|---|---|
| Aplicação | <http://localhost> |
| API | <http://localhost:8080> |
| Swagger UI | <http://localhost/swagger-ui.html> |

> **Primeira execução:** o MySQL inicializa o data dir do zero, o que pode levar **2 a 4 minutos**
> (bem mais em Docker sobre WSL2). O `healthcheck` do compose segura o backend até o banco
> realmente aceitar conexões — é só aguardar. As execuções seguintes sobem em segundos.

> A porta do MySQL **não** é publicada no host de propósito: muitas máquinas já têm um MySQL
> na 3306, e publicá-la faria o `docker compose up` falhar nesses casos. Para inspecionar o
> banco por fora, descomente o bloco `ports` do serviço `mysql` no `docker-compose.yml`.

### Opção 2 — Execução local

**Pré-requisitos:** JDK 21, Maven 3.9+, Node 20+, e um MySQL 8 acessível.

```bash
# 1. Banco (ou use uma instância MySQL já existente)
docker compose up -d mysql

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

### Variáveis de ambiente

Todas têm padrão de desenvolvimento e o projeto sobe sem configuração. Para personalizar:

```bash
cp .env.example .env
```

O `.env` está no `.gitignore`; o `.env.example` lista apenas as chaves esperadas, sem valores
secretos reais.

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
<summary><strong>Fluxo completo em 6 comandos</strong></summary>

```bash
# 1. Login como SOLICITANTE (guarda o token)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao.pereira@fadex.org.br","senha":"suporte123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 2. Abrir um chamado — sem informar categoria nem prioridade
curl -s -X POST http://localhost:8080/api/chamados \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"titulo":"Servidor de arquivos inacessível",
       "descricao":"O compartilhamento de rede não abre em nenhuma máquina do setor e ninguém consegue trabalhar."}'
# → categoria REDE, prioridade ALTA, origem IA, com a justificativa da decisão

# 3. Listar com filtros
curl -s "http://localhost:8080/api/chamados?status=ABERTO&prioridade=ALTA" \
  -H "Authorization: Bearer $TOKEN"

# 4. Indicadores
curl -s http://localhost:8080/api/dashboard/metricas -H "Authorization: Bearer $TOKEN"

# 5. Acompanhar eventos em tempo real (deixe rodando e abra um chamado ALTA em outro terminal)
ADMIN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.souza@fadex.org.br","senha":"suporte123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)
curl -N "http://localhost:8080/api/eventos/stream?token=$ADMIN"

# 6. ADMIN corrige a classificação da IA
curl -s -X PATCH http://localhost:8080/api/chamados/1/triagem \
  -H "Authorization: Bearer $ADMIN" -H "Content-Type: application/json" \
  -d '{"categoria":"REDE","prioridade":"MEDIA"}'
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
| `GET` | `/api/chamados/{id}` | Detalhe + triagem + histórico + comentários | autenticado¹ |
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
```

Se o provider externo falhar (timeout, cota, rede fora), o `TriageService` **cai automaticamente
na heurística local** — a abertura de um chamado nunca falha por indisponibilidade da IA.

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
├── realtime/     SseEmitterRegistry · EventoRealtimePublisher
├── web/          Controllers + DTOs
└── exception/    GlobalExceptionHandler · ApiError

frontend/src/
├── api/          client (JWT, erros) · servicos · tipos     ← única camada que fala com a rede
├── auth/         AuthContext
├── realtime/     RealtimeContext (SSE)
├── components/   design system (badges, estados, toasts, modal)
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
| Fluxo sequencial ABERTO → EM_ANDAMENTO → RESOLVIDO → FECHADO | `409` ao pular etapas |
| SOLICITANTE não acessa chamado de terceiro | `403` |
| Só ADMIN altera status, atribui responsável e revisa a IA | `403` |
| Responsável precisa ser da equipe de suporte | `409` |
| Requisição sem token válido | `401` |

Toda resposta de erro usa o mesmo formato `ApiError`
(`timestamp`, `status`, `erro`, `mensagem`, `caminho`, `campos[]`).

---

## Testes automatizados

```bash
cd backend && mvn test
```

**92 testes**, rodando em **H2 no modo MySQL** — sem Docker e sem MySQL instalado. A suíte executa
as **mesmas migrations Flyway** do ambiente real, com `ddl-auto=validate`: qualquer divergência
entre uma coluna e o campo JPA correspondente derruba o build.

| Suíte | Cobre |
|---|---|
| `AuthControllerTest` | cadastro, hash BCrypt, login, e-mail duplicado, token inválido/forjado, 401 |
| `ChamadoControllerTest` | CRUD, filtros, paginação, permissões, fluxo de status, reabertura, cancelamento, revisão da IA, comentários |
| `HeuristicTriageProviderTest` | categoria, prioridade, confiança, determinismo, acentuação, texto vazio |
| `MetricasETempoRealTest` | indicadores, escopo por papel, publicação de eventos, alerta ALTA, autenticação do SSE |
| `StatusChamadoTest` | fluxo sequencial e estados terminais |
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
- [Especificação de implementação](docs/ESPECIFICACAO.md)
- [Prompt de UI/UX que originou o design](docs/PROMPT-UI-UX.md)
- [Coleção Postman](docs/central-chamados.postman_collection.json)
