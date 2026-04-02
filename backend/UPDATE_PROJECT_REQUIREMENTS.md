# Levantamento de Requisitos – Rota de Atualização de Projeto (`PUT /api/projects/{projectId}`)

## 1. Contexto e Objetivo

A rota de atualização de projetos publicados (`status = ABERTO`) deve permitir que o administrador do projeto modifique metadados, representantes, questionários, perguntas, etapas e iterações **de forma controlada**, respeitando o ciclo de vida dos dados e a integridade dos cálculos ISEP.

> **Nota:** A rota `PUT /{projectId}/draft` já existe para projetos `RASCUNHO`. Esta especificação trata exclusivamente de projetos `ABERTO` (já publicados e potencialmente em uso).

---

## 2. Modelo de Dados Atual (Resumo Relacional)

```
Project (PG)
├── status: RASCUNHO | ABERTO | CONCLUIDO | ARQUIVADO
├── timelineStatus: PENDENTE | EM_ANDAMENTO | ATRASADO | CONCLUIDO
├── type: CASCATA | ITERATIVO_INCREMENTAL
├── owner → User
├── stages[] → Stage (name, weight, sequence, status, datas)
├── iterations[] → Iteration (name, weight, status, datas)
├── questionnaires[] → Questionnaire
│   ├── stage → Stage (Cascata)  OU  iterationRef → Iteration (Iterativo)
│   ├── status: PENDENTE | EM_ANDAMENTO | ATRASADO | CONCLUIDO
│   ├── weight, applicationStartDate, applicationEndDate
│   └── questions[] → Question
│       ├── text (value)
│       ├── roles[] → Role (M:N via question_role)
│       └── stages[] → Stage (M:N via question_stage)
├── representatives[] → Representative
│   ├── user → User (email, firstName, lastName)
│   ├── roles[] → Role (M:N via representative_roles)
│   ├── weight
│   └── creationDate, updateDate, deletionDate
├── QuestionnaireResult (PG) – resultado ISEP de 1 questionário
└── ProjectIsepResult (PG) – resultado ISEP consolidado do projeto

QuestionnaireResponse (MongoDB: questionnaire_responses)
├── projectId, questionnaireId, representativeId, stageId
├── status: PENDING | IN_PROGRESS | COMPLETED
├── submissionDate
└── answers[] → AnswerDocument
    ├── questionId, questionText, stageIds, roleIds
    ├── response (Boolean), justification, evidence, attachments
```

---

## 3. Classificação de Campos por Nível de Impacto

### 3.1. Impacto BAIXO (aplicável sem invalidação de respostas)

| Campo | Entidade | Observação |
|---|---|---|
| `name` | Project | Apenas texto descritivo |
| `deadline` | Project | Somente extensão (para frente), nunca para antes de `hoje` |
| `startDate` | Project | Somente se projeto ainda não teve questionário `EM_ANDAMENTO` |
| `name` (etapa) | Stage | Renomear etapa não altera perguntas |
| `weight` | Stage | Altera cálculo futuro, não invalida respostas já coletadas |
| `name` (iteração) | Iteration | Renomear iteração não altera perguntas |
| `weight` | Iteration | Altera cálculo futuro, não invalida respostas já coletadas |
| `name` (questionário) | Questionnaire | Apenas texto descritivo |
| `weight` | Questionnaire | Altera cálculo futuro |
| `description`, `domain` | Questionnaire | Metadados descritivos |
| `weight` | Representative | Altera cálculo ISEP futuro, não invalida resposta |
| `currentSituation` | Project | Campo informativo |

**Regra:** Aplicar diretamente no banco. Nenhuma resposta é invalidada. Notificações opcionais (informativa para representantes sobre renomeação).

### 3.2. Impacto MÉDIO (exige recriação/atualização de respostas MongoDB)

