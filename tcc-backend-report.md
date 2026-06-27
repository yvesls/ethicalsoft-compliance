# Relatório Técnico do Backend — Ethicalsoft Compliance

> Documento gerado a partir da análise direta do código-fonte e arquivos de configuração em **27/06/2026**.  
> Todas as informações foram verificadas na fonte — nenhum item é estimativa ou suposição.

---

## 1. Arquitetura e Organização de Pacotes

### 1.1 Estrutura do pacote raiz

O backend segue a **Arquitetura Hexagonal** (Ports and Adapters). O pacote raiz é:

```
com.ethicalsoft.ethicalsoft_complience
```

Com os seguintes sub-pacotes de primeiro nível:

| Pacote | Responsabilidade |
|--------|-----------------|
| `adapters` | Implementações das portas — conexão com o mundo externo |
| `application` | Casos de uso, serviços de aplicação e definição das portas (interfaces) |
| `domain` | Entidades de domínio, cálculos, regras de negócio puras |
| `controller` | Camada HTTP/REST (separada dos adapters por decisão de projeto) |
| `infra` | Segurança, configuração Spring, schedulers, bootstrap de dados |
| `common` | Utilitários transversais (mapper, validadores, handlers de exceção) |
| `exception` | Hierarquia de exceções customizadas |

### 1.2 Detalhamento do pacote `adapters`

```
adapters/
├── in/
│   └── web/                  ← Adaptadores de entrada HTTP (mapping de requests)
├── out/
│   ├── auth/                 ← Autenticação local + Google OAuth
│   ├── llm/                  ← Integração com Groq via Spring AI
│   ├── mongo/                ← Repositórios e modelos MongoDB
│   ├── notification/         ← Envio de e-mails (JavaMail + FreeMarker)
│   ├── pdf/                  ← Geração de PDF (FreeMarker + OpenHTMLtoPDF)
│   └── postgres/             ← Repositórios JPA (Spring Data + Hibernate)
└── mapper/                   ← Mapeamentos ModelMapper
```

### 1.3 Detalhamento do pacote `application`

```
application/
├── port/                     ← Interfaces (portas) que os adapters implementam
│   ├── ai/
│   ├── auth/
│   ├── notification/
│   ├── project/
│   ├── questionnaire/
│   └── template/
├── service/                  ← Serviços orquestradores (ISEQ, IA, criação de projeto)
│   ├── ai/
│   └── strategy/             ← Strategy Pattern para criação de projetos (Cascata/Iterativo)
└── usecase/                  ← Casos de uso (um por funcionalidade)
    ├── ai/
    ├── auth/
    ├── document/
    ├── i18n/
    ├── notification/
    ├── project/
    ├── questionnaire/
    ├── template/
    ├── timeline/
    └── user/
```

### 1.4 Detalhamento do pacote `domain`

```
domain/
├── isep/                     ← Cálculo do ISEQ (índice ético), faixas, math puro
├── notification/             ← Modelo de domínio de notificações e critérios de disparo
├── i18n/                     ← Suporte a internacionalização
├── service/                  ← Política de timeline do projeto
└── repository/               ← Interfaces de repositório do lado do domínio
```

---

## 2. Tecnologias e Integrações

### 2.1 Stack principal

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 (LTS) | Linguagem principal |
| Spring Boot | 3.4.2 | Framework base |
| Spring Security | (incluído no Boot 3.4.2) | Autenticação e autorização |
| Spring Data JPA | (incluído no Boot 3.4.2) | Persistência relacional |
| Spring Data MongoDB | (incluído no Boot 3.4.2) | Persistência documental |
| Hibernate | 6.6.5.Final | ORM para PostgreSQL |
| Lombok | (incluído no Boot 3.4.2) | Redução de boilerplate |
| ModelMapper | 3.0.0 | Mapeamento entre objetos |

### 2.2 LLM / IA — Groq Cloud

- **Biblioteca:** `spring-ai-starter-model-openai` versão **1.0.5**
- **Provedor:** Groq Cloud, acessado via API compatível com OpenAI
- **Modelo configurado:** `llama-3.3-70b-versatile` (configurável por variável de ambiente `AI_MODEL_NAME`)
- **Base URL:** `https://api.groq.com/openai`
- **Temperatura:** 0.3 | **Max tokens:** 2048 | **Timeout:** 60 segundos

**Funcionalidades que usam o LLM:**

O `GroqLlmAdapter` expõe quatro operações:

| Operação | Descrição |
|---|---|
| `generateInsights` | Geração de insights éticos sobre o dashboard do questionário |
| `generateRiskReport` | Relatório automático de riscos éticos do projeto |
| `explainIsepResults` | Explicação em linguagem natural do resultado ISEQ |
| `askQuestion` (Q&A SSE) | Perguntas livres ao LLM sobre o contexto do projeto — resposta via Server-Sent Events (streaming) |
| `translate` | Tradução de textos para o idioma preferido do usuário |

