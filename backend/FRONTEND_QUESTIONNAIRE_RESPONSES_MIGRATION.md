# 📋 Migração: Respostas do Questionário — Remoção da Paginação

## Motivação

A paginação foi removida dos endpoints de resposta do questionário.
O motivo principal: o cálculo de `completed` (se o representante respondeu **todas** as perguntas)
era sempre `false` porque as respostas eram enviadas e buscadas em pedaços — o servidor nunca conseguia
verificar se todas as perguntas tinham sido respondidas.

---

## 🔄 Endpoints alterados

### ❌ Antigo — GET com paginação

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/responses/page
     ?page=0&size=10&representativeId=14
```

**Resposta antiga:**
```json
{
  "pageNumber": 0,
  "pageSize": 10,
  "totalPages": 2,
  "completed": false,
  "answers": [ ... ]
}
```

### ✅ Novo — GET sem paginação

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/responses
```

**Resposta nova:**
```json
{
  "completed": false,
  "answers": [
    {
      "questionId": 24,
      "response": null,
      "justification": null,
      "evidence": null,
      "attachments": []
    },
    ...
  ]
}
```

---

### ❌ Antigo — POST com paginação

```
POST /api/projects/{projectId}/questionnaires/{questionnaireId}/responses/page
```

**Body antigo:**
```json
{
  "pageNumber": 0,
  "pageSize": 10,
  "representativeId": 14,
  "draft": false,
  "answers": [
    { "questionId": 24, "response": true },
    { "questionId": 25, "response": false }
  ]
}
```

### ✅ Novo — POST sem paginação

```
POST /api/projects/{projectId}/questionnaires/{questionnaireId}/responses
```

**Body novo:**
```json
{
  "representativeId": 14,
  "draft": false,
  "answers": [
    { "questionId": 24, "response": true,  "justification": null, "evidence": null, "attachments": [] },
    { "questionId": 25, "response": false, "justification": null, "evidence": null, "attachments": [] },
    { "questionId": 26, "response": true,  "justification": null, "evidence": null, "attachments": [] }
  ]
}
```

> **Importante:** `representativeId` é opcional para usuários não-admin. O backend resolve automaticamente
> pelo usuário autenticado. Só envie quando for admin enviando em nome de outro representante.

**Resposta nova (igual ao GET):**
```json
{
  "completed": true,
  "answers": [ ... ]
}
```

---

## 📝 O que o frontend precisa mudar

### 1. Remover toda lógica de paginação

- Remover controles de página (`page`, `totalPages`, `pageSize`, `pageNumber`)
- Remover botões "Próxima página" / "Página anterior" no questionário
- Remover parâmetros `page` e `size` nas chamadas à API

### 2. Carregar todas as respostas de uma vez

```js
// Antigo
const { data } = await api.get(
  `/projects/${projectId}/questionnaires/${questionnaireId}/responses/page`,
  { params: { page: 0, size: 10, representativeId } }
);

// Novo
const { data } = await api.get(
  `/projects/${projectId}/questionnaires/${questionnaireId}/responses`
);
// data = { completed: boolean, answers: [...] }
```

### 3. Enviar TODAS as respostas de uma vez ao submeter

```js
// Antigo (enviava por página)
await api.post(
  `/projects/${projectId}/questionnaires/${questionnaireId}/responses/page`,
  {
    pageNumber: currentPage,
    pageSize: 10,
    draft: false,
    answers: answersOnCurrentPage
  }
);

// Novo (envia todas de uma vez)
await api.post(
  `/projects/${projectId}/questionnaires/${questionnaireId}/responses`,
  {
    draft: false,
    answers: allAnswers  // TODAS as respostas do representante
  }
);
```

### 4. Rascunho (salvar progresso)

Mesma chamada, apenas com `draft: true`. O servidor salva sem marcar como concluído:

```js
await api.post(
  `/projects/${projectId}/questionnaires/${questionnaireId}/responses`,
  {
    draft: true,
    answers: allAnswers  // pode ter respostas parciais (null = não respondido)
  }
);
```

### 5. Verificar `completed` para feedback visual

```js
const { completed, answers } = response.data;

if (completed) {
  // Mostrar mensagem de sucesso: "Questionário concluído!"
  // Desabilitar edição
} else {
  // Mostrar progresso: X de Y perguntas respondidas
  const answered = answers.filter(a => a.response !== null).length;
  const total = answers.length;
  // "Você respondeu X de Y perguntas"
}
```

### 6. Sugestão de fluxo UX

1. **Ao entrar na tela do questionário:**
   - `GET /responses` → carregar todas as respostas existentes
   - Exibir todas as perguntas em scroll (sem paginação)
   - Mostrar progresso: "X/Y respondidas"

2. **Ao salvar rascunho (botão "Salvar progresso"):**
   - `POST /responses` com `draft: true` e todas as respostas atuais (incluindo nulls)

3. **Ao submeter (botão "Finalizar questionário"):**
   - Validar localmente se todas foram respondidas
   - `POST /responses` com `draft: false` e todas as respostas
   - Se `completed: true` → feedback de sucesso

---

## ⚠️ Atenção: endpoint antigo foi removido

`/responses/page` **não existe mais**. Atualizar todas as chamadas para `/responses`.

---

## Resumo dos campos removidos do request/response

| Campo removido | Onde era usado |
|---|---|
| `pageNumber` | body do POST e resposta do GET |
| `pageSize` | body do POST e resposta do GET |
| `totalPages` | resposta do GET |
| `?page=` | query param do GET |
| `?size=` | query param do GET |
| `?representativeId=` | query param do GET (não é mais necessário) |