| Mudança | Entidade | Impacto |
|---|---|---|
| Adicionar representante | Representative | Criar `QuestionnaireResponse` para todos os questionários `PENDENTE`/`EM_ANDAMENTO` |
| Remover representante | Representative | Deletar respostas `PENDING`; bloquear se `COMPLETED` em questionário sem resultado ISEP |
| Alterar roles do representante | Representative | Recalcular perguntas visíveis; adicionar/remover `AnswerDocument`s em respostas `PENDING`/`IN_PROGRESS` |
| Alterar email do representante | Representative → User | Resolver/criar novo User; enviar credenciais; atualizar vínculo |
| Adicionar pergunta ao questionário | Question | Adicionar `AnswerDocument` em todas respostas `PENDING`/`IN_PROGRESS` de representantes com role correspondente |
| Remover pergunta do questionário | Question | Remover `AnswerDocument` de respostas `PENDING`/`IN_PROGRESS`; bloquear se questionário `CONCLUIDO` |
| Alterar roles da pergunta | Question | Recalcular quem deve ver a pergunta; adicionar/remover `AnswerDocument` |
| Alterar texto da pergunta | Question | Atualizar `questionText` em todos `AnswerDocument`s (apenas texto, não invalida resposta) |
| Alterar datas do questionário | Questionnaire | `applicationEndDate` para frente: OK; para trás: bloquear se antes de `hoje` |
| Adicionar questionário | Questionnaire | Criar estrutura completa + respostas MongoDB para todos representantes |
| Remover questionário | Questionnaire | Somente se `PENDENTE` e sem respostas `IN_PROGRESS`/`COMPLETED` |

### 3.3. Impacto BLOQUEADO (operações proibidas)

| Operação | Razão |
|---|---|
| Qualquer mudança em questionário `CONCLUIDO` (com `QuestionnaireResult`) | Resultado ISEP já calculado; integridade estatística |
| Qualquer mudança se `ProjectIsepResult` existe | Projeto já finalizado |
| Alterar `type` do projeto (CASCATA ↔ ITERATIVO) | Mudança estrutural radical; exigiria destruir toda estrutura |
| Reduzir `deadline` para antes de `hoje` | Inconsistência temporal |
| Reduzir `applicationEndDate` do questionário para antes de `hoje` | Inconsistência temporal |
| Remover representante com resposta `COMPLETED` em questionário sem resultado | Perda de dados de resposta |
| Remover/alterar pergunta em questionário `CONCLUIDO` | Resultado já calculado |
| Adicionar/remover pergunta em questionário `EM_ANDAMENTO` com respostas `COMPLETED` | Invalidaria respostas já submetidas |
| Remover questionário com `QuestionnaireResult` | Resultado ISEP depende dele |
| Remover etapa/iteração com questionários vinculados que não sejam `PENDENTE` | Dependência estrutural |

---

## 4. Fluxo de Detecção de Mudanças (Diff Engine)

### 4.1. Necessidade de IDs nos DTOs

Os DTOs atuais (`QuestionnaireDTO`, `QuestionDTO`, `RepresentativeDTO`, `StageDTO`, `IterationDTO`) **não possuem campo `id`**. Para atualização, é obrigatório incluir:

| DTO | Campo necessário | Tipo |
|---|---|---|
| `QuestionnaireDTO` | `id` | `Integer` (null = novo) |
| `QuestionDTO` | `id` | `Integer` (null = nova) |
| `RepresentativeDTO` | `id` | `Long` (null = novo) |
| `StageDTO` | `id` | `Integer` (null = nova) |
| `IterationDTO` | `id` | `Integer` (null = nova) |

**Regra de diff:**
- Item com `id = null` → **CRIAÇÃO**
- Item com `id` presente no banco → **ATUALIZAÇÃO** (comparar campos)
- Item presente no banco mas ausente no request → **REMOÇÃO**

### 4.2. Algoritmo de Diff (por entidade)

```
Para cada entidade (Stage, Iteration, Questionnaire, Question, Representative):
  1. Mapear entidades existentes por ID
  2. Separar itens do request em:
     - NOVOS (id == null)
     - ATUALIZADOS (id != null && existe no banco)
     - REMOVIDOS (existem no banco mas ausentes no request)
  3. Para cada ATUALIZADO, comparar campo-a-campo
  4. Classificar cada mudança por nível de impacto (Baixo/Médio/Bloqueado)
  5. Se qualquer mudança for BLOQUEADA → rejeitar request inteiro com lista de erros
  6. Aplicar mudanças de Baixo impacto
  7. Aplicar mudanças de Médio impacto (com efeitos colaterais em MongoDB)
```

