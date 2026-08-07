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

### Fluxo de auto-save

```
Usuário digita
      │
      ▼
setText(newText)        ← atualiza estado local imediatamente
setIsTyping(true)
      │
      ▼
debounce(1000ms)        ← aguarda 1s de inatividade
      │
      ▼
PUT /api/v1/notes/:slug ← salva no backend
      │
      ▼
setTimeout(2000ms)      ← após 2s sem digitar, isTyping = false
      │
      ▼
polling ativo           ← GET a cada 2s para sincronizar com outros usuários
                           (pausa quando a aba fica em segundo plano;
                            busca imediata ao voltar o foco — Page Visibility API)
```

Cada GET envia `If-None-Match` com o ETag da última resposta (derivado de
`updatedAt`); se a nota não mudou, o backend responde `304 Not Modified` sem
corpo, economizando banda no ciclo de polling.

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
│   ├── UpsertNotaService.java  # Escrita com @CacheEvict
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
  Retorna 204 No Content
```

O upsert é uma única query (`NotaRepository.upsert`, via `JdbcTemplate`), em vez do
SELECT seguido de INSERT/UPDATE que o JPA fazia — isso elimina a janela de corrida
entre leitura e escrita, então não há mais necessidade de optimistic locking (ver
[Concorrência](#concorrência)).

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

> O TTL de 30s existe como rede de segurança. Na prática, o `@CacheEvict` no `PUT` invalida o cache da nota imediatamente após cada salvamento, então usuários que estão editando a mesma nota recebem dados atualizados a cada ciclo de polling (2s).

---

## Concorrência

A escrita usa `INSERT ... ON CONFLICT (slug) DO UPDATE` — uma única instrução
atômica no Postgres. Não existe mais a janela entre "ler a nota" e "salvar a
nota" que exigiria optimistic locking: duas escritas concorrentes no mesmo slug
serializam no próprio `ON CONFLICT`, e a que for aplicada por último vence
(last-write-wins), sem lançar erro. Não há mais handler de `409 Conflict` por
conflito de edição — nesse fluxo, não há mais o que conflitar.

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
