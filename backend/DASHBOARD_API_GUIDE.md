# Frontend Integration Guide – BR04 Dashboard ISEP

## Novos Endpoints Adicionados

### Base URL: `http://localhost:8080/api/projects/{projectId}`

---

## 1. Dashboard Consolidado do Projeto (Nível 3 – ISEP Global)

```
GET /api/projects/{projectId}/dashboard
```

**Resposta:** `ProjectIsepDashboardDTO`

```json
{
  "projectId": 7,
  "projectName": "Meu Projeto",
  "projectType": "ITERATIVO",
  "projectIsep": 0.8523,
  "projectIsepPercent": 85.23,
  "projectBand": "B",
  "isepHistory": [
    {
      "questionnaireId": 13,
      "questionnaireName": "Sprint 1",
      "stageName": "Iniciação",
      "iterationName": "Sprint 1",
      "isep": 0.8523,
      "isepPercent": 85.23,
      "band": "B",
      "calculatedAt": "2026-02-17T15:51:33"
    }
  ],
  "totalQuestionnaires": 3,
  "completedQuestionnaires": 1
}
```

---

## 2. Dashboard da Iteração/Etapa (Nível 2 – Visão Gestão)

Disponível **após 100% das respostas** (ou deadline expirado).

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/dashboard
```

**Resposta:** `QuestionnaireIsepDashboardDTO`

```json
{
  "questionnaireId": 13,
  "questionnaireName": "Iniciação",
  "stageName": "Iniciação",
  "iterationName": null,
  "isep": 0.8523,
  "isepPercent": 85.23,
  "band": "B",
  "teamSimpleAverage": 0.8200,
  "teamSimpleAveragePercent": 82.00,
  "teamStandardDeviation": 0.0512,
  "teamStandardDeviationPercent": 5.12,
  "calculatedAt": "2026-02-17T15:51:33",
  "bandDistribution": { "A": 1, "B": 2, "C": 0, "D": 0, "E": 0 },
  "memberResults": [
    {
      "representativeId": 9,
      "representativeName": "Pedro Costa",
      "icp": 0.9000,
      "icpPercent": 90.00,
      "band": "A"
    }
  ],
  "stageResults": [
    {
      "representativeId": 9,
      "representativeName": "Pedro Costa",
      "stageId": 17,
      "stageName": "Iniciação",
      "iem": 0.9000,
      "iemPercent": 90.00,
      "band": "A"
    }
  ],
  "justificationTexts": []
}
```

---

## 3. Heatmap de Risco + Gráfico 4: Percepção por Encargo

Dados para o **Gráfico 3 (Heatmap)** e **Gráfico 4 (Barras Agrupadas por Papel)**.

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/dashboard/role-stage
```

**Resposta:** `List<RoleStageComplianceDTO>`

```json
[
  {
    "roleId": 1,
    "roleName": "Desenvolvedor",
    "iemByStage": {
      "17": {
        "stageId": 17,
        "stageName": "Iniciação",
        "iemPercent": 85.00,
        "band": "B",
        "memberCount": 2
      },
      "18": {
        "stageId": 18,
        "stageName": "Requisitos",
        "iemPercent": 72.50,
        "band": "C",
        "memberCount": 2
      }
    }
  },
  {
    "roleId": 2,
    "roleName": "Gerente de Projeto",
    "iemByStage": {
      "17": {
        "stageId": 17,
        "stageName": "Iniciação",
        "iemPercent": 91.00,
        "band": "A",
        "memberCount": 1
      }
    }
  }
]
```

**Como usar no frontend:**

- **Heatmap (Gráfico 3):** Linhas = papéis (`roleName`), Colunas = etapas (`stageName`), Valor = `iemPercent`, Cor = `band`
- **Gráfico 4 (Barras Agrupadas):** Eixo X = etapas, Barras = papéis, Altura = `iemPercent`

---

## 4. Nuvem de Palavras-Chave das Justificativas (Widget 5)

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/dashboard/word-cloud
```

**Resposta:** `WordCloudDTO`

```json
{
  "questionnaireId": 13,
  "wordFrequency": {
    "prazo": 15,
    "pressão": 12,
    "custo": 8,
    "cliente": 7
  },
  "topWords": [
    { "word": "prazo", "frequency": 15 },
    { "word": "pressão", "frequency": 12 },
    { "word": "custo", "frequency": 8 }
  ],
  "totalJustifications": 24
}
```

**Como usar:** Passar `topWords` direto para uma biblioteca de word cloud (ex: `react-d3-cloud`, `angular-tag-cloud-module`).

---

## 5. Painel Individual (Visão 1 – Disponível após submissão)

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/dashboard/individual?representativeId={id}
```