---

## 5. Matriz de Validação por Estado do Questionário

| Estado do Questionário | Adicionar Pergunta | Remover Pergunta | Alterar Texto | Alterar Roles Pergunta | Alterar Peso | Alterar Datas | Adicionar Rep. | Remover Rep. |
|---|---|---|---|---|---|---|---|---|
| `PENDENTE` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ (se PENDING) |
| `EM_ANDAMENTO` | ⚠️¹ | ⚠️² | ✅³ | ⚠️⁴ | ✅ | ✅⁵ | ✅ | ⚠️⁶ |
| `ATRASADO` | ⚠️¹ | ⚠️² | ✅³ | ⚠️⁴ | ✅ | ✅⁵ | ✅ | ⚠️⁶ |
| `CONCLUIDO` (com Result) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

**Notas:**
1. ⚠️¹ Adicionar pergunta em questionário `EM_ANDAMENTO`: Permitido **apenas** se nenhum representante com a role correspondente já tem resposta `COMPLETED`. Adiciona `AnswerDocument` nas respostas `PENDING`/`IN_PROGRESS`.
2. ⚠️² Remover pergunta: Permitido **apenas** se nenhuma resposta `COMPLETED` contém a pergunta respondida (response != null).
3. ✅³ Alterar texto: Sempre permitido (apenas atualiza `questionText` no `AnswerDocument`).
4. ⚠️⁴ Alterar roles da pergunta: Pode adicionar novos representantes que devem ver a pergunta ou remover acesso. Bloquear se remover acesso de representante que já respondeu.
5. ✅⁵ Alterar datas: `applicationEndDate` somente para frente; `applicationStartDate` somente se nenhuma resposta `IN_PROGRESS`/`COMPLETED` existe.
6. ⚠️⁶ Remover representante: Bloquear se tem resposta `COMPLETED` neste questionário.

---

## 6. Impactos por Tipo de Projeto

### 6.1. Projeto Cascata
- 1 questionário por etapa
- Alterar etapa = potencialmente altera o questionário vinculado
- Adicionar nova etapa = deve criar novo questionário automaticamente (ou manual)
- Remover etapa = remove questionário (se permitido)
- Renomear etapa → impacto baixo no questionário
- Alterar peso da etapa → altera `Peso_Etapa` no cálculo ISEP_Projeto (Nível 3)

### 6.2. Projeto Iterativo Incremental
- 1 questionário por iteração (com todas as etapas dentro)
- Alterar iteração = potencialmente altera o questionário vinculado
- Adicionar nova iteração = deve criar novo questionário automaticamente
- Remover iteração = remove questionário (se permitido)
- Alterar peso da iteração → altera `Peso_Iteração` no cálculo ISEP_Projeto (Nível 3)
- Alterar etapas → altera `Peso_Etapa` no cálculo ICP (Nível 1) de iterações futuras

---

## 7. Política de Notificações

### 7.1. Notificações obrigatórias

| Evento | Destinatários | Template | Canal |
|---|---|---|---|
| Representante adicionado ao projeto | Novo representante | `PROJECT_ASSIGNMENT` (existente) | Email + Interno |
| Representante removido do projeto | Representante removido | **`PROJECT_UNASSIGNMENT` (NOVO)** | Email + Interno |
| Novo usuário criado (email novo) | Novo usuário | `NEW_USER_CREDENTIALS` (existente) | Email |
| Pergunta adicionada em questionário EM_ANDAMENTO | Representantes afetados (roles) | **`QUESTIONNAIRE_UPDATED` (NOVO)** | Email + Interno |
| Pergunta removida de questionário EM_ANDAMENTO | Representantes que tinham a pergunta | **`QUESTIONNAIRE_UPDATED` (NOVO)** | Email + Interno |
| Roles de representante alterados | Representante afetado | **`REPRESENTATIVE_ROLES_UPDATED` (NOVO)** | Email + Interno |
| Datas do questionário alteradas | Todos representantes do questionário | **`QUESTIONNAIRE_DATES_UPDATED` (NOVO)** | Email + Interno |
| Email do representante alterado | Email antigo E email novo | **`REPRESENTATIVE_EMAIL_CHANGED` (NOVO)** | Email |
| Deadline do projeto estendido | Admin + todos representantes | **`PROJECT_DEADLINE_UPDATED` (NOVO)** | Interno |
| Questionário adicionado | Todos representantes do projeto | `QUESTIONNAIRE_REMINDER` (existente) | Email + Interno |
| Questionário removido | Todos representantes do projeto | **`QUESTIONNAIRE_REMOVED` (NOVO)** | Interno |

