# Migração: Rotas de Perguntas do Questionário (Separação Admin x Representante)

## Contexto

A rota `GET /questions` foi **dividida em duas rotas distintas** para separar claramente o que um **representante** vê (apenas suas perguntas) do que um **administrador/dono do projeto** vê (todas as perguntas).

---

## Rotas Atualizadas

### 1. `GET /api/projects/{projectId}/questionnaires/{questionnaireId}/questions`

**Propósito:** Listar as perguntas do questionário **filtradas pelos papéis do representante autenticado**.  
**Quem usa:** Apenas **representantes** (membros do projeto que devem responder o questionário).  
**Comportamento:**
- Retorna somente as perguntas associadas aos papéis que o representante autenticado possui no projeto.
- Parâmetro opcional `representativeId`: permite que um admin consulte as perguntas de um representante específico.
- **Não deve ser chamada por admins para visualizar todas as perguntas** — use `/questions/all` para isso.

**Query params:**
| Parâmetro        | Tipo    | Obrigatório | Descrição                                      |
|------------------|---------|-------------|------------------------------------------------|
| `page`           | int     | Não         | Número da página (padrão: 0)                   |
| `size`           | int     | Não         | Tamanho da página (padrão: 10)                 |
| `questionText`   | String  | Não         | Filtro parcial por texto da pergunta           |
| `roleName`       | String  | Não         | Filtro pelo nome do papel                      |
| `representativeId` | Long  | Não         | ID do representante (opcional, para admin)     |

**Exemplo de uso (representante respondendo):**
```
GET /api/projects/1/questionnaires/5/questions?page=0&size=10
```

---

### 2. `GET /api/projects/{projectId}/questionnaires/{questionnaireId}/questions/all` ⭐ NOVA ROTA

**Propósito:** Listar **todas** as perguntas do questionário **sem filtro de papel**.  
**Quem usa:** Apenas **administradores** e **donos do projeto**.  
**Comportamento:**
- Retorna todas as perguntas do questionário, independentemente do papel.
- Retorna HTTP 403 se o usuário autenticado não for admin/owner do projeto.
- Deve ser usada pelo admin para visualizar o questionário completo, ver perguntas de todos os papéis, etc.

**Query params:**
| Parâmetro      | Tipo   | Obrigatório | Descrição                            |
|----------------|--------|-------------|--------------------------------------|
| `page`         | int    | Não         | Número da página (padrão: 0)         |
| `size`         | int    | Não         | Tamanho da página (padrão: 10)       |
| `questionText` | String | Não         | Filtro parcial por texto da pergunta |
| `roleName`     | String | Não         | Filtro pelo nome do papel            |

**Exemplo de uso (admin visualizando todas as perguntas):**
```
GET /api/projects/1/questionnaires/5/questions/all?page=0&size=20
```

---

## O que o Frontend Precisa Adaptar

### Tela de resposta do questionário (representante)
- **Não muda nada.** Continue usando `GET /questions`.
- A rota já retorna apenas as perguntas dos papéis do representante autenticado.

### Tela de administração / visualização do questionário pelo admin
- **Troque** `GET /questions` por `GET /questions/all`.
- Isso garante que o admin veja **todas as perguntas** de todos os papéis, sem depender de qual papel ele tem como representante.

### Decisão de qual rota chamar por perfil
```typescript
// Exemplo (adapte ao seu serviço/hook):
const fetchQuestions = (projectId: number, questionnaireId: number, isAdmin: boolean, pageable: PageParams) => {
  if (isAdmin) {
    // Admin vê tudo
    return api.get(`/api/projects/${projectId}/questionnaires/${questionnaireId}/questions/all`, { params: pageable });
  } else {
    // Representante vê apenas suas perguntas
    return api.get(`/api/projects/${projectId}/questionnaires/${questionnaireId}/questions`, { params: pageable });
  }
};
```

### Verificação de perfil no frontend
O campo que define se o usuário é admin pode vir do token JWT ou do endpoint de perfil.  
Use o mesmo campo que você já usa para exibir/ocultar menus de gestão.

---

## Resumo das Mudanças de Backend

| Antes                         | Depois                             |
|-------------------------------|------------------------------------|
| `GET /questions` (para todos) | `GET /questions` (só representante) |
| —                             | `GET /questions/all` (só admin) ⭐ |

> **Atenção:** A rota `POST /questions/search` no `QuestionnaireQueryController` **não foi alterada** e continua funcionando para buscas avançadas com body, também com controle de acesso por perfil.

