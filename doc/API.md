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

**Resposta de conflito de escrita simultânea — `409 Conflict`:**

```json
{
  "type": "about:blank",
  "title": "Conflito de edição",
  "status": 409,
  "detail": "A nota foi modificada por outra sessão. Tente novamente."
}
```

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
| `409` | Conflito de escrita simultânea ou violação de unicidade |
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
