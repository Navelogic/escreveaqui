# Escreve Aqui 🇧🇷

> Texto online, anônimo e minimalista.  
> Open source, sem anúncios, feito no Brasil.

![logo do projeto](https://raw.githubusercontent.com/Navelogic/escreveaqui/refs/heads/main/doc/GitHub%20Social%20Preview.png)

---

![License](https://img.shields.io/github/license/Navelogic/escreveaqui?style=flat-square&color=009c3b)
![React](https://img.shields.io/badge/frontend-React-61DAFB?style=flat-square&logo=react&logoColor=black)
![Spring Boot](https://img.shields.io/badge/backend-Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)

---

## 💡 Por que o Escreve Aqui existe?

Muitas ferramentas de texto online são cheias de anúncios, rastreadores ou exigem cadastro. Precisávamos de algo simples para criar e compartilhar textos rapidamente através de uma URL amigável.

O **Escreve Aqui** nasceu para ser o oposto disso:

- Sem anúncios
- Sem cadastro
- Sem rastreadores

Apenas um espaço para escrever e compartilhar.

---

## 🎯 Filosofia do Projeto

- Simplicidade acima de tudo
- Código limpo e legível
- Privacidade em primeiro lugar
- Comunidade aberta e respeitosa

Qualquer nova funcionalidade deve respeitar esses princípios.

---

## 🚀 Tecnologias

### Frontend

| Tecnologia | Versão | Finalidade |
|---|---|---|
| React | 19 | UI |
| TypeScript | 5.9 | Tipagem |
| Vite | 7 | Build |
| Tailwind CSS | 3 | Estilização |
| Shadcn/UI | — | Componentes |
| React Router | 7 | Roteamento |

### Backend

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Spring Boot | 4 | Framework web |
| Java | 21 (LTS) | Linguagem |
| PostgreSQL | — | Banco de dados |
| Caffeine | — | Cache in-process |
| HikariCP | — | Pool de conexões |

---

## 📦 Como rodar localmente

### Com Docker (recomendado)

A forma mais fácil de rodar o projeto completo:

```bash
# Clone o repositório
git clone https://github.com/Navelogic/escreveaqui.git
cd escreveaqui

# Copie e configure as variáveis de ambiente
cp .env.example .env

# Suba todos os serviços
docker compose up -d
```

Acesse:
- **Frontend:** `http://localhost:3000`
- **Backend:** `http://localhost:8080`
- **PostgreSQL:** `localhost:5433`

Para parar os serviços:

```bash
docker compose down
```

---

### Sem Docker

#### Pré-requisitos

- Node.js 22+
- Java JDK 21+
- PostgreSQL 14+

---

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Disponível em: `http://localhost:5173`

---

### Backend

**1. Configure as variáveis de ambiente:**

```bash
export DB_HOST=localhost
export DB_DATABASE=escreveaqui
export DB_USERNAME=seu_usuario
export DB_PASSWORD=sua_senha

# Opcional — padrão: http://localhost:5173
export ALLOWED_ORIGINS=https://escreveaqui.com.br
```

**2. Inicie a aplicação:**

```bash
cd backend
./mvnw spring-boot:run
```

API disponível em: `http://localhost:8080`

> **Dica:** crie um arquivo `.env` e exporte as variáveis antes de rodar, ou configure-as no seu ambiente de CI/CD.

---

## 🔌 API

A documentação completa da API está em [doc/API.md](doc/API.md).

Resumo dos endpoints:

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/notes/{slug}` | Lê uma nota pelo slug |
| `PUT` | `/api/v1/notes/{slug}` | Cria ou atualiza uma nota |

---

## 🏗️ Arquitetura

A documentação de arquitetura está em [doc/ARCHITECTURE.md](doc/ARCHITECTURE.md).

Visão geral:

```
Frontend (React + Tailwind)
    ↕ HTTP (axios)
Backend (Spring Boot)
    ↕ Caffeine (cache in-process, TTL 30s)
    ↕ HikariCP (pool de conexões)
PostgreSQL
```

---

## 🤝 Contribuição

Contribuições são muito bem-vindas.

Antes de contribuir:

1. Leia o [CONTRIBUTING.md](CONTRIBUTING.md)
2. Verifique issues existentes antes de abrir uma nova
3. Mantenha o foco na simplicidade

---

## 🔐 Privacidade

- Não exige cadastro
- Não utiliza rastreadores
- Não exibe anúncios
- Notas são públicas — qualquer pessoa com a URL pode ler e editar
- Notas inativas por 30 dias são removidas automaticamente

---

## 📌 Status do Projeto

🚧 Em desenvolvimento ativo

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

Feito com 💚 no Brasil
