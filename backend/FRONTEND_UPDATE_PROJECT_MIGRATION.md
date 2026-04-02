# Guia de Migração Frontend — Atualização de Projetos

## Resumo da Mudança

Novos endpoints backend para edição de projetos publicados (status `ABERTO`).
- **`GET /api/projects/{projectId}/edit`** — Carrega o snapshot completo editável do projeto (etapas, iterações, questionários com perguntas, representantes com roles). Funciona para **CASCATA e ITERATIVO**.
- **`PUT /api/projects/{projectId}`** — Atualização incremental com detecção de diff e validação de impacto.

> ⚠️ **IMPORTANTE:** O frontend deve implementar a tela de edição para **AMBOS** os tipos de projeto (Cascata e Iterativo). O backend suporta ambos no mesmo endpoint.

> ⚠️ **NOTA:** Após alterações no backend, certifique-se de fazer `mvn clean compile` para que os novos endpoints sejam registrados. O erro `No static resource api/projects/{id}/edit` indica que o build está desatualizado.

---

## Novo Endpoint — Carregar Dados para Edição

### `GET /api/projects/{projectId}/edit`

**Autenticação:** Bearer Token (Admin/Owner do projeto)

Retorna o snapshot completo do projeto com **todos os dados necessários** para popular o formulário de edição, incluindo informação de `locked` (itens que não podem ser removidos/editados).

### Response (200 OK)

```json
{
  "id": 1,
  "name": "Template Base Governança Ética - Cascata",
  "type": "CASCATA",
  "startDate": "2026-04-01",
  "deadline": "2026-12-31",
  "closingDate": null,
  "status": "ABERTO",
  "timelineStatus": "EM_ANDAMENTO",
  "iterationDuration": null,
  "iterationCount": null,
  "currentSituation": "Etapa: Iniciação",
  "editable": true,
  "editableReason": null,

  "stages": [
    {
      "id": 1,
      "name": "Iniciação",
      "weight": 0.25,
      "sequence": 1,
      "applicationStartDate": "2026-04-01",
      "applicationEndDate": "2026-06-30",
      "status": "EM_ANDAMENTO",
      "locked": false,
      "lockReason": null
    }
  ],

  "iterations": [],

  "questionnaires": [
    {
      "id": 5,
      "name": "Questionário Ética",
      "weight": 1,
      "stageName": "Iniciação",
      "stageId": 1,
      "iterationName": null,
      "iterationId": null,
      "applicationStartDate": "2026-04-01",
      "applicationEndDate": "2026-06-30",
      "status": "EM_ANDAMENTO",
      "domain": "Governança",
      "description": "Descrição do questionário",
      "hasIsepResult": false,
      "locked": false,
      "lockReason": null,
      "questions": [
        {
          "id": 230,
          "text": "Foi determinado quem toma as decisões?",
          "roleIds": [7, 3],
          "roleNames": ["Cliente", "Gerente de Projeto"],
          "stageNames": ["Iniciação"],
          "stageIds": [1]
        }
      ]
    }
  ],

  "representatives": [
    {
      "id": 1,
      "userId": 3,
      "firstName": "Gabriel",
      "lastName": "Nama",
      "email": "gabriel@email.com",
      "roleIds": [7, 3],
      "roleNames": ["Cliente", "Gerente de Projeto"],
      "weight": 1.00,
      "hasResponses": true,
      "locked": true,
      "lockReason": "Representante já possui respostas submetidas. Remoção não permitida."
    }
  ]
}
```

### Campos de controle

| Campo | Significado |
|---|---|
| `editable` | `false` se o projeto está CONCLUÍDO ou tem resultado ISEP final |
| `editableReason` | Motivo pelo qual o projeto não é editável |
| `locked` (em cada item) | `true` se o item não pode ser removido |
| `lockReason` | Motivo pelo qual o item está bloqueado |
| `hasIsepResult` (questionário) | `true` se já teve resultado ISEP calculado — todo o questionário fica locked |
| `hasResponses` (representante) | `true` se já submeteu respostas |