**Importante:** cada usuário cadastra seu próprio token Groq individualmente via `PUT /ai/me/token`. O token é armazenado criptografado com **AES-256** no MongoDB (`UserAiTokenDocument`). A autoconfiguration padrão do Spring AI está desabilitada — o `ChatModel` é instanciado programaticamente por usuário em tempo de execução pelo `UserChatModelResolver`.

**Cache de geração:** as respostas do LLM são cacheadas no MongoDB (`AiGenerationCacheDocument`) com chave SHA-256 derivada de `operation + projectId + questionnaireId + isepPercent + band + idioma`. Isso evita requisições repetidas para o mesmo contexto.

### 2.3 Circuit Breaker — Resilience4j

- **Biblioteca:** `spring-cloud-starter-circuitbreaker-resilience4j` (Spring Cloud 2024.0.0)
- **O que protege:** **exclusivamente a chamada ao LLM** (`GroqLlmAdapter`)
- **Instância configurada:** `llm`

Parâmetros:

```properties
resilience4j.circuitbreaker.instances.llm.sliding-window-size=10
resilience4j.circuitbreaker.instances.llm.failure-rate-threshold=50     # Abre com 50% de falhas
resilience4j.circuitbreaker.instances.llm.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.llm.permitted-number-of-calls-in-half-open-state=3
resilience4j.timelimiter.instances.llm.timeout-duration=60s
resilience4j.retry.instances.llm.max-attempts=2
resilience4j.retry.instances.llm.wait-duration=2s
```

**Fallback:** quando o circuito está aberto, o método `fallbackInsightsLang` é invocado, retornando um `AiInsightResult.unavailable(...)` com mensagem amigável ao usuário. O dashboard continua funcionando normalmente sem análise de IA.

### 2.4 Autenticação — JWT + Google OAuth

**Dois fluxos coexistem:**

#### Fluxo 1 — Autenticação local (email + senha)

- `TokenService` usa a biblioteca `com.auth0:java-jwt:4.4.0`
- JWT é **gerado localmente** com `Algorithm.HMAC256(secret)` onde o `secret` vem da variável de ambiente `JWT_SECRET`
- Token expira em **30 minutos**; refresh token expira em **7 dias** (604800000 ms)
- Claims incluídos: `sub` (email), `roles`, `email`, `name`, `isFirstAccess`, `authProvider`, `avatarUrl`
- Validação: `SecurityFilter` intercepta todas as requisições, extrai e valida o JWT localmente

#### Fluxo 2 — Google OAuth (Login com Google)

- Biblioteca: `com.google.api-client:google-api-client:2.7.0`
- O frontend envia o **Google ID Token** para `POST /auth/google`
- O backend verifica o ID Token usando `GoogleIdTokenVerifier` (validação na infraestrutura do Google)
- Após verificação, o backend **emite seu próprio JWT** (auth0/java-jwt) e retorna ao cliente
- Contas Google são vinculadas à tabela `user` pelo `googleId` (migration V14)

### 2.5 Migrações — Flyway

- 17 arquivos de migração SQL no diretório `src/main/resources/db/migration/`
- Cobrem desde a criação da estrutura base (V1) até a adição do campo `classification` em perguntas (V17)
- `spring.flyway.validate-on-migrate=false` — checksums não são validados no startup

| Versão | Conteúdo |
|---|---|
| V1 | Tabelas base: projetos, roles, questionários |
| V2 | Tabela de resultados de questionários |
| V3 | Resultado ISEP por projeto |
| V4 | Scores por domínio ético |
| V5 | Roles Líder de Equipe e Arquiteto de Software |
| V6 | Colunas de domínio nos questionários |
| V7 | Datas anuláveis para rascunhos |
| V8 | Campo `duration_days` em etapas |
| V9–V13 | Correções progressivas no constraint de status do projeto |
| V14 | Campos Google Auth (`google_id`, `auth_provider`, `avatar_url`) |
| V15 | Renomeação ISEP → ISEQ nas tabelas de resultado |
| V16 | Campo `type` em perguntas (`BASE` / `CUSTOM`) |
| V17 | Campo `classification` em perguntas (`PROJETO_INTEIRO` / `BASE_ITERACAO` / `ROTATIVA`) |

### 2.6 MongoDB — Coleções armazenadas

O MongoDB **não armazena apenas templates**. As seguintes entidades usam MongoDB:

| Coleção / Documento | Finalidade |
|---|---|
| `ProjectTemplate` | Templates de questionário (Cascata, Iterativo, SelecIA RH) |
| `QuestionnaireResponse` | Respostas dos questionários (documentos dinâmicos por respondente) |
| `QuestionMetadataDocument` | Metadados de perguntas (domínio ético, tags) |
| `NotificationDocument` | Notificações enviadas |
| `NotificationTemplateDocument` | Templates de notificação configuráveis |
| `PdfDocumentConfigDocument` | Configurações dos documentos PDF emitidos |
| `DocumentEmissionRecord` | Histórico de emissões de certificados e boletins |
| `AiGenerationCacheDocument` | Cache de respostas do LLM por hash SHA-256 |
| `TranslationCacheDocument` | Cache de traduções geradas pelo LLM |
| `UserAiTokenDocument` | Token Groq de cada usuário (criptografado AES-256) |
| `UserLanguagePreferenceDocument` | Preferência de idioma por usuário |

**Critério de separação PostgreSQL × MongoDB:**

- **PostgreSQL:** entidades com relacionamentos fixos, identidade forte e que precisam de transações ACID — usuários, projetos, representantes, etapas, iterações, questionários, perguntas, resultados ISEQ
- **MongoDB:** documentos com estrutura variável, hierárquica ou sem esquema fixo — templates configuráveis, respostas de questionários (que variam por projeto), caches, notificações, preferências de usuário

### 2.7 FreeMarker — Uso em e-mails e PDFs

O FreeMarker é usado em **dois contextos distintos**:

#### PDFs (via `DocumentPdfRenderer`)

O `DocumentPdfRenderer` usa FreeMarker para renderizar HTML e depois converte para PDF com OpenHTMLtoPDF (`openhtmltopdf-pdfbox:1.0.10`):

| Template | Documento gerado |
|---|---|
| `documents/certificado-conformidade.ftl` | Certificado de Conformidade Ética |
| `documents/boletim-nao-conformidade.ftl` | Boletim de Não Conformidade |

#### E-mails (via JavaMail)

Templates para notificações automáticas por e-mail:

| Template | Evento |
|---|---|
| `recover/recovery-email.ftl` | Recuperação de senha |
| `users/new-user-credentials.ftl` | Credenciais para novo usuário |
| `users/project-assignment.ftl` | Atribuição a projeto |
| `users/project-unassignment.ftl` | Remoção de projeto |
| `users/project-deadline-reminder.ftl` | Lembrete de prazo |
| `users/project-isep-calculated.ftl` | ISEQ calculado (nível projeto) |
| `users/project-overdue.ftl` | Projeto em atraso |
| `users/questionnaire-reminder.ftl` | Lembrete de questionário pendente |
| `users/questionnaire-submitted.ftl` | Questionário submetido |
| `users/questionnaire-completed.ftl` | Questionário concluído |
| `users/questionnaire-isep-calculated.ftl` | ISEQ calculado (nível questionário) |
| `users/questionnaire-rescheduled.ftl` | Questionário reagendado |
| `users/questionnaire-overdue.ftl` | Questionário em atraso |
| `users/non-compliance-bulletin-emitted.ftl` | Boletim de não conformidade emitido |
| `users/next-questionnaire-starting-soon.ftl` | Próximo questionário começando em breve |

### 2.8 Apache POI e Apache Commons CSV

**Apache POI (`poi:5.2.3`)** está declarado no `pom.xml`, porém **não há nenhum `import org.apache.poi`** no código-fonte Java. A dependência existe mas não é utilizada na versão atual.

**Apache Commons CSV (`commons-csv:1.10.0`)** também está declarado no `pom.xml`, porém igualmente **sem imports no código-fonte**. A exportação CSV do ISEQ (`ExportIsepCsvUseCase`) é implementada manualmente com `StringBuilder` e delimitador `;`, sem uso da biblioteca.

Ambas as dependências são **candidatas a remoção** do `pom.xml`.

### 2.9 AOP — Spring AOP

- `spring-boot-starter-aop` está declarado no `pom.xml`
- **Não foram encontrados `@Aspect`, `@Around`, `@Before`, `@After` ou `@Pointcut`** em nenhuma classe do código-fonte
- O AOP está disponível para uso via `@EnableAspectJAutoProxy`, mas **não há aspectos implementados** ativamente
- A anotação `@Transactional` (do Spring) usa AOP via proxy internamente, mas não via aspectos customizados

### 2.10 Endpoints públicos (sem autenticação)

Conforme a configuração em `SecurityConfigurations`, os únicos endpoints acessíveis sem autenticação são:

| Padrão | Método | Finalidade |
|---|---|---|
| `/auth/**` | Qualquer | Login local, refresh token, cadastro, login com Google, recuperação de senha |
| `/**` | OPTIONS | Preflight CORS |

**Todo o restante da API exige JWT válido.**

---

## 3. Qualidade e Testes

### 3.1 JaCoCo e SonarQube