### 7.2. Novos Templates Necessários

| Key | Título (sugestão) | Corpo (variáveis) |
|---|---|---|
| `PROJECT_UNASSIGNMENT` | "Você foi removido do projeto {projectName}" | projectName, representativeName |
| `QUESTIONNAIRE_UPDATED` | "O questionário '{questionnaireName}' foi atualizado" | projectName, questionnaireName, changeDescription |
| `REPRESENTATIVE_ROLES_UPDATED` | "Seus papéis no projeto '{projectName}' foram atualizados" | projectName, oldRoles, newRoles |
| `QUESTIONNAIRE_DATES_UPDATED` | "As datas do questionário '{questionnaireName}' foram alteradas" | projectName, questionnaireName, newStartDate, newEndDate |
| `REPRESENTATIVE_EMAIL_CHANGED` | "Seu email de acesso ao projeto foi alterado" | projectName, oldEmail, newEmail |
| `PROJECT_DEADLINE_UPDATED` | "O prazo do projeto '{projectName}' foi estendido" | projectName, oldDeadline, newDeadline |
| `QUESTIONNAIRE_REMOVED` | "O questionário '{questionnaireName}' foi removido" | projectName, questionnaireName |

---

## 8. Fluxo Detalhado da Alteração de Email de Representante

```
1. Admin envia request com representante id=X, email="novo@email.com"
2. Sistema busca Representative(id=X) → user.email = "antigo@email.com"
3. Se email diferente:
   a. Buscar User com email "novo@email.com" no banco
   b. SE existe:
      - Atualizar representative.user = User encontrado
   c. SE NÃO existe:
      - Criar novo User (firstName, lastName do request, email novo)
      - Gerar senha temporária
      - Enviar NEW_USER_CREDENTIALS para "novo@email.com"
   d. Enviar REPRESENTATIVE_EMAIL_CHANGED para "antigo@email.com" (informando a remoção)
   e. Enviar REPRESENTATIVE_EMAIL_CHANGED para "novo@email.com" (informando o novo acesso)
   f. Atualizar representative.updateDate = hoje
4. As respostas MongoDB ficam vinculadas por representativeId (não userId), então NÃO precisam ser alteradas
```

> **Importante:** O `QuestionnaireResponse` no MongoDB é vinculado por `representativeId`, não por `userId`. Portanto, trocar o email (User) do representante NÃO afeta as respostas existentes.

---

## 9. Efeitos Colaterais em MongoDB (por operação)

### 9.1. Adicionar Representante
```
Para cada Questionnaire Q do projeto onde Q.status IN (PENDENTE, EM_ANDAMENTO):
  - Buscar perguntas de Q onde question.roles ∩ representative.roles ≠ ∅
  - Se há perguntas aplicáveis:
    - Criar QuestionnaireResponse com answers[] = AnswerDocument[] para cada pergunta
    - status = PENDING
```

### 9.2. Remover Representante
```
Para cada QuestionnaireResponse R onde R.representativeId = rep.id:
  - Se R.status == COMPLETED E Questionnaire(R.questionnaireId).status != CONCLUIDO:
    - BLOQUEAR (resposta já submetida, seria perdida)
  - Se R.status == COMPLETED E existe QuestionnaireResult para R.questionnaireId:
    - BLOQUEAR (resultado ISEP já calculado com essa resposta)
  - Caso contrário (PENDING ou IN_PROGRESS):
    - DELETE do documento MongoDB
```