**Resposta:** `IndividualDashboardDTO`

```json
{
  "representativeId": 14,
  "representativeName": "YVES SILVA",
  "personalIcp": 0.7500,
  "personalIcpPercent": 75.00,
  "personalBand": "B",
  "personalHistoricalAverage": 0.7800,
  "personalHistoricalAveragePercent": 78.00,
  "teamAnonymousAverage": 0.8200,
  "teamAnonymousAveragePercent": 82.00,
  "personalEvolution": [
    {
      "questionnaireId": 13,
      "questionnaireName": "Iniciação",
      "stageName": "Iniciação",
      "iterationName": null,
      "isep": 0.7500,
      "isepPercent": 75.00,
      "band": "B",
      "calculatedAt": "2026-02-17T15:51:33"
    }
  ]
}
```

> **Nota:** `teamAnonymousAverage` é `null` quando o ISEP ainda não foi calculado (equipe não completou 100%).

---

## 6. Exportação de Dados JSON e CSV (BR04 §5)

### Exportar todos os questionários do projeto (JSON):

```
GET /api/projects/{projectId}/dashboard/export?anonymize=true
```

### Exportar um questionário específico (JSON):

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/dashboard/export?anonymize=true
```

**Resposta (questionário único):** `IsepDataExportDTO`

```json
{
  "projectId": 7,
  "projectName": "Meu Projeto",
  "questionnaireId": 13,
  "questionnaireName": "Iniciação",
  "iterationOrStageName": "Iniciação",
  "calculatedAt": "2026-02-17T15:51:33",
  "isepPercent": 85.23,
  "band": "B",
  "teamAveragePercent": 82.00,
  "standardDeviationPercent": 5.12,
  "members": [
    {
      "representativeId": 9,
      "memberName": null,
      "icpPercent": 90.00,
      "band": "A",
      "stageResults": [
        { "stageId": 17, "stageName": "Iniciação", "iemPercent": 90.00 }
      ]
    }
  ]
}
```

### Exportar todos os questionários do projeto (CSV):

```
GET /api/projects/{projectId}/dashboard/export/csv?anonymize=true
```

**Headers da resposta CSV:**
```
Content-Disposition: attachment; filename="isep-projeto-{projectId}.csv"
Content-Type: text/csv; charset=UTF-8
```

### Exportar um questionário específico (CSV):

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/dashboard/export/csv?anonymize=true
```

**Headers da resposta CSV:**
```
Content-Disposition: attachment; filename="isep-questionario-{questionnaireId}.csv"
Content-Type: text/csv; charset=UTF-8
```

**Parâmetro `anonymize`:**
- `true` (padrão): omite os nomes dos membros (para gestores Nível 2)
- `false`: inclui nomes completos (somente para admins Nível 1)

---

## 7. Encerramento Manual de Questionário (Admin)

Permite que o administrador force o encerramento de um questionário mesmo sem 100% de respostas.

> **Requer:** `hasAuthority('ADMIN')`

```
POST /api/projects/{projectId}/questionnaires/{questionnaireId}/force-close
```

**Resposta:** `204 No Content`

**Comportamento:**
- Calcula o ISEP com as respostas existentes (ignora a validação de 100%).
- Marca o questionário com status `CONCLUIDO`.
- Dispara notificação `QUESTIONNAIRE_ISEP_CALCULATED` para todos os representantes e o administrador.
- Automaticamente tenta finalizar o projeto se todos os outros questionários já estiverem concluídos (`tryFinalizeProjectAfterQuestionnaire`).
- Lança `400 Bad Request` se o questionário já estiver encerrado.
- Lança `400 Bad Request` se não houver nenhuma resposta registrada.

---

## 8. Encerramento Manual do Projeto (Admin)

Calcula o ISEP consolidado (Nível 3) com os questionários que já possuem resultado.

> **Requer:** `@projectAccessAuthorizationEvaluator.canAccess(authentication)`

```
POST /api/projects/{projectId}/close
```

**Resposta:** `200 OK`

