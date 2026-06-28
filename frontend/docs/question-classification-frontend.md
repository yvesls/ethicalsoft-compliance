# Classificação de Perguntas — Ajustes de Frontend

## Modelo de dados

Cada pergunta possui dois atributos independentes:

| Atributo | Enum | Valores possíveis |
|---|---|---|
| `type` | `QuestionType` | `BASE`, `CUSTOM` |
| `classification` | `QuestionClassificationType` | `WHOLE_PROJECT`, `RECURRING`, `CURRENT_STAGE`, `null` |

### QuestionType

Determina a **origem** e o grau de imutabilidade da pergunta.

- `BASE` — criada pelo template inicial do sistema. Texto e exclusão **bloqueados** para o usuário. Apenas vínculos de papel e etapa podem ser ajustados.
- `CUSTOM` — criada pelo usuário. Pode ser editada e excluída livremente.

> O `type` é atribuído automaticamente pela origem: toda pergunta do template inicial é `BASE`; toda pergunta criada pelo usuário é `CUSTOM`.

### QuestionClassificationType

Determina o **escopo de exibição** da pergunta dentro do projeto.

| Valor | Label (PT) | Cor do badge | Descrição |
|---|---|---|---|
| `WHOLE_PROJECT` | Projeto inteiro | Azul | Exibida apenas na primeira sprint/fase |
| `RECURRING` | Recorrente | Verde | Aparece em todas as sprints/fases |
| `CURRENT_STAGE` | Etapa atual | Laranja | Específica desta sprint/etapa (sempre CUSTOM) |
| `null` | — | — | Sem escopo especial; comporta-se como CURRENT_STAGE |

### Combinações válidas

| type | classification | Comportamento |
|---|---|---|
| `BASE` | `WHOLE_PROJECT` | Imutável; exibida só na primeira sprint |
| `BASE` | `RECURRING` | Imutável; exibida em todas as sprints |
| `CUSTOM` | `WHOLE_PROJECT` | Editável; exibida só na primeira sprint |
| `CUSTOM` | `RECURRING` | Editável; exibida em todas as sprints |
| `CUSTOM` | `CURRENT_STAGE` | Editável; exibida só na sprint atual |
| `CUSTOM` | `null` | Editável; sem distribuição automática |

> `BASE` + `CURRENT_STAGE` é inválido e não deve ocorrer.

---

## Arquivos alterados

### Enum

- [`src/app/shared/enums/question-classification-type.enum.ts`](../src/app/shared/enums/question-classification-type.enum.ts)
  - Renomeados para inglês: `WholeProject`, `Recurring`, `CurrentStage`
  - Removido `BaseIteracao` (não é mais um valor de classificação; o comportamento "base por iteração" é dado pela combinação `RECURRING + BASE`)

### Lógica de distribuição de perguntas do template

- [`iterativo-project-form.component.ts`](../src/app/features/projects/components/iterativo-project-form/iterativo-project-form.component.ts) — `getTemplateQuestionsForIteration`
- [`cascata-project-form.component.ts`](../src/app/features/projects/components/cascata-project-form/cascata-project-form.component.ts) — `getQuestionsForQuestionnaire`

Regra de filtragem:
```
WHOLE_PROJECT → somente iterationIndex === 0
RECURRING     → todas as iterações
CURRENT_STAGE ou null → somente o questionário/etapa correspondente
```

### Badges nos formulários de questionário

- [`iterativo-questionnaire-form.component.html`](../src/app/features/projects/pages/iterativo-questionnaire-form/iterativo-questionnaire-form.component.html)
- [`cascata-questionnaire-form.component.html`](../src/app/features/projects/pages/cascata-questionnaire-form/cascata-questionnaire-form.component.html)

Badges são exibidos para **qualquer** classificação (não somente perguntas BASE). O cadeado de bloqueio permanece exclusivo de `type === BASE`.

Classes SCSS:
| Classificação | Classe | Cor |
|---|---|---|
| `WHOLE_PROJECT` | `.badge-whole-project` | `$blue-primary` |
| `RECURRING` | `.badge-recurring` | `$green-primary` |
| `CURRENT_STAGE` | `.badge-current-stage` | `$semantic-orange` |

### Seletor de classificação no modal de pergunta

- [`question-modal.component.ts`](../src/app/features/projects/components/question-modal/question-modal.component.ts)
- [`question-modal.component.html`](../src/app/features/projects/components/question-modal/question-modal.component.html)

O campo `classification` foi adicionado ao formulário reativo. O seletor (radio buttons) aparece somente quando `textReadOnly === false`, ou seja, somente para perguntas CUSTOM. Para perguntas BASE, a classificação é herdada e não exibida como seletor.

Quando `RECURRING` é selecionado, um aviso informa o usuário que a pergunta será adicionada a todas as iterações.

### i18n

Arquivos: `pt-BR.json`, `en-US.json`, `es-ES.json`

Chaves adicionadas em `questionnaire.form.classification`:
- `whole_project`, `whole_project_hint`
- `recurring`, `recurring_hint`
- `current_stage`, `current_stage_hint`

Chaves adicionadas em `questionnaire.question_modal`:
- `classification_label`
- `classification_none`
- `classification_whole_project`
- `classification_recurring`
- `classification_current_stage`
- `recurring_hint`

Chaves removidas (substituídas):
- `base_badge`, `type_a_badge`, `type_a_locked` — substituídas pelas chaves de `classification`

---

## Responsabilidades do backend

As seguintes funcionalidades dependem do backend e **não** são gerenciadas pelo frontend:

- Distribuição automática de perguntas `RECURRING` criadas pelo usuário para todas as iterações existentes
- Validação de `BASE + CURRENT_STAGE` (combinação inválida)
- Pré-preenchimento de respostas `WHOLE_PROJECT` já respondidas em iterações anteriores
