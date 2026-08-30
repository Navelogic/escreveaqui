# 🏗️ Arquitetura — Escreve Aqui

Este documento descreve a arquitetura técnica do projeto, os fluxos de dados e as decisões de design.

---

## Visão Geral

O Escreve Aqui é uma aplicação web monolítica com frontend e backend separados, comunicando-se via REST.

```
┌─────────────────────────────────┐
│           Usuário               │
│      (qualquer navegador)       │
└────────────────┬────────────────┘
                 │ HTTPS
┌────────────────▼────────────────┐
│     Frontend (React + Vite)     │
│  Tailwind CSS  •  Shadcn/UI     │
│       http://localhost:5173     │
└────────────────┬────────────────┘
                 │ HTTP (axios)
┌────────────────▼────────────────┐
│    Backend (Spring Boot 4)      │
│          Java 21 LTS            │
│       http://localhost:8080     │
│                                 │
│  ┌──────────┐  ┌─────────────┐  │
│  │ Caffeine │  │  HikariCP   │  │
│  │  Cache   │  │ Pool (≤20)  │  │
│  └────┬─────┘  └──────┬──────┘  │
└───────│────────────────│────────┘
        │                │ JDBC
        │         ┌──────▼──────┐
        └─────────►  PostgreSQL │
                  └─────────────┘
```

---

## Frontend

### Estrutura de pastas

```
src/
├── components/
│   ├── ui/               # Componentes Shadcn/UI (Button, Input, Dialog, Textarea)
│   ├── Contributors.tsx  # Lista de contribuidores via GitHub API
│   └── Modal.tsx         # Wrapper sobre Dialog do Shadcn
├── pages/
│   ├── Home/             # Página inicial — criação de notas
│   └── Editor/           # Editor de texto full-screen
├── services/
│   └── notaService.ts    # Client HTTP (axios) para a API
├── interface/            # Tipos TypeScript (Nota, NotaRequest)
└── lib/
    └── utils.ts          # Helper cn() para Tailwind
```

### Roteamento

| Rota | Componente | Descrição |
|---|---|---|
| `/` | `Home` | Página inicial — criar ou acessar uma nota |
| `/:key` | `Editor` | Editor da nota identificada pelo slug `key` |

### Fluxo de auto-save e sincronização

```
Usuário digita
      │
      ▼
setText(newText)        ← atualiza estado local imediatamente
setIsTyping(true)        ← também espelhado em isTypingRef (ver abaixo)
      │
      ▼
debounce(1000ms)        ← aguarda 1s de inatividade
      │
      ▼
PUT /api/v1/notes/:slug ← salva no backend e dispara evento SSE (ver Tempo real)
      │
      ▼
setTimeout(2000ms)      ← após 2s sem digitar, isTyping = false
```