```json
{
  "projectId": 7,
  "isepPercent": "85.23",
  "band": "B",
  "questionnaireCount": 3,
  "calculatedAt": "2026-03-07T03:00:00",
  "closedBy": "Yves Silva"
}
```

**Regras de validação:**
- Lança `400 Bad Request` se o projeto já possuir ISEP consolidado calculado.
- Lança `400 Bad Request` se nenhum questionário possuir ISEP calculado (requer ao menos um).
- Não exige 100% dos questionários concluídos — encerra com o que existe.

**Comportamento após encerramento:**
- Persiste `ProjectIsepResult` na tabela `project_isep_result`.
- Altera `project.status = CONCLUIDO` e `project.timelineStatus = CONCLUIDO`.
- Define `project.closingDate` como a data atual.
- Dispara notificação `PROJECT_ISEP_CALCULATED` para o admin e todos os representantes.

---

## 9. Cálculo Automático por Scheduler (Jobs Noturnos)

O sistema executa jobs automáticos diariamente via `TimelineStatusScheduler`.

### Horários dos Jobs

| Job | Horário (cron) | Responsabilidade |
|-----|:-:|---|
| Atualizar status de timeline | `0 0 0 * * *` (00:00) | Recalcula `timelineStatus` de todos os projetos |
| Lembretes automáticos de questionário | `0 0 6 * * *` (06:00) | Envia e-mails/notificações de lembrete |
| Lembretes de deadline do projeto | `0 0 7 * * *` (07:00) | Notifica proximidade do prazo final |
| **Processar ISEP de questionários expirados** | **`0 0 2 * * *` (02:00)** | Calcula ou marca como atrasado |
| **Processar ISEP de projetos expirados** | **`0 0 3 * * *` (03:00)** | Calcula consolidado ou marca como atrasado |

### Fluxo do Job de Questionários (`processExpiredQuestionnairesIsep` - 02:00)

Busca questionários com `applicationEndDate = hoje` que ainda **não possuem resultado ISEP**.

```
Para cada questionário expirado:
  ├── Se 100% dos representantes responderam:
  │    ├── Calcula ISEP (ProcessQuestionnaireIsepUseCase)
  │    ├── Notifica QUESTIONNAIRE_ISEP_CALCULATED
  │    └── Tenta finalizar o projeto automaticamente (tryFinalizeProjectAfterQuestionnaire)
  └── Se incompleto:
       ├── Marca questionnaire.status = ATRASADO
       └── Notifica QUESTIONNAIRE_OVERDUE para o admin do projeto
```

### Fluxo do Job de Projetos (`processExpiredProjectsIsep` - 03:00)

Busca projetos com `deadline = hoje`, status `ABERTO`, sem `ProjectIsepResult`.

```
Para cada projeto expirado:
  ├── Se todos os questionários possuem ISEP calculado:
  │    ├── Calcula ISEP consolidado (Nível 3)
  │    ├── Persiste ProjectIsepResult
  │    ├── Atualiza project.status = CONCLUIDO / timelineStatus = CONCLUIDO
  │    └── Notifica PROJECT_ISEP_CALCULATED para admin + representantes
  └── Se incompleto:
       ├── Marca project.timelineStatus = ATRASADO
       └── Notifica PROJECT_OVERDUE para admin + representantes
```

### Auto-finalização em Cascata

Quando o **último questionário** de um projeto é concluído (via scheduler ou `force-close`), o sistema automaticamente chama `tryFinalizeProjectAfterQuestionnaire`:

```
Questionnaire concluído → verifica se TODOS os questionários do projeto têm ISEP calculado
  └── Se sim → calculateAndPersistProjectIsep → status = CONCLUIDO → PROJECT_ISEP_CALCULATED
```

---

## 10. Notificações Automáticas do Ciclo de Vida

| Tipo (`NotificationType`) | Quando é disparado | Destinatários |
|---|---|---|
| `QUESTIONNAIRE_REMINDER` | Antes do prazo do questionário | Representantes pendentes |
| `DEADLINE_REMINDER` | Antes do prazo final do projeto | Admin + representantes |
| `QUESTIONNAIRE_ISEP_CALCULATED` | ISEP do questionário calculado (scheduler ou force-close) | Admin + representantes |
| `QUESTIONNAIRE_OVERDUE` | Questionário expirado sem 100% de respostas | Admin do projeto |
| `PROJECT_ISEP_CALCULATED` | ISEP consolidado do projeto calculado | Admin + todos os representantes |
| `PROJECT_OVERDUE` | Projeto expirado sem todos os questionários concluídos | Admin + todos os representantes |