### 9.3. Adicionar Pergunta
```
Para cada QuestionnaireResponse R do questionário Q:
  - Buscar Representative(R.representativeId)
  - Se representative.roles ∩ novaPergunta.roles ≠ ∅:
    - Se R.status == COMPLETED:
      - BLOQUEAR (representante já finalizou)
    - Adicionar AnswerDocument à lista R.answers
    - Se R.status == PENDING, manter PENDING
    - Se R.status == IN_PROGRESS, manter IN_PROGRESS
```

### 9.4. Remover Pergunta
```
Para cada QuestionnaireResponse R do questionário Q:
  - Buscar AnswerDocument com questionId == pergunta.id
  - Se encontrado E R.status == COMPLETED:
    - BLOQUEAR
  - Se encontrado E answer.response != null (já respondida, mas não submitada):
    - AVISO (perda de resposta parcial)
  - Remover AnswerDocument de R.answers
```

### 9.5. Alterar Roles da Pergunta
```
Para cada QuestionnaireResponse R do questionário Q:
  - Buscar Representative(R.representativeId)
  - rolesBefore = roles antigos da pergunta
  - rolesAfter = roles novos da pergunta
  
  - Se representative.roles ∩ rolesBefore ≠ ∅ E representative.roles ∩ rolesAfter == ∅:
    - Representante PERDE acesso à pergunta
    - Se R.status == COMPLETED → BLOQUEAR
    - Se answer.response != null → AVISO
    - Remover AnswerDocument
    
  - Se representative.roles ∩ rolesBefore == ∅ E representative.roles ∩ rolesAfter ≠ ∅:
    - Representante GANHA acesso à pergunta
    - Se R.status == COMPLETED → BLOQUEAR
    - Adicionar novo AnswerDocument
```

### 9.6. Alterar Roles do Representante
```
Para cada Questionnaire Q do projeto:
  - Para cada Question P de Q:
    - rolesAntes = rep.roles antigos
    - rolesDepois = rep.roles novos
    - perguntaRoles = P.roles
    
    - Antes via? = rolesAntes ∩ perguntaRoles ≠ ∅
    - Depois via? = rolesDepois ∩ perguntaRoles ≠ ∅
    
    - Se Antes E !Depois: rep perde acesso à pergunta P
      - Se resposta COMPLETED → BLOQUEAR
      - Remover AnswerDocument
    
    - Se !Antes E Depois: rep ganha acesso à pergunta P
      - Se resposta COMPLETED → BLOQUEAR
      - Adicionar AnswerDocument
```

---

## 10. Estrutura do Request DTO (proposta)

Como os DTOs atuais não possuem IDs, é necessário criar um **novo DTO de atualização** ou adicionar IDs aos existentes:

```java
@Data
public class UpdateProjectRequestDTO {
    // --- Campos de projeto ---
    private String name;
    private LocalDate startDate;
    private LocalDate deadline;
    // type NÃO é editável em projeto ABERTO
    
    // --- Entidades com IDs para diff ---
    private List<UpdateStageDTO> stages;
    private Set<UpdateIterationDTO> iterations;
    private Set<UpdateQuestionnaireDTO> questionnaires;
    private Set<UpdateRepresentativeDTO> representatives;
}

@Data
public class UpdateStageDTO {
    private Integer id;          // null = nova etapa
    private String name;
    private BigDecimal weight;
}

@Data
public class UpdateIterationDTO {
    private Integer id;          // null = nova iteração
    private String name;
    private BigDecimal weight;
    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
}

@Data
public class UpdateQuestionnaireDTO {
    private Integer id;          // null = novo questionário
    private String name;
    private Integer weight;
    private String stageName;    // ou stageId
    private String iterationName; // ou iterationId
    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
    private String domain;
    private String description;
    private Set<UpdateQuestionDTO> questions;
}

@Data
public class UpdateQuestionDTO {
    private Integer id;          // null = nova pergunta
    private String value;        // texto da pergunta
    private Set<Long> roleIds;
    private List<String> stageNames; // categorias da pergunta
}

@Data
public class UpdateRepresentativeDTO {
    private Long id;             // null = novo representante
    private String firstName;
    private String lastName;
    private String email;
    private Set<Long> roleIds;
    private BigDecimal weight;
}
```