A busca inicial do conteúdo (`GET /:slug`) acontece uma única vez, ao montar o
editor ou trocar de slug — não existe mais polling periódico. Atualizações de
outras sessões chegam via SSE (ver [Tempo real (SSE)](#tempo-real-sse)).

`isTyping` é replicado num `useRef` (`isTypingRef`) lido pelo listener de SSE,
para que o efeito da conexão SSE dependa só de `key` e não seja recriado a cada
tecla digitada — diferente do polling antigo, que recriava o `setInterval` e o
`AbortController` a cada keystroke por depender de `text` no array de
dependências.

### Cursor brasileiro

O editor usa `caret-color` nativo do browser com ciclo entre as cores da bandeira:

```
Verde (#009c3b) → Amarelo (#ffdf00) → Azul (#002776) → Verde...
```

O ciclo ocorre a cada 800ms via `setInterval` em React, usando `style={{ caretColor }}` na textarea. Isso funciona em todos os dispositivos, incluindo mobile.

---

## Backend

### Estrutura de pacotes

```
br.com.escreveaqui.backend/
├── BackendApplication.java     # Ponto de entrada (@EnableScheduling, @EnableCaching)
├── configs/
│   └── CorsConfig.java         # CORS via variável ALLOWED_ORIGINS
├── controllers/
│   └── NotaController.java     # Endpoints REST (/api/v1/notes)
├── services/
│   ├── ReadNotaService.java    # Leitura com @Cacheable
│   ├── UpsertNotaService.java  # Escrita com @CacheEvict, notifica SseService
│   ├── SseService.java         # Gerencia conexões SSE e notifica atualizações
│   └── DeleteNotaService.java  # Limpeza agendada com @CacheEvict
├── repositories/
│   └── NotaRepository.java     # JdbcTemplate — SQL Postgres direto (findBySlug, upsert, deleteOldNotes)
├── models/
│   └── Nota.java               # Record simples (id, slug, content, createdAt, updatedAt)
├── dtos/
│   ├── NotaRequestDTO.java     # Record — entrada (content)
│   └── NotaResponseDTO.java    # Record — saída (slug, content, updatedAt)
└── handlers/
    └── GlobalExceptionHandler.java  # @RestControllerAdvice — RFC 9457
```

### Fluxo de leitura (GET)

```
GET /api/v1/notes/{slug}
         │
         ▼
  Validação do slug
  (regex @Pattern)
         │
         ▼
  Cache Caffeine
  ┌──────────────────┐
  │ HIT?             │──► Retorna 200 com dado em memória (sem I/O)
  │ MISS?            │──► findBySlug() no PostgreSQL
  └──────────────────┘        │
                              ▼
                     Armazena no cache (TTL 30s)
                              │
                              ▼
                       Retorna 200 OK
```

### Fluxo de escrita (PUT)

```
PUT /api/v1/notes/{slug}
         │
         ▼
  Validação do slug + body
  (@Pattern + @Valid @Size)
         │
         ▼
  makeSlug() — normalização
  (NFD, lowercase, remove acentos)
         │
         ▼
  INSERT ... ON CONFLICT (slug) DO UPDATE
  RETURNING (xmax = 0) AS inserted
  (upsert atômico — uma única ida ao banco,
   sem SELECT prévio)
         │
         ▼
  @CacheEvict — invalida cache do slug
         │
         ▼
  SseService.notify(slug, content) — envia o novo conteúdo
  para os clientes inscritos no stream desse slug
         │
         ▼
  Retorna 204 No Content
```

O upsert é uma única query (`NotaRepository.upsert`, via `JdbcTemplate`), em vez do
SELECT seguido de INSERT/UPDATE que o JPA fazia — isso elimina a janela de corrida
entre leitura e escrita, então não há mais necessidade de optimistic locking (ver
[Concorrência](#concorrência)).

### Tempo real (SSE)

```
GET /api/v1/notes/{slug}/stream
         │
         ▼
  Validação do slug (regex @Pattern, mesmo SlugUtils.SLUG_REGEX do GET/PUT)
         │
         ▼
  SseService.subscribe(slug)
         │
         ▼
  Cria SseEmitter(timeout=0L) — sem expiração pelo servidor
         │
         ▼
  Adiciona à lista de emitters do slug
  (Map<String, List<SseEmitter>> em memória, thread-safe)
         │
         ▼
  Retorna a conexão HTTP aberta (text/event-stream)
```

Quando outra sessão salva a nota (`PUT`), `UpsertNotaService` chama
`SseService.notify(slug, content)` logo após o upsert e o `@CacheEvict`,
transmitindo o conteúdo recém-salvo (não relido do banco) como um evento
`nota-update` para cada `SseEmitter` inscrito naquele slug.

Cada emitter registra callbacks `onCompletion`, `onTimeout` e `onError` que o
removem da lista ao desconectar, evitando vazamento de memória. Se o envio a
um emitter falhar (cliente já desconectado), ele também é removido na hora.

No frontend, o editor abre uma única conexão `EventSource` por slug (via
`useEffect` com `[key]` como dependência) e aplica o conteúdo recebido apenas
se o usuário não estiver digitando no momento (`isTypingRef.current`). Isso
substituiu o polling de 2 em 2 segundos que existia antes.

**Limitações conhecidas:**

- O estado dos emitters é local ao processo (`ConcurrentHashMap` em memória).
  Se o backend rodar em múltiplas instâncias sem um load balancer com sticky
  sessions, uma escrita atendida pela instância A não chega aos clientes
  conectados à instância B. Escalar isso exigiria um mecanismo de fan-out
  entre instâncias (pub/sub via Redis, por exemplo — mesma lógica já discutida
  em [Por que Caffeine e não Redis?](#por-que-caffeine-e-não-redis)).
- Não há replay de eventos perdidos: o `EventSource` do navegador reconecta
  sozinho após queda de conexão, mas eventos disparados durante a
  desconexão não são reenviados nem historizados no servidor.

### Limpeza automática

```
Todo dia às 03:00 UTC
         │
         ▼
  DeleteNotaService.execute()
         │
         ▼
  DELETE FROM notes WHERE updated_at < (now - 30 dias)
         │
         ▼
  @CacheEvict(allEntries = true) — limpa todo o cache
         │
         ▼
  log.info("X nota(s) removida(s)")
```

---

## Banco de Dados

### Tabela `notes`

| Coluna | Tipo | Restrição | Descrição |
|---|---|---|---|
| `id` | UUID | PK, gerado | Identificador interno |
| `slug` | VARCHAR(255) | NOT NULL, UNIQUE | Identificador público da nota |
| `content` | TEXT | nullable | Conteúdo da nota |
| `created_at` | TIMESTAMPTZ | NOT NULL | Data de criação (imutável) |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Data da última modificação |

**Índice:** `idx_notes_slug` (único) em `slug` — garante lookup O(log n) no caminho
crítico e é o alvo do `ON CONFLICT` no upsert. `idx_notes_updated_at` acelera a
limpeza de notas antigas.

**Schema versionado via Flyway** — migrations em
`backend/src/main/resources/db/migration/`, aplicadas automaticamente na
inicialização (substituiu o `ddl-auto` do Hibernate).

---

## Cache

| Propriedade | Valor | Justificativa |
|---|---|---|
| Implementação | Caffeine | In-process, zero latência de rede |
| TTL | 30s | Curto para garantir consistência em edição colaborativa |
| Capacidade máxima | 500 entradas | Baixo footprint de memória |
| Invalidação | Por chave no PUT, total no cleanup | Dado fresco imediatamente após salvamento |

> O TTL de 30s existe como rede de segurança. Na prática, o `@CacheEvict` no `PUT` invalida o cache da nota 
> imediatamente após cada salvamento. A sincronização entre sessões abertas na mesma nota não depende desse cache: 
> é feita via SSE (ver [Tempo real (SSE)](#tempo-real-sse)), não por releitura periódica.

---

## Concorrência

A escrita usa `INSERT ... ON CONFLICT (slug) DO UPDATE` — uma única instrução
atômica no Postgres. Não existe mais a janela entre "ler a nota" e "salvar a
nota" que exigiria optimistic locking: duas escritas concorrentes no mesmo slug
serializam no próprio `ON CONFLICT`, e a que for aplicada por último vence
(last-write-wins), sem lançar erro. Não há mais handler de `409 Conflict` por
conflito de edição — nesse fluxo, não há mais o que conflitar.

O `GlobalExceptionHandler` ainda trata `DataIntegrityViolationException` com
`409`, mas isso é apenas uma rede de segurança genérica, a única constraint de
unicidade da tabela (`slug`) já é resolvida pelo `ON CONFLICT` antes de chegar
a violar o banco, então esse handler não é mais exercitado pelo fluxo normal
de escrita.

---

## Decisões de Design

### Por que sem autenticação?

O projeto é intencionalmente público e anônimo. A URL é a "senha" — quem tem o link pode ler e editar. Isso elimina qualquer barreira de entrada e está alinhado com a filosofia de simplicidade.

### Por que Caffeine e não Redis?

Para o volume atual do projeto, um cache in-process é suficiente e não adiciona complexidade operacional (sem servidor adicional, sem rede). Se o projeto escalar para múltiplas instâncias do backend, Redis seria a evolução natural.

### Por que `PUT` para upsert?

O `PUT` é semanticamente correto para operações idempotentes: chamar `PUT` várias vezes com o mesmo corpo produz o mesmo resultado. O endpoint cria a nota se ela não existir, ou atualiza se existir — ambos os casos resultam no mesmo estado final.

### Por que notas são deletadas após 30 dias?

Para manter o banco de dados enxuto e evitar acúmulo de dados órfãos. Notas ativas são acessadas com frequência e têm o `updated_at` renovado. Notas abandonadas são limpas automaticamente.