---

## 11. Modelo de Cálculo ISEP (Resumo Técnico)

### Projeto Iterativo — 4 Níveis

| Nível | Nome | Fórmula |
|---|---|---|
| L0 | IEM (Índice de Etapa por Membro) | `SIM_da_etapa / total_perguntas_da_etapa` |
| L1 | ICP (Índice de Conformidade Pessoal) | `Σ(IEM × peso_etapa) / Σ(peso_etapa)` |
| L2 | ISEP da Iteração | `Σ(ICP × peso_membro) / Σ(peso_membro)` |
| L3 | ISEP Consolidado do Projeto | `Σ(ISEP_iteração × peso_questionário) / Σ(peso_questionário)` |

### Projeto Cascata — 3 Níveis

| Nível | Nome | Fórmula |
|---|---|---|
| L1 | ICP da Etapa | `total_SIM / total_perguntas` |
| L2 | ISEP da Etapa | `Σ(ICP × peso_membro) / Σ(peso_membro)` |
| L3 | ISEP Consolidado do Projeto | `Σ(ISEP_etapa × peso_etapa) / Σ(peso_etapa)` |

> **Precisão:** Todos os cálculos utilizam `BigDecimal` com `RoundingMode.HALF_UP` e precisão de 4 casas decimais.

### Entidade `ProjectIsepResult` (tabela `project_isep_result`)

| Campo | Tipo | Descrição |
|---|---|---|
| `result_id` | `BIGINT` (PK) | Identificador único |
| `project_id` | `BIGINT` (UNIQUE) | Referência ao projeto (1 resultado por projeto) |
| `isep` | `DECIMAL(7,4)` | ISEP consolidado (0.0000 a 1.0000) |
| `band` | `VARCHAR(1)` | Faixa de classificação (A, B, C, D, E) |
| `questionnaire_count` | `INT` | Número de questionários incluídos no cálculo |
| `calculated_at` | `TIMESTAMP` | Data/hora do cálculo |
| `closed_by` | `VARCHAR(255)` | Quem encerrou (usuário ou "Sistema (Scheduler)") |
---

## Novos Endpoints – Visualização de Respostas por Representante e Consolidadas

### 1. Respostas Detalhadas de um Representante Específico

Permite ao administrador visualizar **todas as respostas** de um representante selecionado em um questionário.

