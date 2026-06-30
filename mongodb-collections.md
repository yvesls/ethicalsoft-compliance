# Collections MongoDB — EthicalSoft Compliance

Banco de dados: **ethicalsoft_db** · Motor: **MongoDB 8.0**

---

## 1. `ai_generation_cache`

Armazena o resultado de chamadas à LLM (Groq/llama-3.3-70b) para evitar reprocessamento de prompts idênticos.

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno do MongoDB |
| `hash` | String *(unique)* | Hash SHA-256 do prompt; chave de cache para deduplicação |
| `operation` | String | Tipo de operação gerada (ex.: `DASHBOARD_SNAPSHOT`) |
| `language` | String | Idioma do conteúdo gerado (ex.: `pt`, `en`) |
| `projectId` | Long | ID do projeto ao qual a geração se refere |
| `questionnaireId` | Integer | ID do questionário ao qual a geração se refere |
| `content` | String | Texto gerado pela LLM |
| `createdAt` | LocalDateTime (UTC) | Momento da geração |

**Uso:** consultada antes de cada chamada à IA; se o hash já existir o conteúdo é retornado diretamente, sem custo de API.

---

## 2. `document_emission_records`

Registro de auditoria imutável de cada documento oficial emitido (Boletim de Não Conformidade ou Certificado de Conformidade).

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `authenticityCode` | String *(unique)* | Código de autenticidade gerado no momento da emissão (ex.: `BNC-...`) |
| `documentType` | String | Tipo do documento emitido (`BOLETIM_NAO_CONFORMIDADE`, `CERTIFICADO_CONFORMIDADE`) |
| `projectId` | Long | ID do projeto |
| `projectName` | String | Nome do projeto no momento da emissão |
| `questionnaireId` | Integer | ID do questionário (nullable para certificado consolidado) |
| `questionnaireName` | String | Nome do questionário |
| `stageId` | Integer | ID da etapa (projetos cascata) |
| `stageName` | String | Nome da etapa |
| `iterationId` | Integer | ID da iteração (projetos iterativos) |
| `iterationName` | String | Nome da iteração |
| `scopeLabel` | String | Rótulo do escopo exibido no documento (`Etapa`, `Iteração`, `Projeto`) |
| `emittedAt` | LocalDateTime (UTC) | Data e hora da emissão |
| `emittedByUserId` | Long | ID do usuário que emitiu |
| `emittedByName` | String | Nome do usuário que emitiu |
| `isepPercent` | BigDecimal | Valor do ISEQ (%) no momento da emissão |
| `band` | String | Faixa de conformidade (`A`, `B`, `C`, `D`, `E`) |
| `dataHash` | String | Hash SHA-256 dos dados-fonte; permite detectar adulteração posterior |
| `templateVersion` | String | Versão do template FreeMarker usado para renderizar o PDF |

**Uso:** trilha de auditoria; permite validar autenticidade de documentos e detectar re-emissões.

---

## 3. `notification_templates`

Templates configuráveis de notificação, definindo quem pode enviar, quais canais usar e o conteúdo padrão de cada tipo de evento.

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `key` | String *(unique)* | Chave do tipo de notificação (ex.: `PROJECT_ASSIGNMENT`) |
| `whoCanSend` | List\<String\> | Roles que podem disparar esta notificação |
| `recipients` | List\<String\> | Perfis destinatários padrão |
| `title` | String | Título padrão da notificação |
| `body` | String | Corpo padrão da notificação (suporta variáveis) |
| `templateLink` | String | Caminho do template FreeMarker para o email |
| `channels` | List\<String\> | Canais de entrega (`EMAIL`, `IN_APP`) |

**Uso:** consultada pelo `SendNotificationUseCase` para determinar como cada evento deve ser entregue; permite customização sem redeploy.

---

## 4. `notifications`