---

## Endpoint de Atualização

### `PUT /api/projects/{projectId}`

**Autenticação:** Bearer Token (Admin/Owner do projeto)

### Request Body

```json
{
  "name": "Nome atualizado do projeto",
  "startDate": "2026-04-01",
  "deadline": "2026-12-31",
  "iterationDuration": 30,
  "iterationCount": 4,
  "dryRun": false,

  "stages": [
    { "id": 1, "name": "Iniciação (renomeada)", "weight": 0.25 },
    { "id": null, "name": "Nova Etapa", "weight": 0.15 }
  ],

  "iterations": [
    { "id": 1, "name": "Sprint 1 (atualizada)", "weight": 0.30, "applicationStartDate": "2026-04-01", "applicationEndDate": "2026-05-01" },
    { "id": null, "name": "Sprint Nova", "weight": 0.20, "applicationStartDate": "2026-05-01", "applicationEndDate": "2026-06-01" }
  ],

  "questionnaires": [
    {
      "id": 5,
      "name": "Questionário Ética - Atualizado",
      "weight": 1,
      "applicationStartDate": "2026-04-01",
      "applicationEndDate": "2026-06-30",
      "domain": "Governança",
      "description": "Descrição atualizada",
      "questions": [
        { "id": 230, "value": "Texto atualizado da pergunta?", "roleIds": [1, 2], "stageNames": ["Iniciação"] },
        { "id": null, "value": "Nova pergunta adicionada?", "roleIds": [3], "stageNames": ["Iniciação"] }
      ]
    }
  ],

  "representatives": [
    { "id": 1, "firstName": "Gabriel", "lastName": "Nama", "email": "gabriel@email.com", "roleIds": [7, 3], "weight": 1.00 },
    { "id": null, "firstName": "Novo", "lastName": "Representante", "email": "novo@email.com", "roleIds": [9], "weight": 1.00 }
  ]
}
```

### Regras de Diff

| Condição | Ação |
|---|---|
| Item com `id: null` | **CRIAÇÃO** — novo registro será criado |
| Item com `id` existente | **ATUALIZAÇÃO** — campos alterados serão aplicados |
| Item existente no banco mas **ausente** no request | **REMOÇÃO** — será removido (sujeito a validação) |

### Modo Preview (Dry Run)

Envie `"dryRun": true` para receber o resumo de mudanças **sem aplicar nenhuma alteração**.
Útil para exibir uma tela de confirmação antes de salvar.

### Response (200 OK)

```json
{
  "id": 1,
  "name": "Nome atualizado do projeto",
  "type": "CASCATA",
  "status": "ABERTO",
  "startDate": "2026-04-01",
  "deadline": "2026-12-31",
  "timelineStatus": "EM_ANDAMENTO",
  "changesSummary": {
    "stagesAdded": 1,
    "stagesRemoved": 0,
    "stagesUpdated": 1,
    "iterationsAdded": 1,
    "iterationsRemoved": 0,
    "iterationsUpdated": 1,
    "questionnairesAdded": 0,
    "questionnairesRemoved": 0,
    "questionnairesUpdated": 1,
    "questionsAdded": 1,
    "questionsRemoved": 0,
    "questionsUpdated": 1,
    "representativesAdded": 1,
    "representativesRemoved": 0,
    "representativesUpdated": 0,
    "responsesCreated": 5,
    "responsesUpdated": 3,
    "responsesDeleted": 0,
    "notificationsSent": 1,
    "warnings": [],
    "blockedReasons": []
  }
}
```

### Response Errors

| HTTP Status | Cenário |
|---|---|
| `400 Bad Request` | Validação de campos (ex: deadline no passado) |
| `409 Conflict` | Operações bloqueadas — corpo contém `errors[]` com lista detalhada |
| `403 Forbidden` | Sem permissão (não é admin/owner) |
| `404 Not Found` | Projeto não encontrado |