- **JaCoCo** versão **0.8.11** está integrado ao build Maven com dois goals: `prepare-agent` (fase `initialize`) e `report` (fase `prepare-package`)
- O relatório XML é gerado em `target/site/jacoco/jacoco.xml`
- **SonarQube** está configurado: a propriedade `sonar.coverage.jacoco.xmlReportPaths` aponta para esse arquivo, e o plugin `sonar-maven-plugin` está no build — o projeto está **pronto para análise no SonarQube**, mas a integração é acionada manualmente via `mvn sonar:sonar`
- **Não há `<rule>` de cobertura mínima** definida no `pom.xml` — nenhuma porcentagem mínima é exigida para o build passar

### 3.2 Classes excluídas da cobertura

O JaCoCo está configurado para **excluir** da análise:
- `**/controller/**`
- `**/model/**`
- `**/repository/**`
- `**/infra/**`
- `**/configuration/**`
- `**/exception/**`
- `**/util/**`
- `EthicalsoftComplienceApplication`

Isso direciona a cobertura para **casos de uso e serviços de domínio**, que são as classes mais críticas do negócio.

### 3.3 Tipos de testes

- **11 arquivos de teste** no total (`src/test/`)
- Todos utilizam `@ExtendWith(MockitoExtension.class)` com `@Mock` e `@InjectMocks` — **são testes unitários com mocks**, sem contexto Spring nem banco de dados real
- Não há testes de integração (`@SpringBootTest`) nem testes de repositório (`@DataJpaTest` / `@DataMongoTest`)

| Arquivo de Teste | Classe testada |
|---|---|
| `IseqCalculationServiceTest` | Cálculo do índice ISEQ |
| `AuthServiceTest` | Serviço de autenticação |
| `PasswordRecoveryServiceTest` | Recuperação de senha |
| `IterativeProjectStrategyTest` | Estratégia de criação de projeto iterativo |
| `WaterfallProjectStrategyTest` | Estratégia de criação de projeto cascata |
| `TemplateMongoAdapterTest` | Adaptador de template MongoDB |
| `QuestionnaireServiceTest` | Serviço de questionários |
| `ExportIsepCsvUseCaseTest` | Exportação CSV do ISEQ |
| `ProcessExpiredQuestionnairesIsepUseCaseTest` | Processamento de questionários expirados |
| `ProcessExpiredProjectIsepUseCaseTest` | Processamento de projetos expirados |
| `SendInternalNotificationUseCaseTest` | Envio de notificações internas |

---

## 4. Banco de Dados

### 4.1 PostgreSQL

- **Versão em uso:** PostgreSQL **16.14** (Ubuntu)
- **Banco:** `ethicalsoft_db`
- **Driver:** `org.postgresql.Driver` (versão gerenciada pelo Spring Boot 3.4.2)
- **ORM:** Hibernate 6.6.5.Final com `spring.jpa.hibernate.ddl-auto=update`
- **Pool de conexões:** HikariCP (padrão do Spring Boot)

### 4.2 MongoDB

- **Versão em uso:** MongoDB **8.0.15**
- **Banco:** `ethicalsoft_db`
- **Autenticação:** usuário `ethicalsoft`, autenticado no banco `admin`

### 4.3 Critério de separação entre os bancos

| Critério | PostgreSQL | MongoDB |
|---|---|---|
| **Tipo de dado** | Relacional, estrutura fixa | Documental, estrutura variável |
| **Transações** | ACID — essencial para projetos, usuários, resultados | Eventual — caches, preferências, histórico |
| **Relacionamentos** | ManyToMany, OneToMany (JPA) | Documentos embarcados, sem JOIN |
| **Exemplos** | `user`, `project`, `questionnaire`, `question`, `representative`, `questionnaire_result` | `QuestionnaireResponse` (respostas dinâmicas), `ProjectTemplate`, caches de IA, notificações |

A regra prática adotada: **o que tem esquema fixo e participação em transações fica no PostgreSQL; o que tem estrutura dinâmica ou é acessório fica no MongoDB**.

---

## 5. Observações Finais para o TCC

- O projeto implementa a **Arquitetura Hexagonal** de forma completa, com separação clara entre portas (`application/port`), adaptadores (`adapters`) e domínio (`domain`)
- O padrão **Strategy** é usado para suportar dois tipos de projeto (Cascata e Iterativo) sem condicional no caso de uso principal
- O padrão **Template Method** implícito aparece nos builders de questionário do `BaseQuestionnaireTemplateInitializer`
- A integração com IA é **opt-in por usuário**: o sistema funciona completamente sem IA; ela é uma camada adicional de análise
- O **Circuit Breaker** garante que falhas no Groq não derrubem o sistema principal — o dashboard ético continua acessível mesmo com a IA indisponível
- A autenticação suporta **dois provedores** (local e Google) com o mesmo token JWT de saída, unificando a experiência do cliente