Notificações geradas e enviadas a usuários específicos, com controle de status de leitura.

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `sender` | NotificationParty | Remetente: `{ userId, fullName, email, roles }` |
| `recipient` | NotificationParty | Destinatário: `{ userId, fullName, email, roles }` |
| `title` | String | Título da notificação |
| `content` | String | Conteúdo renderizado da notificação |
| `status` | String (enum) | Status de entrega/leitura (`PENDING`, `READ`, `FAILED`) |
| `createdAt` | LocalDateTime (UTC) | Data de criação |
| `updatedAt` | LocalDateTime (UTC) | Data da última atualização de status |
| `templateKey` | String | Chave do template usado (referência a `notification_templates`) |
| `governanceEvent` | String | Evento de governança que originou a notificação |

**Índice composto:** `{ 'recipient.userId': 1, 'status': 1 }` — otimiza listagem de notificações por usuário filtradas por status.

**Uso:** inbox de notificações in-app e registro de histórico de comunicações do sistema.

---

## 5. `pdf_document_configs`

Configuração canônica dos documentos PDF emitidos pelo sistema (título, template, textos padrão, ações corretivas).

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `key` | String *(unique)* | Chave do documento (`BOLETIM_NAO_CONFORMIDADE`, `CERTIFICADO_CONFORMIDADE`) |
| `templateLink` | String | Caminho do template FreeMarker (ex.: `documents/boletim-nao-conformidade.ftl`) |
| `documentTitle` | String | Título exibido no cabeçalho do PDF |
| `systemName` | String | Nome do sistema exibido no rodapé |
| `footerNote` | String | Nota de rodapé padrão |
| `impactSummary` | String | Resumo do impacto ético (exclusivo do boletim) |
| `defaultCorrectiveActions` | List\<String\> | Lista de ações corretivas sugeridas (exclusivo do boletim) |
| `validationUrl` | String | URL ou instrução de validação do documento (exclusivo do certificado) |
| `issuerLabel` | String | Rótulo do emissor exibido na assinatura digital (exclusivo do certificado) |

**Uso:** permite que administradores personalizem textos e templates dos PDFs sem alterar código; reconciliada automaticamente no startup pelo `PdfDocumentConfigInitializer`.

---

## 6. `project_templates`

Templates de projeto reutilizáveis, contendo a estrutura completa de etapas, iterações, questionários, perguntas e papéis.

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `name` | String | Nome do template |
| `type` | String (enum) | Tipo do projeto-modelo: `CASCATA` ou `ITERATIVO` |
| `description` | String | Descrição do template |
| `visibility` | String (enum) | `PUBLIC` (todos) ou `PRIVATE` (apenas o criador) |
| `userId` | Long | ID do usuário dono (preenchido apenas se `visibility = PRIVATE`) |
| `defaultIterationCount` | Integer | Número padrão de sprints (projetos iterativos) |
| `defaultIterationDuration` | Integer | Duração padrão de cada sprint em dias |
| `stages` | List\<TemplateStageDTO\> | Etapas: `{ name, weight, sequence, durationDays, applicationStartDate, applicationEndDate }` |
| `iterations` | List\<TemplateIterationDTO\> | Iterações: `{ name, weight, applicationStartDate, applicationEndDate }` |
| `questionnaires` | List\<TemplateQuestionnaireDTO\> | Questionários: `{ name, weight, stageName, iterationRefName, domain, description, questions[] }` |
| `questionnaires[].questions[]` | TemplateQuestionDTO | Perguntas: `{ value, type (BASE/CUSTOM), classification (PROJETO_INTEIRO/BASE_ITERACAO/ROTATIVA), stageName, roles[], stages[] }` |
| `representatives` | List\<TemplateRepresentativeDTO\> | Representantes-modelo: `{ email, firstName, lastName, weight, roles[] }` |

**Uso:** base para criação de novos projetos; o frontend carrega a estrutura via `GET /templates/{id}` e envia o payload populado para `POST /projects`.

---

## 7. `question_metadata`