---

## 11. Estrutura do Response DTO (proposta)

```java
@Data
public class UpdateProjectResponseDTO {
    // Estado atual do projeto
    private ProjectResponseDTO project;
    
    // Resumo de mudanças aplicadas
    private ChangesSummaryDTO changesSummary;
}

@Data
public class ChangesSummaryDTO {
    private int stagesAdded;
    private int stagesRemoved;
    private int stagesUpdated;
    
    private int iterationsAdded;
    private int iterationsRemoved;
    private int iterationsUpdated;
    
    private int questionnairesAdded;
    private int questionnairesRemoved;
    private int questionnairesUpdated;
    
    private int questionsAdded;
    private int questionsRemoved;
    private int questionsUpdated;
    
    private int representativesAdded;
    private int representativesRemoved;
    private int representativesUpdated;
    
    private int responsesCreated;    // novas respostas MongoDB
    private int responsesUpdated;    // respostas com perguntas add/removed
    private int responsesDeleted;    // respostas removidas (rep removido)
    
    private int notificationsSent;
    
    private List<String> warnings;   // avisos sobre impactos
}
```

---

## 12. Arquitetura da Implementação

### 12.1. Classes a criar

| Classe | Camada | Responsabilidade |
|---|---|---|
| `UpdateProjectUseCase` | application/usecase/project | Orquestrador principal |
| `ProjectUpdateDiffService` | domain/service | Motor de diff: detecta mudanças por entidade |
| `ProjectUpdateValidationPolicy` | domain/service | Validações de bloqueio por estado |
| `ProjectUpdateResponseSyncService` | application/service | Sincronização das respostas MongoDB |
| `UpdateProjectRequestDTO` | dto/request | Request com IDs |
| `UpdateProjectResponseDTO` | dto/response | Response com resumo de mudanças |
| `UpdateStageDTO` ... | dto | DTOs de atualização com IDs |
| Novos `NotificationType` entries | domain/notification | Novos tipos de evento |
| Novos templates MongoDB | infra/bootstrap | Templates das notificações |
| Novas strategies de notificação | application/service/strategy/notification | Handlers dos novos tipos |

### 12.2. Fluxo de execução do `UpdateProjectUseCase`

```
1. Validar que projeto existe e status == ABERTO
2. Validar que NÃO existe ProjectIsepResult
3. Carregar estado completo (projeto + questionários + perguntas + representantes + respostas MongoDB)
4. Executar ProjectUpdateDiffService → gerar lista de ChangeSets
5. Executar ProjectUpdateValidationPolicy sobre cada ChangeSet → aprovar ou rejeitar
6. Se há bloqueios → lançar BusinessException com detalhes
7. Aplicar mudanças de baixo impacto (diretamente no PG)
8. Aplicar mudanças de médio impacto:
   a. Atualizar PG (questionnaires, questions, representatives)
   b. Executar ProjectUpdateResponseSyncService → sincronizar MongoDB
9. Recalcular timeline do projeto (ProjectTimelineStatusPolicy)
10. Coletar notificações a enviar
11. Commit da transação
12. Enviar notificações (afterCommit)
13. Retornar UpdateProjectResponseDTO com resumo
```

---

## 13. Casos de Teste para Validação

### 13.1. Cenários que DEVEM ser permitidos

| # | Cenário | Estado Questionário |
|---|---|---|
| 1 | Renomear projeto | Qualquer |
| 2 | Estender deadline | Qualquer |
| 3 | Alterar peso de etapa | Qualquer |
| 4 | Alterar peso de representante | Qualquer |
| 5 | Adicionar representante (roles compatíveis) | PENDENTE/EM_ANDAMENTO |
| 6 | Adicionar pergunta ao questionário | PENDENTE |
| 7 | Remover representante sem respostas | PENDENTE |
| 8 | Alterar texto de pergunta | Qualquer (não CONCLUIDO) |
| 9 | Adicionar novo questionário | N/A (novo) |
| 10 | Remover questionário PENDENTE sem respostas | PENDENTE |