Exemplo de resposta 409:
```json
{
  "errors": [
    "Questionário 'Q1' já possui resultado ISEP. Não pode ser removido.",
    "Representante 'Gabriel' possui resposta COMPLETED no questionário id=5 sem resultado ISEP. Não pode ser removido."
  ]
}
```

---

## Fluxo Recomendado para o Frontend

### 1. Tela de Edição de Projeto (CASCATA e ITERATIVO)

A tela de edição deve funcionar para **AMBOS** os tipos de projeto. Ao abrir:

1. **Carregar snapshot** via `GET /api/projects/{projectId}/edit`
2. **Verificar `editable`** — se `false`, exibir a `editableReason` e desabilitar edição
3. **Popular formulários** com os dados do snapshot:
   - **CASCATA:** Mostrar etapas (stages) + questionários vinculados a etapas
   - **ITERATIVO:** Mostrar iterações (iterations) + questionários vinculados a iterações
   - **Ambos:** Representantes com suas roles e questionários com perguntas
4. **Itens com `locked: true`:** Exibir em modo somente-leitura com ícone de cadeado e tooltip com `lockReason`. **Não permitir remoção.**
5. **Cada item DEVE manter o campo `id`** no envio para o PUT

### 2. Componente Angular — Usar o mesmo para ambos os tipos

```
@if (projectType() === ProjectType.Cascata) {
    <app-edit-cascata-project-form></app-edit-cascata-project-form>
}
@if (projectType() === ProjectType.Iterativo) {
    <app-edit-iterativo-project-form></app-edit-iterativo-project-form>
}
```

**Ambos os componentes devem:**
- Chamar `GET /api/projects/{projectId}/edit` no `ngOnInit`
- Usar o `ProjectEditSnapshotDTO` como modelo base
- Montar o `UpdateProjectRequestDTO` no submit
- Implementar dry run antes de salvar

### 3. Preview de Mudanças (Recomendado)

Antes de salvar:

1. Enviar `PUT /api/projects/{projectId}` com `"dryRun": true`
2. Exibir o `changesSummary` ao usuário com diálogo de confirmação
3. Se `blockedReasons` não estiver vazio, exibir em vermelho e bloquear ação

### 4. Confirmar e Salvar

1. Enviar `PUT /api/projects/{projectId}` com `"dryRun": false`
2. Exibir mensagem de sucesso com o resumo retornado

---

## Diferenças entre os tipos no formulário de edição

| Aspecto | CASCATA | ITERATIVO |
|---|---|---|
| Seção de etapas | ✅ Mostrar e permitir editar | ❌ Ocultar |
| Seção de iterações | ❌ Ocultar | ✅ Mostrar e permitir editar |
| Questionário.stageName/stageId | ✅ Usado para vincular à etapa | ❌ Ignorar |
| Questionário.iterationName/iterationId | ❌ Ignorar | ✅ Usado para vincular à iteração |
| iterationDuration / iterationCount | ❌ Ocultar | ✅ Mostrar e permitir editar |

---

## Regras de Diff (PUT)

| Condição | Ação |
|---|---|
| Item com `id: null` | **CRIAÇÃO** — novo registro será criado |
| Item com `id` existente | **ATUALIZAÇÃO** — campos alterados serão aplicados |
| Item existente no banco mas **ausente** no request | **REMOÇÃO** — será removido (sujeito a validação) |

## Modo Preview (Dry Run)

Envie `"dryRun": true` para receber o resumo de mudanças **sem aplicar nenhuma alteração**.

## Notificações Automáticas

| Evento | Destinatário | Canal |
|---|---|---|
| Representante adicionado | Novo representante | Email + Interno |
| Representante removido | Representante removido | Email + Interno |
| Novo usuário criado (email novo) | Novo usuário | Email |
| Email de representante alterado | Email antigo **E** novo | Email |