Metadados de enriquecimento semântico das perguntas do PostgreSQL, usados no cálculo de sub-índices de conformidade por domínio.

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `questionId` | Long *(unique)* | FK para a tabela `question` do PostgreSQL |
| `domain` | String (enum) | Domínio da pergunta: `ETHICS`, `PROCESS`, `QUALITY`, `SECURITY`, `ESG`, `FAIRNESS`, `AI_GOVERNANCE` |
| `theme` | String | Tema ou tópico da pergunta (usado no aquecimento de cache de tradução) |
| `weight` | BigDecimal | Peso da pergunta no cálculo do domínio (padrão: `1.0`) |
| `critical` | Boolean | Indica se a pergunta é crítica para a faixa de conformidade |

**Uso:** consultada durante o cálculo do ISEQ para gerar os sub-índices `DomainScores` (Ética, ESG, Equidade, etc.) exibidos no dashboard.

---

## 8. `questionnaire_responses`

Respostas dos representantes a cada questionário de um projeto. É o documento mais volumoso e central do sistema operacional.

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `projectId` | Long | ID do projeto |
| `questionnaireId` | Integer | ID do questionário |
| `representativeId` | Long | ID do representante (null = resposta-base/template) |
| `stageId` | Integer | ID da etapa associada |
| `status` | String (enum) | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| `submissionDate` | LocalDateTime | Data de envio final |
| `answers` | List\<AnswerDocument\> | Lista de respostas individuais (ver abaixo) |

**Subdocumento `AnswerDocument`:**

| Campo | Tipo | Descrição |
|---|---|---|
| `questionId` | Long | ID da pergunta no PostgreSQL |
| `questionText` | String | Texto da pergunta no momento da resposta (snapshot) |
| `stageIds` | List\<Integer\> | IDs das etapas vinculadas à pergunta |
| `roleIds` | List\<Long\> | IDs dos papéis elegíveis para responder |
| `response` | Boolean | Resposta: `true` (Sim / Conforme), `false` (Não / Não conforme), `null` (não respondida) |
| `justification` | LinkDocument | Justificativa: `{ descricao, url }` |
| `evidence` | LinkDocument | Evidência: `{ descricao, url }` |
| `attachments` | List\<LinkDocument\> | Anexos adicionais |

**Uso:** alimenta o cálculo do ISEQ, o dashboard de conformidade, os relatórios de não conformidade e a geração de documentos PDF.

---

## 9. `translation_cache`

Cache de traduções geradas pela LLM para textos dinâmicos do sistema (nomes de etapas, perguntas, templates).

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `hash` | String *(unique)* | Hash do par `(text + targetLanguage)`; chave de deduplicação |
| `language` | String | Código do idioma de destino (ex.: `en`, `es`) |
| `original` | String | Texto original (idioma-fonte) |
| `translated` | String | Texto traduzido |
| `createdAt` | LocalDateTime (UTC) | Momento em que a tradução foi gerada e cacheada |

**Uso:** consultada pelo `TranslateDynamicTextUseCase` antes de chamar a LLM; o `WarmTranslationCacheUseCase` pré-aquece esta collection iterando sobre todos os textos dos templates.

---

## 10. `user_ai_tokens`

Tokens pessoais de IA armazenados pelos usuários para uso em chamadas à LLM em seu próprio nome.

| Campo | Tipo | Descrição |
|---|---|---|
| `_id` | String (ObjectId) | Identificador interno |
| `userId` | Long *(unique)* | ID do usuário no PostgreSQL |
| `encryptedToken` | String | Token cifrado com chave simétrica (AES via `EncryptionPort`) |
| `tokenHint` | String | Máscara do token para exibição na UI (ex.: `sk-...****abc`) |
| `provider` | String | Provedor da IA (ex.: `GROQ`) |
| `createdAt` | LocalDateTime (UTC) | Data de cadastro do token |
| `updatedAt` | LocalDateTime (UTC) | Data da última atualização |

**Uso:** permite que usuários configurem seu próprio token de IA; o sistema utiliza o token do usuário nas chamadas à LLM quando disponível, em vez do token global da aplicação.
