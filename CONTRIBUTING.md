# 🤝 Contribuindo para o Escreve Aqui

Obrigado por considerar contribuir com o **Escreve Aqui** 🇧🇷  
Queremos manter o projeto simples, organizado e acessível para todos.

---

## 📌 Antes de começar

- Leia o [README](README.md) para entender o projeto
- Verifique se já existe uma issue relacionada ao que você quer fazer
- Mantenha o foco na simplicidade e no minimalismo

---

## 🐛 Reportando Bugs

Antes de abrir uma issue:

1. Verifique se o bug já foi reportado
2. Certifique-se de que está usando a versão mais recente

Ao abrir uma issue, inclua:

- Descrição clara do problema
- Passos para reproduzir
- Comportamento esperado vs. comportamento atual
- Prints ou vídeo (se aplicável)
- Ambiente:
  - Sistema operacional
  - Versão do Node.js
  - Versão do Java
  - Navegador (para bugs de UI)

---

## ✨ Sugerindo Melhorias

Para novas funcionalidades:

1. Abra uma issue primeiro
2. Explique:
   - Qual problema resolve
   - Por que é útil para o projeto
   - Possível abordagem técnica (se souber)

Evite abrir Pull Request sem discussão prévia para features grandes.

---

## 🚀 Como Contribuir

### 1. Fork o projeto

Clique em **Fork** no GitHub.

### 2. Clone seu fork

```bash
git clone https://github.com/SEU-USUARIO/escreveaqui.git
cd escreveaqui
```

### 3. Configure o ambiente

Consulte o [README](README.md) para instalar as dependências e configurar as variáveis de ambiente.

### 4. Crie uma branch

Use o padrão:

```
feature/nome-da-feature
fix/nome-do-bug
docs/descricao
refactor/descricao
```

Exemplo:

```bash
git checkout -b feature/dark-mode
```

---

## 🧱 Padrão de Commits

Utilizamos **Conventional Commits**:

```
feat: adiciona modo escuro
fix: corrige erro de validação no slug
docs: atualiza README com variáveis de ambiente
refactor: simplifica lógica do UpsertNotaService
test: adiciona testes para o endpoint de leitura
chore: atualiza dependências do frontend
```

---

## 🧪 Rodando o Projeto Localmente

Consulte o [README](README.md#-como-rodar-localmente) para instruções completas de setup.

---

## 🧼 Padrões de Código

### Frontend

- **TypeScript** obrigatório — sem `any` sem justificativa
- **Componentes funcionais** com hooks
- **Tailwind CSS** para estilização — não adicionar CSS avulso sem necessidade
- **Shadcn/UI** para componentes de interface — prefira os componentes existentes antes de criar novos
- Lógica pesada fora dos componentes (hooks ou services)
- Manter minimalismo visual — novas telas devem seguir o estilo existente

### Backend

- **Java 21** — sem features de preview sem aprovação prévia
- Seguir padrão REST
- Controllers finos — apenas recebem e devolvem dados
- Lógica concentrada nos services
- Tratamento de exceções via `GlobalExceptionHandler`
- Logging com SLF4J (`@Slf4j`) — `log.debug` para fluxo normal, `log.info` para eventos de negócio, `log.error` para falhas

---

## 🔎 Pull Requests

Antes de abrir um PR:

- [ ] Código compila sem erros
- [ ] Não há erros de lint (`npm run lint` no frontend)
- [ ] Branch está atualizada com `main`
- [ ] Descrição clara do que foi feito e por quê

Template sugerido:

```markdown
## O que foi feito?

## Por que?

## Como testar?

## Prints (se aplicável)
```

---

## 📦 Escopo do Projeto

O Escreve Aqui é:

- Minimalista
- Sem autenticação
- Sem anúncios
- Sem rastreamento

Evite sugestões que adicionem complexidade desnecessária ou que quebrem esses princípios.

---

## 🔐 Segurança

Se encontrar uma vulnerabilidade, **não abra uma issue pública**.

Envie um e-mail para: weslleysantoszbd@gmail.com

---

## 🧭 Filosofia do Projeto

- Simplicidade acima de tudo
- Código limpo e legível
- Zero complexidade desnecessária
- Transparência
- Comunidade aberta e respeitosa

---

## 💚 Agradecimento

Cada contribuição importa.

Obrigado por ajudar a construir uma alternativa open source, brasileira e minimalista.
