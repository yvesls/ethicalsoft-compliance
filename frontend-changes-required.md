# Ajustes necessários no frontend — classificação de perguntas

## Contexto

O backend passou a suportar o campo `classification` em perguntas de template e de projeto. Esse campo implementa a divisão em **Tipo A / B / C** descrita na documentação do TCC:

| Valor do enum | Significado |
|---|---|
| `PROJETO_INTEIRO` | Pergunta de governança estrutural — aparece apenas na Sprint 1 |
| `BASE_ITERACAO` | Checkpoint recorrente — aparece antes de cada sprint |
| `ROTATIVA` | Pergunta específica do foco da sprint |

---

## O que mudou na API

### 1. `GET /templates/{id}` — carregamento de template

O objeto `TemplateQuestionDTO` dentro de `questionnaires[].questions[]` agora retorna um campo extra:

```json
{
  "value": "Foi aprovado o uso de IA...",
  "type": "BASE",
  "classification": "PROJETO_INTEIRO",
  "stageName": "Iniciação",
  "stages": [...],
  "roles": [...]
}
```

**Valores possíveis de `classification`:** `PROJETO_INTEIRO` | `BASE_ITERACAO` | `ROTATIVA` | `null` (perguntas customizadas criadas antes dessa mudança)

---

### 2. `POST /projects` — criação de projeto a partir de template

O objeto `QuestionDTO` dentro de `questionnaires[].questions[]` no corpo da requisição agora aceita o campo `classification`. O backend persiste o valor recebido.

```json
{
  "questionnaires": [
    {
      "name": "Sprint 1",
      "iterationName": "Sprint 1",
      "questions": [
        {
          "value": "Foi aprovado o uso de IA...",
          "type": "BASE",
          "classification": "PROJETO_INTEIRO",
          "roleIds": [2, 3, 9],
          "stageNames": ["Iniciação"]
        }
      ]
    }
  ]
}
```

**Ação necessária:** ao montar o payload de criação de projeto a partir de um template, propagar o campo `classification` de cada pergunta do template para o `QuestionDTO` correspondente.

---

## O que o frontend precisa fazer

### Obrigatório

- [ ] **Propagar `classification` no mapeamento template → projeto**: ao converter `TemplateQuestionDTO` em `QuestionDTO` para o payload de `POST /projects`, incluir `classification` no objeto enviado.

### Recomendado (UX)

- [ ] **Exibir a classificação na interface de template**: ao visualizar um template, mostrar um badge ou indicador junto a cada pergunta indicando se ela é `Projeto inteiro`, `Base por iteração` ou `Rotativa`. Isso ajuda o gestor a entender o papel de cada pergunta antes de criar o projeto.

- [ ] **Exibir a classificação no questionário do projeto**: ao listar perguntas de um questionário aberto, indicar visualmente a classificação da pergunta (especialmente útil para diferenciar as perguntas de checkpoint recorrente das rotativas da sprint).

---

## Enum completo (referência)

```
PROJETO_INTEIRO  →  "Projeto inteiro"
BASE_ITERACAO    →  "Base por iteração"
ROTATIVA         →  "Rotativa por sprint"
```

---

## Compatibilidade

- Projetos e templates existentes antes dessa mudança terão `classification: null` nas perguntas — o frontend deve tratar `null` como ausência de classificação e não quebrar a renderização.
- O cálculo de ISEQ **não foi alterado** — nenhuma tela de resultado precisa de ajuste.
