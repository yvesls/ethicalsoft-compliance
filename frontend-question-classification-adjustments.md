# Frontend Adjustments: Question Classification System

This document describes all frontend changes required to support the new `QuestionClassificationEnum`
and `QuestionTypeEnum` system introduced in the backend refactoring.

---

## 1. Background: The New Model

Every questionnaire question now has two independent attributes:

| Attribute        | Possible values                                    | Who sets it           |
|------------------|----------------------------------------------------|-----------------------|
| `type`           | `BASE`, `CUSTOM`                                   | System (BASE = from template) |
| `classification` | `WHOLE_PROJECT`, `RECURRING`, `CURRENT_STAGE`      | System (template) or user (custom questions) |

**Valid combinations:**

| type   | classification    | Meaning                                                   |
|--------|-------------------|-----------------------------------------------------------|
| BASE   | WHOLE_PROJECT     | Template question answered once for the whole project     |
| BASE   | RECURRING         | Template question repeated in every iteration/sprint      |
| CUSTOM | WHOLE_PROJECT     | User question answered once for the whole project         |
| CUSTOM | RECURRING         | User question added to all existing and future iterations |
| CUSTOM | CURRENT_STAGE     | User question visible only in the current sprint/stage    |

> **Invalid:** `BASE + CURRENT_STAGE` — base template questions are never stage-specific.

---

## 2. What Changes by Feature Area

### 2.1 Creating / Editing Custom Questions

When a user creates or edits a custom question, show an **optional** classification selector.

**UI behaviour:**
- Label: "Escopo da pergunta"
- Options (radio or select):
  - "Projeto inteiro" (`WHOLE_PROJECT`) — answered once, applies to the whole project
  - "Recorrente" (`RECURRING`) — added to all iterations/sprints automatically
  - "Etapa atual" (`CURRENT_STAGE`) — visible only in the current sprint/stage
  - "Nenhum / padrão" (no classification) — treated as a plain custom question with no automatic behavior
- If none is selected, send `classification: null` to the backend.

**Constraints:**
- `CURRENT_STAGE` is only available when the user is inside a specific sprint/stage context.
  Hide it (or disable it) from the project-level question list.
- When the user selects `RECURRING`, show a confirmation: _"Esta pergunta será adicionada a todas as iterações existentes e futuras."_

---

### 2.2 BASE Questions — Immutable in UI

Questions with `type === 'BASE'` must be read-only:

- **No edit button** for BASE questions in the question list.
- **No delete button** for BASE questions.
- Optionally show a lock icon or a tooltip: _"Pergunta do template base — não pode ser editada."_
- Classification labels for BASE questions should still be shown (read-only), e.g., badge "Recorrente" or "Projeto inteiro".

---

### 2.3 Question List — Classification Labels

Show the classification as a badge/chip next to each question:

| `classification`  | Display label    | Suggested badge color |
|-------------------|------------------|-----------------------|
| `WHOLE_PROJECT`   | Projeto inteiro  | blue                  |
| `RECURRING`       | Recorrente       | green                 |
| `CURRENT_STAGE`   | Etapa atual      | orange                |
| `null`            | (no badge)       | —                     |

---

### 2.4 Sprint / Stage Question Loading

The backend filters questions per stage. The frontend should reflect this logic visually:

- **Sprint 1 (first iteration):** shows WHOLE_PROJECT + RECURRING + CURRENT_STAGE(sprint 1) questions.
- **Sprint N (subsequent):** shows RECURRING + CURRENT_STAGE(sprint N) questions.
  WHOLE_PROJECT questions are **not** repeated in subsequent sprints.
- CURRENT_STAGE questions are strictly local: they appear only in the sprint where they were created.

---

### 2.5 Questionnaire Submission — Classification-Aware Validation

When validating questionnaire completeness before submission:

- WHOLE_PROJECT questions that are already answered in a previous questionnaire of the same project
  should be considered pre-filled (do not require re-answering).
- RECURRING questions must be answered in every questionnaire/sprint.
- CURRENT_STAGE questions must be answered in the questionnaire they belong to.

---

## 3. `projectStatus` Integration (BUG-03)

`ProjectIsepDashboardDTO` now includes `projectStatus: string` (e.g., `'CONCLUIDO'`, `'EM_ANDAMENTO'`).

**Required guards:**

```typescript
// Disable certificate emission button when project is concluded
const canEmitCertificate = dashboard.projectStatus !== 'CONCLUIDO';

// Disable boletim emission button when project is concluded
const canEmitBulletin = dashboard.projectStatus !== 'CONCLUIDO';
```

Show a tooltip or notice when disabled: _"O projeto foi encerrado. Documentos não podem ser emitidos após o encerramento."_

---

## 4. API Contract Changes

### 4.1 Question DTO — New Fields

The question DTOs now include:

```typescript
interface Question {
  id: number;
  text: string;
  type: 'BASE' | 'CUSTOM';                              // new
  classification: 'WHOLE_PROJECT' | 'RECURRING' | 'CURRENT_STAGE' | null; // new
  // ... existing fields
}
```

### 4.2 Create/Update Question Payload

```typescript
interface CreateQuestionPayload {
  text: string;
  classification?: 'WHOLE_PROJECT' | 'RECURRING' | 'CURRENT_STAGE' | null;
  // ... existing fields
}
```

### 4.3 Register Emission Endpoints — Optional Code Params

Both emission registration endpoints now accept optional query params to pass back
the document code previewed by the user:

```
POST /api/projects/{projectId}/questionnaires/{questionnaireId}/bulletin/register-emission
  ?documentCode=BNC-XXXX-YYYY          (optional)

POST /api/projects/{projectId}/certificate/register-emission
  ?certificateCode=ESC-XXXX-YYYY       (optional)
```

**Why:** The PDF preview and the registration are separate requests. If the ISEP is
recalculated between preview and registration, the document codes diverge. The frontend
should capture the `documentCode` / `certificateCode` returned by the preview/generate
endpoint and pass it back when registering.

---

## 5. Summary of All Required Changes

| Area                        | Change                                                                         | Priority |
|-----------------------------|--------------------------------------------------------------------------------|----------|
| Question create/edit form   | Add optional "Escopo da pergunta" selector                                     | High     |
| Question list               | Show classification badge; hide edit/delete for BASE type                      | High     |
| Sprint question loading      | Filter by stage — WHOLE_PROJECT only on sprint 1, RECURRING on all             | High     |
| Dashboard / project page    | Guard emit buttons using `projectStatus !== 'CONCLUIDO'` (BUG-03)             | High     |
| Emission register endpoints | Pass `documentCode` / `certificateCode` back from preview to register call     | Medium   |
| Questionnaire validation    | Skip WHOLE_PROJECT questions already answered in a previous sprint              | Medium   |
| Question DTO types          | Update TypeScript interfaces to include `type` and `classification` fields     | Low      |
