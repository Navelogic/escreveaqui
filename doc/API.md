# 🔌 Documentação da API — Escreve Aqui

Base URL: `http://localhost:8080` (desenvolvimento)

Todos os endpoints estão sob o prefixo `/api/v1/notes`.

---

## Formato do Slug

O slug é o identificador único de uma nota — ele aparece diretamente na URL.

**Regras de validação (entrada):**

- Caracteres permitidos: letras (a-z, A-Z), números (0-9), espaços, hífens (`-`) e underscores (`_`)
- O backend normaliza automaticamente o slug antes de salvar:
  - Converte para minúsculas
  - Remove acentos
  - Substitui espaços por hífens
  - Remove hífens duplicados e nas extremidades

**Exemplo:**

| Slug enviado | Slug salvo |
|---|---|
| `Minha Nota` | `minha-nota` |
| `résumé` | `resume` |
| `--test--` | `test` |

---

## Endpoints

### GET `/api/v1/notes/{slug}`

Retorna o conteúdo de uma nota pelo slug.

**Parâmetros de rota:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `slug` | string | sim | Identificador da nota |

**Resposta de sucesso — `200 OK`:**

```json
{
  "slug": "minha-nota",
  "content": "Conteúdo da nota aqui.",
  "updatedAt": "2025-04-18T14:30:00Z"
}
```

> **Nota:** se o slug não existir, o endpoint retorna `200 OK` com `content` vazio e `updatedAt` igual ao momento da requisição. Isso permite que o frontend trate "nota nova" e "nota existente" de forma transparente.

A resposta inclui os headers `ETag` (derivado de `updatedAt`) e
`Cache-Control: no-cache`. Se o cliente reenviar `If-None-Match` com esse ETag,
o backend responde `304 Not Modified` sem corpo. Como o frontend agora faz uma
única busca por slug em vez de polling periódico, isso beneficia principalmente
recarregamentos de página, não a sincronização em tempo real (feita via SSE).

**Resposta de slug inválido — `400 Bad Request`:**

```json
{
  "type": "about:blank",
  "title": "Requisição inválida",
  "status": 400,
  "detail": "read.slug: deve corresponder a \"^[A-Za-z0-9_\\s-]+$\""
}
```

---

### GET `/api/v1/notes/{slug}/stream`

Abre uma conexão [Server-Sent Events](https://developer.mozilla.org/docs/Web/API/Server-sent_events)
para receber atualizações em tempo real de uma nota, sem precisar de polling.

**Parâmetros de rota:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `slug` | string | sim | Identificador da nota |

**Resposta de sucesso — `200 OK`, `Content-Type: text/event-stream`:**

A conexão fica aberta indefinidamente (sem timeout no servidor). Cada evento
enviado tem o nome `nota-update` e o `data` é o conteúdo atualizado da nota em
texto puro (não é JSON):

```
event: nota-update
data: Conteúdo atualizado da nota.

```

Um evento é disparado para todos os clientes inscritos naquele slug sempre que
alguém salva a nota via `PUT`. Ver [Tempo real (SSE)](ARCHITECTURE.md#tempo-real-sse)
na arquitetura para detalhes de implementação e limitações conhecidas.

> **Nota:** este endpoint só transmite mudanças a partir do momento em que a
> conexão é aberta. Para carregar o conteúdo atual da nota, use o `GET /{slug}`
> normal antes de abrir o stream.

---

### PUT `/api/v1/notes/{slug}`

Cria ou atualiza uma nota. A operação é idempotente.

**Parâmetros de rota:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `slug` | string | sim | Identificador da nota |

**Corpo da requisição (`application/json`):**

```json
{
  "content": "Conteúdo da nota."
}
```

| Campo | Tipo | Obrigatório | Limite |
|---|---|---|---|
| `content` | string | não | máx. 1.000.000 caracteres (~1 MB) |

**Resposta de sucesso — `204 No Content`**

Sem corpo de resposta.

**Resposta de corpo inválido — `400 Bad Request`:**

```json
{
  "type": "about:blank",
  "title": "Corpo da requisição inválido",
  "status": 400,
  "detail": "content: tamanho deve ser entre 0 e 1000000"
}
```

> **Escritas concorrentes:** o upsert é atômico (`INSERT ... ON CONFLICT DO UPDATE`
> em uma única query), então duas escritas simultâneas no mesmo slug nunca geram
> erro. A última a ser processada pelo banco vence (last-write-wins). Não existe
> resposta `409` para esse caso. Ver [Concorrência](ARCHITECTURE.md#concorrência)
> na arquitetura.

---

## Respostas de Erro

Todos os erros seguem o formato [RFC 9457 (Problem Details)](https://www.rfc-editor.org/rfc/rfc9457):

```json
{
  "type": "about:blank",
  "title": "Descrição curta do erro",
  "status": 400,
  "detail": "Mensagem detalhada sobre o problema."
}
```

| Status | Situação |
|---|---|
| `400` | Slug inválido ou corpo da requisição fora dos limites |
| `500` | Erro interno inesperado |

---

## Cache

Leituras (`GET`) são cacheadas in-process com **Caffeine**:

- **TTL:** 30 segundos após a escrita
- **Capacidade:** até 500 notas em memória
- **Invalidação:** o cache da nota é invalidado imediatamente após qualquer `PUT` bem-sucedido

---

## CORS

Por padrão, apenas `http://localhost:5173` é aceito como origem.

Em produção, configure a variável de ambiente `ALLOWED_ORIGINS`:

```bash
ALLOWED_ORIGINS=https://escreveaqui.com.br,https://www.escreveaqui.com.br
```

---

## Exemplo de uso (curl)

**Criar ou atualizar uma nota:**

```bash
curl -X PUT http://localhost:8080/api/v1/notes/minha-nota \
  -H "Content-Type: application/json" \
  -d '{"content": "Olá, mundo!"}'
```

**Ler uma nota:**

```bash
curl http://localhost:8080/api/v1/notes/minha-nota
```