> **Botão no frontend:** "Ver Respostas" ao lado de cada membro na tabela **Resultados por Membro** (imagem com ICP/Faixa por membro).

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/responses/representative/{representativeId}
```

**Resposta:** `RepresentativeResponseDTO`

```json
{
  "representativeId": 14,
  "representativeName": "Gabriel Nama",
  "questionnaireId": 13,
  "status": "COMPLETED",
  "submissionDate": "2026-03-07T16:20:33",
  "totalQuestions": 15,
  "answeredQuestions": 15,
  "yesCount": 13,
  "noCount": 2,
  "answers": [
    {
      "questionId": 101,
      "questionText": "As decisões de design consideram impacto ético?",
      "stageIds": [1],
      "roleIds": [3, 5],
      "response": true,
      "justification": { "descricao": "Revisão de pares", "url": null },
      "evidence": null,
      "attachments": []
    },
    {
      "questionId": 102,
      "questionText": "A privacidade dos dados foi avaliada?",
      "stageIds": [1],
      "roleIds": [3],
      "response": false,
      "justification": { "descricao": "Prazo apertado impediu análise completa", "url": null },
      "evidence": null,
      "attachments": []
    }
  ]
}
```

**Campos importantes:**
| Campo | Descrição |
|---|---|
| `totalQuestions` | Total de perguntas atribuídas ao representante |
| `answeredQuestions` | Quantas possuem resposta (SIM ou NÃO) |
| `yesCount` / `noCount` | Contagem de SIM/NÃO |
| `answers[].stageIds` | Etapas associadas à pergunta |
| `answers[].roleIds` | Papéis associados à pergunta |
| `answers[].justification` | Justificativa (preenchida quando resposta = NÃO) |

---

### 2. Respostas Consolidadas do Questionário (Paginadas + Filtros)

Listagem paginada de **todas as respostas de todos os representantes** de um questionário específico. Cada linha = 1 resposta de 1 representante a 1 pergunta.

> **Botão no frontend:** "Ver Todas as Respostas" no dashboard do questionário (Nível 2).

```
GET /api/projects/{projectId}/questionnaires/{questionnaireId}/responses/consolidated
```

**Parâmetros de filtro (query params):**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `representativeId` | `Long` | Filtrar por representante específico |
| `questionId` | `Long` | Filtrar por pergunta específica |
| `roleId` | `Long` | Filtrar por papel (role) associado à pergunta |
| `response` | `Boolean` | Filtrar por resposta: `true` (SIM) ou `false` (NÃO) |
| `questionText` | `String` | Busca parcial no texto da pergunta (case-insensitive) |
| `page` | `int` | Página (default: 0) |
| `size` | `int` | Itens por página (default: 20) |
| `sort` | `String` | Ordenação (ex: `questionId,asc`) |

**Resposta:** `Page<ConsolidatedAnswerDTO>`

```json
{
  "content": [
    {
      "representativeId": 14,
      "representativeName": "Gabriel Nama",
      "roles": ["Desenvolvedor", "Testador"],
      "questionnaireId": 13,
      "responseStatus": "COMPLETED",
      "submissionDate": "2026-03-07T16:20:33",
      "questionId": 101,
      "questionText": "As decisões de design consideram impacto ético?",
      "stageIds": [1],
      "response": true,
      "justification": null,
      "evidence": null,
      "attachments": []
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "size": 20,
  "number": 0
}
```

**Exemplos de uso com filtros:**
```bash
# Todas as respostas NÃO (com justificativas)
GET .../responses/consolidated?response=false&page=0&size=20

# Respostas de um representante específico
GET .../responses/consolidated?representativeId=14&page=0&size=20

# Busca por texto da pergunta
GET .../responses/consolidated?questionText=privacidade&page=0&size=20

# Filtro combinado: role + resposta NÃO
GET .../responses/consolidated?roleId=3&response=false&page=0&size=10
```

---

### 3. Respostas Consolidadas do Projeto (Todos os Questionários)

Mesma estrutura do endpoint anterior, mas abrange **todos os questionários do projeto**.

> **Botão no frontend:** "Ver Todas as Respostas do Projeto" no dashboard consolidado do projeto (Nível 3).

```
GET /api/projects/{projectId}/responses/consolidated
```

**Parâmetros adicionais:**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `questionnaireId` | `Integer` | Filtrar por questionário específico (dentro do projeto) |

Demais filtros iguais ao endpoint 2 (`representativeId`, `questionId`, `roleId`, `response`, `questionText`, paginação).

**Resposta:** `Page<ConsolidatedAnswerDTO>` (mesma estrutura do endpoint 2)

---

### Frontend – Instruções de Implementação

#### Tela: Dashboard ISEP do Questionário (tabela "Resultados por Membro")

1. **Botão "Ver Respostas"** ao lado de cada linha de membro:
   - Ao clicar, abrir modal ou nova tela com:
     - Header: nome do membro, status, data submissão, contadores (total/respondidas/SIM/NÃO)
     - Tabela de respostas com colunas: Pergunta, Resposta (SIM/NÃO badge), Justificativa, Evidência, Anexos
   - Endpoint: `GET .../responses/representative/{representativeId}`

2. **Botão "Ver Todas as Respostas"** no topo do dashboard do questionário:
   - Abre tela com tabela paginada e barra de filtros
   - Filtros: dropdown de Representante, dropdown de Role, toggle SIM/NÃO/TODOS, campo de busca por texto
   - Cada linha mostra: Nome do membro, Roles, Pergunta, Resposta, Justificativa
   - Endpoint: `GET .../responses/consolidated`

#### Tela: Dashboard Consolidado do Projeto

3. **Botão "Ver Todas as Respostas do Projeto"**:
   - Mesma estrutura do item 2, mas com filtro adicional de Questionário (dropdown)
   - Endpoint: `GET /api/projects/{projectId}/responses/consolidated`

#### Componentes sugeridos:
- `ResponseDetailModal` – modal para respostas individuais de um membro
- `ConsolidatedAnswersTable` – tabela paginada com filtros para respostas consolidadas
- `AnswerFilters` – barra de filtros (representante, role, resposta, texto, questionário)