### 13.2. Cenários que DEVEM ser bloqueados

| # | Cenário | Razão |
|---|---|---|
| 1 | Qualquer mudança em projeto CONCLUIDO | ProjectIsepResult existe |
| 2 | Adicionar pergunta em questionário CONCLUIDO | Resultado calculado |
| 3 | Remover representante com resposta COMPLETED | Perda de dados |
| 4 | Alterar tipo do projeto | Mudança estrutural radical |
| 5 | Reduzir deadline para antes de hoje | Inconsistência temporal |
| 6 | Remover questionário com QuestionnaireResult | Integridade do ISEP |
| 7 | Adicionar pergunta quando representante com role já tem resposta COMPLETED | Invalidaria submissão |
| 8 | Alterar roles removendo de representante que já respondeu | Perda de dados |

### 13.3. Cenários com WARNING (permitido mas com aviso)

| # | Cenário | Aviso |
|---|---|---|
| 1 | Remover pergunta com resposta parcial (IN_PROGRESS) | "Respostas parciais de X representantes serão perdidas" |
| 2 | Alterar roles de representante com respostas IN_PROGRESS | "Perguntas X serão removidas da resposta parcial de Y" |
| 3 | Alterar datas encurtando para próximo de hoje | "Representantes terão menos tempo para responder" |

---

## 14. Endpoint REST

```
PUT /api/projects/{projectId}
Authorization: Bearer {token}  (Admin/Owner do projeto)
Content-Type: application/json

Request Body: UpdateProjectRequestDTO
Response: 200 OK → UpdateProjectResponseDTO
          400 Bad Request → erros de validação simples
          409 Conflict → operações bloqueadas (com lista detalhada)
          403 Forbidden → sem permissão
          404 Not Found → projeto não encontrado
```

---

## 15. Estimativa de Complexidade

| Componente | Complexidade | Observação |
|---|---|---|
| DTOs de update (com IDs) | Baixa | Criação direta |
| Diff engine (ProjectUpdateDiffService) | **Alta** | Comparação multi-nível com relações M:N |
| Validação por estado (ProjectUpdateValidationPolicy) | **Alta** | Matriz de regras por estado × tipo de mudança |
| Sincronização MongoDB (ProjectUpdateResponseSyncService) | **Alta** | CRUD granular em documentos embedded |
| Notificações | Média | Reuso do padrão existente + novos templates |
| Controller + UseCase | Média | Orquestração padrão |
| Testes | **Alta** | Muitos cenários de borda |

---

## 16. Riscos e Decisões Pendentes

1. **Concorrência:** Um representante pode estar respondendo enquanto o admin edita. Recomenda-se `@Version` (optimistic locking) no `Project` e tratamento de `OptimisticLockException`.

2. **Atomicidade PG + MongoDB:** Transações distribuídas não são triviais. Recomenda-se aplicar mudanças no PG primeiro (com @Transactional) e MongoDB na sequência. Se MongoDB falhar, logar erro e permitir retry manual.

3. **Granularidade do endpoint:** Endpoint único que aceita o projeto completo (full replacement com diff) OU endpoints separados por entidade (PATCH granular)? Recomenda-se **endpoint único com diff**, pois simplifica o frontend e permite validação cruzada.

4. **Soft delete vs hard delete de representantes:** Atualmente `Representative` tem `deletionDate`. Na remoção, fazer soft delete (setar `deletionDate`) e manter as respostas MongoDB para auditoria? Ou hard delete? Recomenda-se **soft delete** + manter respostas `COMPLETED` para histórico.

5. **Confirmação do frontend:** Para operações de médio impacto (ex: remover pergunta com respostas parciais), o frontend deveria receber um "preview" das mudanças e pedir confirmação? Ou o endpoint rejeita diretamente? Recomenda-se **modo preview** via query param `?dryRun=true` que retorna o `ChangesSummaryDTO` sem aplicar.