## Campos NÃO editáveis em projetos ABERTOS

- `type` (CASCATA / ITERATIVO) — **fixo após publicação**
- Qualquer campo de questionário com `hasIsepResult: true`
- Qualquer campo se `editable: false`

---

## Exemplos cURL

### Carregar Snapshot para Edição
```bash
curl -X GET http://localhost:8080/api/projects/1/edit \
  -H "Authorization: Bearer <TOKEN>"
```

### Preview (Dry Run)
```bash
curl -X PUT http://localhost:8080/api/projects/1 \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Novo Nome",
    "deadline": "2026-12-31",
    "dryRun": true,
    "stages": [{"id": 1, "name": "Iniciação", "weight": 0.25}],
    "questionnaires": [{"id": 5, "name": "Q1 Atualizado", "weight": 1}],
    "representatives": [{"id": 1, "weight": 1.0, "roleIds": [7,3]}]
  }'
```

### Aplicar Mudanças
```bash
curl -X PUT http://localhost:8080/api/projects/1 \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Novo Nome",
    "deadline": "2026-12-31",
    "dryRun": false,
    "stages": [{"id": 1, "name": "Iniciação", "weight": 0.25}],
    "questionnaires": [{"id": 5, "name": "Q1 Atualizado", "weight": 1}],
    "representatives": [{"id": 1, "weight": 1.0, "roleIds": [7,3]}]
  }'
```

---

## Troubleshooting

### Erro: `No static resource api/projects/{id}/edit`

**Causa:** O backend não foi recompilado após a adição dos novos endpoints. O Spring está tratando o path como recurso estático porque o controller não está no classpath atualizado.

**Solução:** Execute no backend:
```bash
mvn clean compile
```
Ou pela IDE, faça um **Rebuild Project**.

### Erro 403 Forbidden

**Causa:** O usuário autenticado não é admin nem owner do projeto.

**Solução:** Verificar que o token JWT pertence a um usuário com role ADMIN ou que é o owner (`owner_id`) do projeto.

---

## Implementação Obrigatória para o Frontend

### O que FALTA no frontend (conforme análise):

O componente `edit-project-page.component.html` possui um TODO pendente:

```html
<!-- ANTES (incompleto): -->
@if (projectType() === ProjectType.Cascata) {
    <app-edit-cascata-project-form></app-edit-cascata-project-form>
}
@if (projectType() === ProjectType.Iterativo) {
    <!-- TODO: Implementar EditIterativoProjectFormComponent quando necessário -->
    <p>Edição de projetos iterativos ainda não implementada.</p>
}
```

**O backend JÁ SUPORTA ambos os tipos de projeto no mesmo endpoint.** O frontend DEVE implementar o `EditIterativoProjectFormComponent` com a mesma lógica do Cascata, porém:

1. **Ocultar** a seção de `stages` (etapas)
2. **Mostrar** a seção de `iterations` (iterações) com campos: `name`, `weight`, `applicationStartDate`, `applicationEndDate`
3. **Mostrar** campos `iterationDuration` e `iterationCount` no formulário principal
4. Vincular questionários a `iterationName` / `iterationId` em vez de `stageName` / `stageId`

### Componente `EditIterativoProjectFormComponent` deve:

1. Injetar `ActivatedRoute` para obter `projectId`
2. Chamar `GET /api/projects/{projectId}/edit` no `ngOnInit()`
3. Popular o formulário com os dados retornados
4. Respeitar campos `locked` (desabilitar remoção, mostrar ícone cadeado)
5. No submit, montar o `UpdateProjectRequestDTO` com diff:
   - Itens com `id: null` → criação
   - Itens existentes no banco mas ausentes no request → remoção
   - Itens com `id` presente → atualização
6. Implementar dry run (`dryRun: true`) antes de aplicar mudanças
