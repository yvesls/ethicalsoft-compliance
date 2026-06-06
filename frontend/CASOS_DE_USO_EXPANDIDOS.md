# Descrição Expandida dos Casos de Uso - Sistema de Compliance Ético

> **Documento Atualizado:** Reflete a implementação real do frontend Angular, com detalhes técnicos sobre integração de serviços, fluxos de estado e validações implementadas.

---

## UC01 - Registrar-se como Analista de Qualidade

**Ator(es):** Analista de qualidade (novo usuário).

**Referência:** BR03.

**Pré-condição:** Não há.

### Fluxo Principal

1. **[EV]** O analista acessa a tela de registro (`/register`)
2. **[EV]** O analista preenche os campos:
   - Nome (obrigatório)
   - Sobrenome (obrigatório)
   - Email (obrigatório, validação RFC5322)
   - Senha (obrigatório, validação de segurança)
   - Confirmar Senha (obrigatório)
   
   **[FE01]** [FE02] [FE03]

3. **[EV]** O analista lê o termo de participação e marca o checkbox "Aceito" 
   - **[FE04]** Botão "Cadastrar" permanece desabilitado até aceitar

4. **[RS]** `RegisterComponent` coleta dados do formulário e chama `AuthStore.register()`

5. **[RS]** Frontend envia requisição `POST /auth/register` com payload:
   ```json
   {
     "firstName": "string",
     "lastName": "string",
     "email": "string",
     "password": "string",
     "confirmPassword": "string",
     "acceptTerms": true
   }
   ```

6. **[EV]** O analista clica em "Cadastrar"

7. **[RS]** Sistema processa a requisição:
   - Valida email não duplicado no backend
   - Valida força de senha
   - Cria conta com `isFirstAccessPending = true`
   - Retorna token JWT

8. **[RS]** Sistema exibe notificação [MSG03] e redireciona para `/login`

### Fluxo Alternativo

**FA01 - Validação de Força de Senha em Tempo Real:**
- Conforme o usuário digita a senha, o sistema exibe feedback sobre:
  - Comprimento mínimo (8 caracteres)
  - Maiúsculas obrigatórias
  - Minúsculas obrigatórias
  - Números obrigatórios
  - Caracteres especiais obrigatórios
- Usa validador customizado `CustomValidators.passwordValidator()`

### Fluxo de Exceção

**FE01 - Email já cadastrado:**
- Sistema retorna erro `400 Bad Request`
- [RS] Exibe mensagem [MSG01]
- Campo "Email" recebe bordas vermelhas

**FE02 - Senha e Confirmar não coincidem:**
- Validação ocorre em tempo real ou ao clicar "Cadastrar"
- [RS] Exibe mensagem [MSG02]
- Campo "Confirmar Senha" recebe bordas vermelhas

**FE03 - Senha não atende critérios de segurança:**
- [RS] Exibe mensagem específica indicando qual regra falhou
- Usar feedback visual (checkbox ou lista com critérios)
- Exemplo: "❌ Faltam caracteres especiais (!@#$%^&*)"

**FE04 - Termo não aceito:**
- Ao clicar "Cadastrar" sem marcar checkbox:
- [RS] Checkbox recebe borda vermelha e exibe mensagem de erro

### Pós-condição

- Um novo registro de analista de qualidade existe no sistema
- Status do usuário: `isFirstAccessPending = true` (deve redefinir senha no primeiro acesso)
- Nenhum email de confirmação é enviado (confirmação automática)

### Mensagens

- **MSG01:** "Email informado já está registrado. Tente outro email."
- **MSG02:** "As senhas não coincidem. Verifique os campos."
- **MSG03:** "Cadastrado com sucesso! Realize o login para continuar."
- **MSG04:** "Erro ao registrar. Erro: [Mensagem do sistema]"

---

## UC02 - Realizar Login

**Ator(es):** Agente (analista de qualidade, gerente de projeto, membro da equipe).

**Referência:** BR03.

**Pré-condição:** 
- O agente possui um cadastro ativo no sistema
- O sistema exibe a tela de login

### Fluxo Principal

1. **[RS]** Sistema carrega tela de login (`/login`)

2. **[EV]** O agente preenche o campo "Email"

3. **[EV]** O agente preenche o campo "Senha"

4. **[EV]** O agente marca ou não o checkbox "Manter-me conectado"
   - Se marcado: token salvo em `localStorage` (persistente)
   - Se desmarcado: token salvo em `sessionStorage` (sessão do navegador)

5. **[EV]** O agente clica em "Entrar" **[FE01]**

6. **[RS]** `LoginComponent` coleta credenciais e chama `AuthStore.login()`

7. **[RS]** Frontend envia requisição `POST /auth/login`:
   ```json
   {
     "email": "string",
     "password": "string"
   }
   ```

8. **[RS]** Backend valida credenciais e retorna:
   ```json
   {
     "accessToken": "jwt_token",
     "refreshToken": "jwt_refresh_token",
     "user": {
       "id": "uuid",
       "email": "user@email.com",
       "firstName": "string",
       "lastName": "string",
       "roles": ["Analyst", "ProjectManager"],
       "isFirstAccessPending": boolean
     }
   }
   ```

9. **[RS]** Sistema armazena:
   - Token em storage (localStorage/sessionStorage)
   - Usuário em `AuthenticationService.userRoles$`

10. **[RS]** Sistema verifica `isFirstAccessPending`:
    - **Se true:** Redireciona para `/settings/reset-password` (forçado)
    - **Se false:** Redireciona para `/home`

11. **[RS]** `SessionExpirationService` inicia monitoramento:
    - Calcula tempo até expiração do token
    - Antes de expirar: chama `refreshToken()` automaticamente
    - Se falhar: faz logout + redireciona para `/login`

### Fluxo Alternativo

**FA01 - Lembrar-se de Email:**
- Sistema pode oferecer autocomplete de emails já usados (opcional)

**FA02 - Login com Último Email:**
- Se o campo email está vazio, preencher com último email usado (do localStorage)

### Fluxo de Exceção

**FE01 - Credenciais inválidas:**
- Email e/ou senha incorretos
- [RS] Backend retorna `401 Unauthorized`
- [RS] Exibe mensagem [MSG01]
- Sistema NÃO indica qual campo está incorreto (por segurança)

**FE02 - Erro de Conexão:**
- Requisição falha (timeout, conexão perdida)
- [RS] Exibe mensagem [MSG02] com opção de tentar novamente

**FE03 - Conta Desativada/Bloqueada:**
- Backend retorna status `403 Forbidden`
- [RS] Exibe mensagem [MSG03]

### Pós-condição

- O agente está autenticado no sistema
- Uma sessão é iniciada com duração configurável
- Token JWT é armazenado para requisições futuras
- O agente pode interagir com o sistema de acordo com suas permissões (BR05)
- Spinner (`SpinnerInterceptor`) está pronto para exibir em requisições HTTP

### Mensagens

- **MSG01:** "Email ou senha incorretos. Tente novamente ou clique em 'Esqueci minha senha'."
- **MSG02:** "Erro ao conectar. Verifique sua conexão e tente novamente."
- **MSG03:** "Sua conta foi desativada. Entre em contato com o administrador."

### Observações Técnicas

- **TokenInterceptor:** Adiciona `Authorization: Bearer {token}` automaticamente em todas as requisições
- **ProjectContextInterceptor:** Adiciona `X-Project-Id` header se há projeto ativo
- **Refresh Automático:** Token é renovado automaticamente via `SessionExpirationService`
- **Storage Seguro:** `StorageService` cuida de SSR (Server-Side Rendering)

---

## UC03 - Manter Projeto

**Ator(es):** Analista de qualidade.

**Referência:** BR02.

**Pré-condição:** O analista de qualidade está logado e tem permissão para gerenciar projetos.

### Fluxo Principal - Criação de Projeto (Tipo: Cascata)

1. **[EV]** O analista navega para `/projects` (tela de projetos)

2. **[RS]** Sistema carrega `ProjectListPageComponent` e exibe:
   - Lista de projetos existentes com paginação
   - Filtros por: Nome, Status, Tipo, Data de Criação
   - Botão "Novo Projeto"

3. **[EV]** O analista clica em "Novo Projeto"

4. **[RS]** Sistema apresenta escolha de tipo:
   - Modal com opções: "Projeto Cascata" ou "Projeto Iterativo Incremental"

5. **[EV]** O analista seleciona "Projeto Cascata"

6. **[RS]** Sistema redireciona para `/projects/create?type=Cascata`

7. **[RS]** Sistema exibe `CascataProjectFormComponent` com:
   - Seção 1: "Informações do Projeto" (expandida)
   - Seção 2: "Etapas" (minimizada)
   - Seção 3: "Representantes" (minimizada)
   - Seção 4: "Questionários" (minimizada)

8. **[EV]** Analista preenche Seção 1 - Informações do Projeto:
   - **Nome:** Campo texto obrigatório
   - **Descrição:** Campo texto opcional
   - **Template:** Dropdown com templates predefinidos **[FA01]**
   - **Data de Início:** Campo data obrigatório
   - **Prazo Limite (Data Fim):** Campo data obrigatório
   - **Encargos:** Multi-select com opciones (ex: "Segurança", "Privacidade", "Acessibilidade")
   
   **[FE02]** Validações em tempo real

9. **[RS]** Se Template selecionado **[FA01]:**
   - Sistema popula automaticamente Etapas, Representantes e Questionários
   - Todos os campos permanecem editáveis

10. **[EV]** Analista clica "Continuar" na Seção 1

11. **[RS]** Sistema minimiza Seção 1 e expande Seção 2 (Etapas)

12. **[EV]** Analista gerencia etapas **(Ver UC13 - Manter Etapas)**
    - Adiciona etapas: Design, Desenvolvimento, Testes, Deployment
    - Define pesos (percentuais) para cada etapa

13. **[EV]** Analista clica "Continuar" na Seção 2

14. **[RS]** Sistema minimiza Seção 2 e expande Seção 3 (Representantes)

15. **[EV]** Analista gerencia representantes **(Ver UC12 - Manter Representantes)**
    - Adiciona membros com email, peso e encargos
    - Valida que todos os encargos estão representados **[FE01]**

16. **[EV]** Analista clica "Continuar" na Seção 3

17. **[RS]** Sistema minimiza Seção 3 e expande Seção 4 (Questionários)

18. **[EV]** Analista gerencia questionários **(Ver UC11 - Manter Questionário)**
    - Importa questionários de template
    - Edita questionários e perguntas

19. **[EV]** Analista clica "Salvar Projeto"

20. **[RS]** Sistema valida:
    - Todos os campos obrigatórios preenchidos **[FE02]**
    - Pelo menos uma etapa, um representante e um questionário

21. **[RS]** Se validação OK:
    - Exibe modal com mensagem [MSG01] solicitando confirmação
    - Modal informa que emails de convite serão enviados

22. **[EV]** Analista clica "Confirmar"

23. **[RS]** Sistema faz requisição `POST /api/projects`:
    ```json
    {
      "name": "string",
      "description": "string",
      "type": "Cascata",
      "startDate": "ISO 8601",
      "endDate": "ISO 8601",
      "templateId": "string",
      "stages": [
        {
          "id": "uuid",
          "name": "string",
          "weight": number
        }
      ],
      "representatives": [
        {
          "email": "string",
          "firstName": "string",
          "lastName": "string",
          "weight": number,
          "roles": ["string"]
        }
      ],
      "questionnaires": [
        {
          "id": "uuid",
          "name": "string",
          "weight": number,
          "stageId": "uuid"
        }
      ]
    }
    ```

24. **[RS]** Backend cria projeto com status "Aberto"

25. **[RS]** Backend envia emails de convite para cada representante com:
    - Link de primeiro acesso: `/reset-password?token=...`
    - Instruções de participação

26. **[RS]** Sistema exibe notificação [MSG03] e redireciona para:
    - `/projects/{projectId}` (tela de detalhes do projeto)

### Fluxo Principal - Criação de Projeto (Tipo: Iterativo Incremental)

*Segue mesmos passos da Cascata, com diferenças em Seção 1:*

8b. **[EV]** Analista preenche Seção 1 - Informações do Projeto (Iterativo):
   - **Nome, Descrição, Template, Data de Início, Prazo Limite:** Idem Cascata
   - **Duração das Iterações (em dias úteis):** Campo número obrigatório (ex: 10, 15)
   
9b. **[RS]** Sistema calcula automaticamente:
   - Número de Iterações = (Prazo Limite - Data Início) / Duração
   - Datas de cada iteração (dinâmicas)
   - Exibe na tela: "Total de 5 iterações de 2 semanas cada"

### Fluxo Alternativo

**FA01 - Seleção de Template:**
1. Analista seleciona um Template no dropdown
2. [RS] Sistema popula automaticamente:
   - Tipo, Etapas, Representantes (se modelo contiver), Questionários
   - Campos permanecem editáveis
3. Analista pode sobrescrever qualquer informação

**FA02 - Cálculo Automático de Iterações:**
1. Ao mudar "Data Limite" ou "Duração de Iterações"
2. [RS] Sistema recalcula "Número de Iterações" em tempo real
3. Exibe alerta se houver mudanças automáticas

**FA03 - Salvar como Rascunho:**
1. Analista clica "Salvar Projeto" sem completar todas as seções
2. [RS] Se houver campos obrigatórios não preenchidos:
   - Sistema exibe alerta (não-bloqueante): "Você está salvando um projeto incompleto"
   - Oferece opção de "Continuar" ou "Voltar para editar"
3. Se "Continuar": Projeto é salvo com status "Rascunho"
4. Análista pode editar depois via `/projects/{projectId}/edit`

**FA04 - Cancelar Criação:**
1. A qualquer momento, analista clica "Cancelar"
2. [RS] Se há mudanças não salvas:
   - Exibe modal: "Descartar alterações?"
3. Se "Sim": Redireciona para `/projects`
4. Se "Não": Retorna ao formulário

### Fluxo de Exceção

**FE01 - Encargos não representados (ao avançar de Representantes):**
1. Sistema detecta que nem todos os encargos têm representante
2. [RS] Exibe alerta: [MSG04]
3. Permite prosseguir mesmo assim (warning não-bloqueante)

**FE02 - Campos obrigatórios não preenchidos:**
1. Ao clicar "Continuar" em qualquer seção
2. Sistema detecta campos vazios marcados com "*"
3. [RS] Exibe erro: [MSG05]
4. Destaca campos pendentes com borda vermelha
5. Bloqueia avanço até preencher

**FE03 - Erro ao salvar projeto:**
1. Requisição `POST /api/projects` falha
2. Backend retorna `500 Internal Server Error`
3. [RS] Exibe notificação: [MSG06]
4. Mantém formulário aberto para tentar novamente

**FE04 - Email de representante inválido:**
1. Ao adicionar representante com email mal formado
2. [RS] Exibe erro validação: "Email inválido"
3. Impede salvar até corrigir

**FE05 - Projeto duplicado (mesmo nome):**
1. Backend detecta nome de projeto duplicado
2. [RS] Exibe mensagem: "Já existe um projeto com esse nome"
3. Sugere novo nome ou confirmar sobrescrita

### Pós-condição

- Um novo projeto é criado no sistema com status "Aberto"
- Projeto tem UUID único atribuído
- Todos os representantes recebem email de convite
- Projeto aparece na lista (`/projects`)
- Analista é redirecionado para detalhes do projeto
- Sistema define `ProjectContextService.setCurrentProjectId(projectId)`

### Mensagens

- **MSG01:** "Um template será criado para este projeto e um email de convite será enviado para cada participante contendo as instruções de primeiro acesso. Deseja confirmar a criação do projeto?"

- **MSG02:** "Erro ao salvar o projeto. Erro: [Mensagem do sistema]"

- **MSG03:** "Projeto criado com sucesso! Emails de convite foram enviados para os participantes."

- **MSG04:** "Há encargos que não foram representados por nenhum envolvido. Você pode continuar mesmo assim."

- **MSG05:** "Atenção! Há campos obrigatórios que não foram preenchidos corretamente: [Lista de campos]"

- **MSG06:** "Erro ao criar projeto. Por favor, tente novamente mais tarde."

### Observações Técnicas

- **Store:** `ProjectStore.createProject()` é responsável pela requisição
- **Contexto:** `ProjectContextService.setCurrentProjectId()` atualiza contexto
- **Header:** `ProjectContextInterceptor` adiciona `X-Project-Id` automaticamente
- **Validação:** `DraftCacheService` pode salvar rascunho local se desejar (não implementado)
- **Emails:** Backend integrado com serviço de email (SendGrid, etc.)

---

## UC04 - Gerenciar Senha (Primeiro Acesso / Recuperação)

**Ator(es):** Agente (novo usuário ou usuário que esqueceu senha).

**Referência:** BR04.

**Pré-condição:**
- Para primeiro acesso: Agente foi cadastrado e recebeu email com link `/reset-password?token=...`
- Para recuperação: Agente está na tela de login

### Fluxo Principal - Primeiro Acesso

1. **[RS]** Agente recebe email de convite com link:
   ```
   https://system.com/reset-password?token=eyJhbGc...
   ```

2. **[EV]** Agente clica no link do email

3. **[RS]** Sistema carrega `/reset-password` e valida token:
   - Se inválido/expirado: Exibe [MSG04] e oferece tentar novamente
   - Se válido: Carrega `ResetPasswordComponent`

4. **[RS]** Sistema exibe formulário com campos:
   - **Senha** (obrigatório, validação de força)
   - **Confirmar Senha** (obrigatório)
   - **Checkbox:** "Aceito o termo de participação"
   - (Para primeiro acesso: termo é OBRIGATÓRIO)

5. **[EV]** Agente preenche "Senha" e vê feedback em tempo real:
   - ✓ Mínimo 8 caracteres
   - ✓ Maiúsculas
   - ✓ Minúsculas
   - ✓ Números
   - ✓ Caracteres especiais

   **[FE02]** Se não atende critérios

6. **[EV]** Agente preenche "Confirmar Senha"

7. **[EV]** Agente marca checkbox "Aceito" **[FE03]**

8. **[EV]** Agente clica "Registrar" **[FE01]** **[FE04]**

9. **[RS]** Sistema valida:
   - Senhas coincidem
   - Senha atende critérios
   - Termo aceito (primeira vez)
   - Token ainda válido

10. **[RS]** Se tudo OK, faz requisição `POST /auth/reset-password`:
    ```json
    {
      "token": "jwt_token",
      "newPassword": "string",
      "confirmPassword": "string"
    }
    ```

11. **[RS]** Backend:
    - Valida token
    - Atualiza senha do usuário
    - Define `isFirstAccessPending = false`
    - Retorna success

12. **[RS]** Sistema exibe notificação [MSG03] e redireciona para `/login` (ou `/home` se já autenticado)

### Fluxo Alternativo

**FA01 - Recuperar Senha Esquecida:**

1. **[EV]** Agente na tela de login clica em "Esqueci minha senha"

2. **[RS]** Sistema redireciona para `/recover-account`

3. **[RS]** Sistema exibe `RecoverComponent`:
   - Campo "Email"
   - Botão "Enviar email de recuperação"

4. **[EV]** Agente digita seu email e clica "Enviar"

5. **[RS]** Sistema faz requisição `POST /auth/recover-account`:
   ```json
   {
     "email": "user@email.com"
   }
   ```

6. **[RS]** Backend:
   - Valida se email existe
   - Gera código de 6 dígitos (ou token)
   - Envia email com código: "Seu código de recuperação: 123456"
   - Retorna success (mesmo se email não existe, por segurança)

7. **[RS]** Sistema exibe mensagem [MSG05] e redireciona para `/code-verification`

8. **[EV]** Agente recebe email com código

9. **[EV]** Agente retorna para aplicação ou acessa link do email

10. **[RS]** Sistema exibe `CodeVerificationComponent`:
    - 6 campos para inserir código (um dígito cada)
    - Auto-focus entre campos
    - Botão "Verificar"

11. **[EV]** Agente insere código (ex: 1 2 3 4 5 6) **[FE05]**

12. **[EV]** Agente clica "Verificar" ou pressionan Enter

13. **[RS]** Sistema faz requisição `POST /auth/validate-code`:
    ```json
    {
      "email": "user@email.com",
      "code": "123456"
    }
    ```

14. **[RS]** Se código válido:
    - Backend retorna novo token temporário
    - Sistema redireciona para `/reset-password?token=...`
    - Sistema exibe formulário de reset (sem obrigatoriedade do termo desta vez)

15. **[EV]** Agente define nova senha (passos 5-12 do Fluxo Principal)

16. **[RS]** Após redefinir, redireciona para `/login` com mensagem [MSG03]

### Fluxo de Exceção

**FE01 - Senhas não coincidem:**
- No passo 8 (Registrar), ao validar:
- [RS] Exibe erro [MSG01]
- Campo "Confirmar Senha" recebe foco + borda vermelha
- Impede envio

**FE02 - Senha insegura:**
- Validação ocorre em tempo real conforme digita
- [RS] Exibe feedback visual:
  - ❌ Critério não atendido
  - ✓ Critério atendido
- Botão "Registrar" fica desabilitado enquanto senha fraca
- Se tentar enviar: Mensagem [MSG02] com critérios detalhados

**FE03 - Termo não aceito (primeira vez):**
- Agente clica "Registrar" sem marcar checkbox
- [RS] Checkbox recebe borda vermelha
- Exibe mensagem: "Você deve aceitar o termo de participação para continuar"
- Bloqueia envio

**FE04 - Token expirado/inválido:**
- Ao carregar `/reset-password?token=...` com token inválido
- [RS] Exibe mensagem [MSG04]
- Oferece link para "Solicitar novo código"

**FE05 - Código inválido:**
- Agente insere código errado
- [RS] Backend retorna `400 Bad Request`
- Exibe mensagem [MSG06]
- Oferece:
  - "Reenviar código" (novo email)
  - "Tentar outro email"

**FE06 - Múltiplas tentativas falhadas:**
- Backend detecta 5+ tentativas com código errado
- [RS] Bloqueia por 15 minutos
- Exibe mensagem: "Muitas tentativas falhadas. Tente novamente em 15 minutos."

### Pós-condição

- Senha do agente foi definida ou atualizada no sistema
- Se era primeiro acesso: `isFirstAccessPending = false`
- Token antigo é invalidado
- Agente pode fazer login com nova senha
- Sessão anterior é encerrada (se estava logado)

### Mensagens

- **MSG01:** "Senhas não coincidem. Verifique os campos."

- **MSG02:** "Senha fraca. Atenda aos critérios: Mínimo 8 caracteres, maiúsculas, minúsculas, números e caracteres especiais."

- **MSG03:** "Senha registrada com sucesso! Você pode fazer login agora."

- **MSG04:** "Link expirado ou inválido. Por favor, solicite um novo código de recuperação."

- **MSG05:** "Email enviado! Verifique sua caixa de entrada (e spam) para o código de recuperação."

- **MSG06:** "Código inválido. Tente novamente ou solicite um novo código."

### Observações Técnicas

- **Validador:** `CustomValidators.passwordValidator()` implementa regras
- **Store:** `AuthStore.resetPassword()`, `AuthStore.recover()`, `AuthStore.validateCode()`
- **Segurança:** Tokens JWT com expiração curta (~15 min)
- **Email:** Serviço backend envia código via email
- **Timeout:** Sessão com código expira após 24 horas

---

## UC05 - Responder Questionário

**Ator(es):** Agente (membro da equipe, especialista, desenvolvedor).

**Referência:** BR06.

**Pré-condição:**
- Agente está logado
- Projeto está em status "Aberto"
- Questionário da iteração/etapa vigente está disponível
- Agente é representante do projeto

### Fluxo Principal

1. **[RS]** Agente navega para `/projects` ou acessa `/home`

2. **[RS]** Sistema exibe lista de projetos onde agente é representante

3. **[EV]** Agente clica em projeto para abrir

4. **[RS]** Sistema carrega `ProjectDetailPageComponent`:
   - Lista de questionários com status (Respondido, Em Progresso, Pendente)
   - Filtros por etapa/iteração

5. **[EV]** Agente seleciona questionário não respondido

6. **[RS]** Sistema redireciona para `/projects/{projectId}/questionnaires/{questionnaireId}/respond`

7. **[RS]** Sistema carrega `QuestionnaireResponsePageComponent`:
   - Carrega `QuestionnaireResponseService.loadResponses()`
   - Merge de questões + respostas anteriores (se houver)
   - Exibe:
     - Título do questionário
     - Etapa/Iteração
     - Progresso: "1 de 15 perguntas respondidas"
     - Pergunta atual

8. **[RS]** Para cada pergunta, exibe opções:
   - **Botão "Sim"**
   - **Botão "Não"**
   - Campo "Observação" (apareça/esconda conforme resposta)

9. **[EV]** Agente lê a pergunta e clica "Sim"

10. **[RS]** Sistema habilita campo:
    - **"Evidência"** (upload ou link)
    - **"Justificativa/Detalhes"** (texto opcional)

11. **[EV]** Se deseja, agente carrega evidência:
    - Upload de arquivo (PDF, screenshot, DOC, etc.)
    - Ou cola link externo
    - Previsualização do arquivo **[FA01]**

12. **[EV]** Agente escreve observação/justificativa (opcional)

13. **[RS]** Sistema auto-salva localmente:
    - Usa `DraftCacheService.saveDraft()`
    - Armazena em `sessionStorage`
    - Indica visualmente: "Rascunho salvo às 14:32"

14. **[EV]** Agente clica "Próxima Pergunta" ou avança manualmente

15. **[RS]** Sistema carrega próxima pergunta **[FE01]** [FE02]**

    *Repete passos 8-14 para todas as perguntas*

16. **[RS]** Após responder todas:
    - Sistema exibe: "Progresso: 15 de 15 perguntas respondidas"
    - Botão "Enviar Respostas" fica ativo

17. **[EV]** Agente revisa respostas (opcional):
    - Botão "Revisar" mostra resumo de todas as respostas

18. **[EV]** Agente clica "Enviar Respostas" **[FE03]**

19. **[RS]** Sistema exibe modal de confirmação:
    - "Tem certeza que deseja enviar essas respostas? Não será possível editá-las após envio."
    - Botões: "Cancelar", "Enviar"

20. **[EV]** Agente clica "Enviar"

21. **[RS]** Sistema faz requisição `POST /api/projects/{projectId}/questionnaires/{questionnaireId}/responses`:
    ```json
    {
      "responses": [
        {
          "questionId": "uuid",
          "answer": "Sim",
          "evidence": "file_url",
          "justification": "text",
          "attachments": ["file_id_1", "file_id_2"]
        },
        {
          "questionId": "uuid",
          "answer": "Não",
          "justification": "Requisito não aplicável",
          "attachments": []
        }
      ]
    }
    ```

22. **[RS]** Backend:
    - Valida que todas as perguntas foram respondidas
    - Armazena respostas
    - Atualiza status respondente para "Respondido"
    - Calcula `ISEP` preliminar (baseado em respostas)

23. **[RS]** Sistema limpa `DraftCacheService`

24. **[RS]** Sistema exibe notificação [MSG01] e redireciona para `/projects/{projectId}`

### Fluxo Alternativo

**FA01 - Upload de Evidência:**
1. Agente clica "Anexar arquivo" ou "Upload"
2. [RS] Abre file picker
3. [EV] Agente seleciona arquivo (max 10MB)
4. [RS] Preview do arquivo
5. [EV] Agente confirma upload
6. [RS] Arquivo é enviado para storage (AWS S3, Azure Blob, etc.)
7. [RS] URL armazenada na resposta

**FA02 - Salvar como Rascunho e Sair:**
1. Agente preencheu algumas perguntas
2. [EV] Agente fecha navegador ou clica voltar
3. [RS] `DraftCacheService` salva automaticamente
4. [RS] `SessionExpirationService` monitora:
   - Se sessão expira em 5 min, auto-salva (POST /auto-save)
5. [EV] Agente retorna depois
6. [RS] Sistema carrega rascunho: "Você tem um rascunho salvo. Deseja continuar?"

**FA03 - Visualizar Respostas (Admin/Analista):**
1. Analista navega para `/projects/{projectId}/questionnaires/{questionnaireId}/responses`
2. [RS] Sistema carrega `QuestionnaireResponseService.loadAdminView()`
3. [RS] Exibe:
   - Lista de agentes com status (Respondido, Pendente)
   - Respostas de cada agente (read-only)
   - Evidências com preview
4. [EV] Analista seleciona agente para ver detalhes

### Fluxo de Exceção

**FE01 - Pergunta não respondida:**
1. Agente tenta avançar para próxima sem responder
2. [RS] Sistema exibe alerta: "Responda a pergunta para continuar"
3. Mantém na mesma pergunta

**FE02 - Sessão expirada durante resposta:**
1. Token expira enquanto agente está respondendo
2. [RS] `SessionExpirationService` tenta refresh automático
3. Se refresh falha: Sistema exibe modal "Sua sessão expirou"
4. Oferece:
   - Botão "Relogin"
   - Info: "Seu rascunho foi salvo"
5. Após login: Sistema recarrega rascunho

**FE03 - Erro ao enviar respostas:**
1. Requisição POST falha (timeout, servidor offline)
2. [RS] Exibe modal: [MSG02]
3. Oferece:
   - "Tentar novamente"
   - "Salvar como rascunho e sair"

**FE04 - Arquivo muito grande:**
1. Agente tenta upload de arquivo > 10MB
2. [RS] Exibe erro: "Arquivo muito grande (máx. 10MB)"
3. Impede upload

**FE05 - Formato de arquivo inválido:**
1. Agente tenta upload de arquivo executável ou mal suportado
2. [RS] Exibe erro: "Tipo de arquivo não suportado. Use: PDF, DOC, XLS, PNG, JPG"

**FE06 - Agente não é representante do projeto:**
1. URL manipulation: Agente tenta acessar `/projects/{OUTRO_projectId}/questionnaires/.../respond`
2. [RS] `AuthGuard` + `ProjectContextService` valida permissão
3. [RS] Redireciona para `/projects` com erro: "Você não tem permissão para responder este questionário"

### Pós-condição

- Respostas do agente foram salvas no banco de dados
- Status respondente mudou para "Respondido"
- Rascunho local foi limpo
- ISEP do questionário foi recalculado (agregação de todas respostas)
- Analista pode visualizar respostas no dashboard

### Mensagens

- **MSG01:** "Respostas salvas com sucesso! Você pode visualizá-las em qualquer momento."

- **MSG02:** "Erro ao enviar respostas. Verifique sua conexão e tente novamente."

- **MSG03:** "Existem perguntas não respondidas. Complete todas antes de enviar."

- **MSG04:** "Arquivo anexado com sucesso!"

- **MSG05:** "Sua sessão expirou. Você será redirecionado para login. Seu rascunho foi salvo."

### Observações Técnicas

- **Auto-save:** `DraftCacheService` salva a cada mudança
- **Expiração:** `SessionExpirationService` auto-salva antes de expirar
- **Upload:** Requisição multipart/form-data para arquivos
- **Storage:** Rascunhos em `sessionStorage`, permanentes em `localStorage`
- **Contexto:** `ProjectContextService` valida multi-tenancy
- **Modo Offline:** Poderia cachear questionário para responder offline (não implementado)

---

## UC06 - Visualizar Dashboard

**Ator(es):** Analista de qualidade e gerente de projeto.

**Referência:** BR07.

**Pré-condição:**
- Usuário está logado
- Projeto está em status "Aberto"
- Pelo menos alguns agentes responderam o questionário

### Fluxo Principal - Dashboard de Projeto

1. **[RS]** Usuário navega para `/projects/{projectId}`

2. **[RS]** Sistema carrega `ProjectDetailPageComponent`:
   - Exibe abas: "Visão Geral", "Questionários", "Dashboard"

3. **[EV]** Usuário clica aba "Dashboard"

4. **[RS]** Sistema redireciona para `/projects/{projectId}/dashboard` ou `/dashboard/project/{projectId}`

5. **[RS]** Sistema carrega `ProjectDashboardPageComponent`:
   - Carrega dados via `DashboardService.getProjectDashboard()`
   - Exibe:
     - **KPI Geral:** ISEP % com cor (verde/amarelo/vermelho)
     - **Banda:** Classificação A, B, C, D ou E com barra
     - **Progresso:** Questionários respondidos (5 de 12 completados)
     - **Data Última Atualização:** "Atualizado há 2 horas"

6. **[RS]** Sistema exibe seção "Questionários":
   - Tabela com colunas: Nome, Etapa, Status, ISEP, Respondentes
   - Filtros: Etapa, Status
   - Busca por nome

7. **[EV]** Usuário clica em questão específica

8. **[RS]** Sistema redireciona para `/dashboard/questionnaire/{questionnaireId}`

9. **[RS]** Sistema carrega `QuestionnaireDashboardPageComponent`:
   - Exibe ISEP específico do questionário
   - **Gráfico de Distribuição de Bandas:** Pie chart com A, B, C, D, E
   - **Heatmap Roles vs Etapas:** Grid mostrando compliance por rol
   - **Word Cloud:** Palavras-chave extraídas de justificativas (IA)
   - **Tabela de Respondentes:** Agentes com suas respostas

10. **[RS]** Sistema carrega widgets de IA (se disponível):
    - **AI Insights:** Resumo automático de achados
    - **AI Risk Report:** Áreas de risco identificadas
    - **AI Chat:** Chat para fazer perguntas sobre dados **[FA01]**

11. **[EV]** Usuário clica "Visualizar Gráficos Detalhados" **[FA02]**

12. **[RS]** Sistema expande/exibe:
    - Gráficos maiores em modo fullscreen
    - Opção de exportar como imagem/PDF
    - Opção de drill-down em dados

13. **[EV]** Usuário clica "Finalizar Iteração" (se iterativo) ou "Fechar Projeto"

14. **[RS]** Sistema exibe modal de confirmação:
    - "Tem certeza que deseja finalizar essa iteração?"
    - "Não será possível adicionar mais respostas."
    - Botões: "Cancelar", "Confirmar"

15. **[EV]** Usuário clica "Confirmar"

16. **[RS]** Sistema faz requisição `PUT /api/projects/{projectId}/iterations/{iterationId}/close` (se iterativo) ou `PUT /api/projects/{projectId}/close` (se cascata)

17. **[RS]** Backend:
    - Define status iteração para "Concluída"
    - Calcula ISEP final
    - Determina banda final
    - Se aplicável: Gera certificado ou boletim (UC07, UC08)

18. **[RS]** Sistema exibe notificação [MSG01] e atualiza dashboard

### Fluxo Alternativo

**FA01 - AI Chat Widget:**
1. Usuário digita pergunta: "Quais são os 3 maiores riscos?"
2. [RS] `AiDashboardService.askQuestion()` faz POST com pergunta
3. [RS] Backend retorna Server-Sent Events (SSE) stream
4. [RS] Resposta IA exibida em tempo real (palavra por palavra)
5. [EV] Usuário pode fazer acompanhamento ou nova pergunta

**FA02 - Exportar Dashboard:**
1. Usuário clica "Exportar"
2. [RS] Oferece opções: PDF, PNG (imagem), JSON (dados)
3. Se PDF: Usa biblioteca pdf-lib ou html2pdf
4. Se PNG: Converte gráfico para imagem
5. Se JSON: `DashboardService.exportProjectJson()` ou `.exportQuestionnaireJson()`
6. Download iniciado automaticamente

**FA03 - Dashboard Consolidado:**
1. Usuário navega para `/dashboard/consolidated/{projectId}`
2. [RS] Sistema exibe `ConsolidatedAnswersPageComponent`
3. [RS] Exibe todas as respostas agregadas de todos agentes
4. Filtra por: Pergunta, Etapa, Respondente
5. Exibe: Contagem Sim/Não, evidências relacionadas

**FA04 - Dashboard Individual:**
1. Usuário navega para `/dashboard/individual/{projectId}/{agentId}`
2. [RS] Sistema exibe `IndividualDashboardPageComponent`
3. [RS] Exibe respostas de um agente específico
4. Exibe: Histórico de respostas, padrões, tendências

### Fluxo de Exceção

**FE01 - Nenhum respondente respondeu ainda:**
1. Agente tenta acessar dashboard sem respostas
2. [RS] Sistema exibe mensagem: "Aguardando respostas dos respondentes"
3. Exibe lista de respondentes não respondidos
4. Oferece botão: "Notificar Respondentes" (UC09)

**FE02 - Erro ao carregar dados:**
1. `DashboardService.getProjectDashboard()` falha
2. [RS] Exibe skeleton loaders brevemente
3. [RS] Exibe mensagem: [MSG02]
4. Oferece botão "Tentar Novamente"

**FE03 - Projeto já foi fechado:**
1. Usuário tenta finalizar projeto já finalizado
2. [RS] Sistema exibe aviso: "Este projeto já foi encerrado"
3. Redireciona para histórico

### Pós-condição

- Iteração/Projeto foi marcado como concluído
- ISEP final foi calculado e armazenado
- Dashboard congela dados (read-only)
- Sistema pode gerar certificado ou boletim (próximos CUs)
- Projeto aparece no histórico

### Mensagens

- **MSG01:** "Iteração/Projeto marcado como concluído! Você pode acessá-lo no histórico."

- **MSG02:** "Erro ao carregar dashboard. Por favor, tente novamente mais tarde."

- **MSG03:** "Há respondentes que ainda não completaram o questionário. Deseja notificá-los?"

### Observações Técnicas

- **Gráficos:** ngx-echarts com temas customizados
- **IA:** `AiDashboardService` com SSE streaming
- **Cálculo ISEP:** Agregação de respostas (backend principalmente)
- **Export:** html2pdf, canvas-to-image, ou geradores no backend
- **Real-time:** Poderia usar WebSocket para atualizar dashboard em tempo real (não implementado)

---

## UC07 - Emitir Boletim de Não Conformidade Ética

**Ator(es):** Analista de qualidade e gerente de projeto.

**Referência:** BR08.

**Pré-condição:**
- Projeto está concluído
- Um ou mais requisitos/faixas mínimas de compliance NÃO foram alcançados
- Sistema detectou ISEP < limite mínimo aceitável (ex: < 70%)

### Fluxo Principal

1. **[RS]** Usuário no dashboard identifica que ISEP final < 70%

2. **[RS]** Sistema exibe warning na dashboard:
   - "⚠️ Compliance não atingido"
   - ISEP atual: 65%
   - Faixa mínima exigida: 70%
   - Botão: "Emitir Boletim de Não Conformidade"

3. **[EV]** Usuário clica "Emitir Boletim" **[FE01]**

4. **[RS]** Sistema faz requisição `POST /api/projects/{projectId}/non-compliance-report`:
   ```json
   {
     "projectId": "uuid",
     "iterationId": "uuid", // se iterativo
     "isepFinal": 65,
     "requiredIsep": 70,
     "gap": 5
   }
   ```

5. **[RS]** Backend:
   - Gera PDF do boletim com:
     - Logo/header
     - Data e hora
     - Nome do projeto
     - ISEP final vs. exigido
     - Gap de compliance
     - Áreas de risco
     - Recomendações
     - Assinatura digital
   - Salva boletim em storage
   - Envia email para stakeholders

6. **[RS]** Sistema exibe notificação [MSG02]

7. **[RS]** Sistema oferece:
   - Botão "Visualizar Boletim" (PDF)
   - Botão "Voltar para Relatório" 

8. **[EV]** Usuário pode:
   - Download do boletim
   - Enviar para email novamente
   - Compartilhar link

9. **[RS]** Sistema permite responder questionário novamente (iteração seguinte se aplicável)

### Fluxo Alternativo

**FA01 - Boletim com Recomendações Detalhadas:**
1. Backend analisa quais faixas/encargos tiveram baixo compliance
2. [RS] Boletim inclui:
   - "Segurança: 45% (crítico)"
   - "Privacidade: 75% (aceitável)"
   - Recomendações específicas para cada área

**FA02 - Boletim com Análise de Tendências:**
1. Se houver múltiplas iterações:
2. Boletim inclui gráfico de evolução ISEP ao longo do tempo
3. Identifica se compliance está melhorando ou piorando

### Fluxo de Exceção

**FE01 - Erro na geração do boletim:**
1. Backend falha ao gerar PDF ou enviar email
2. [RS] Exibe mensagem [MSG01]
3. Oferece:
   - Botão "Tentar Novamente"
   - Botão "Contactar Suporte"

**FE02 - Email de stakeholder inválido:**
1. Alguns emails na lista de stakeholders são inválidos
2. [RS] Boletim ainda é gerado
3. Email enviado apenas para endereços válidos
4. Exibe aviso: "Boletim gerado mas não foi possível enviar para {email}"

### Pós-condição

- Boletim de não conformidade foi emitido e armazenado
- Email foi enviado para stakeholders
- Projeto retorna para estado que permite nova rodada de respostas
- Histórico de boletins é mantido

### Mensagens

- **MSG01:** "Ocorreu um problema ao gerar o boletim. Entre em contato com a equipe de suporte e tente novamente mais tarde. Código: ERROR_REPORT_GENERATION"

- **MSG02:** "Boletim de não conformidade emitido com sucesso! Um email foi enviado para os stakeholders."

### Observações Técnicas

- **PDF Generation:** Backend usa bibliotecas como ReportLab (Python) ou iTextSharp (.NET)
- **Email:** Integração com SendGrid, AWS SES, ou similar
- **Storage:** Boletim armazenado em Azure Blob, AWS S3, etc.
- **Assinatura Digital:** Certificado X.509 para autenticidade

---

## UC08 - Emitir Certificado de Compliance Ético para Desenvolvimento de Software

**Ator(es):** Analista de qualidade e gerente de projeto.

**Referência:** BR09.

**Pré-condição:**
- Projeto está concluído
- Todos os requisitos/faixas mínimas de compliance foram alcançados
- Sistema detectou ISEP ≥ limite mínimo aceitável (ex: ≥ 70%)
- Projeto tem status "Concluído"

### Fluxo Principal

1. **[RS]** Usuário no dashboard identifica que ISEP final ≥ 70%

2. **[RS]** Sistema exibe success message na dashboard:
   - "✅ Compliance atingido!"
   - ISEP atual: 78%
   - Faixa mínima: 70%
   - Botão: "Gerar e Encaminhar Certificado de Ciência Ética"

3. **[EV]** Usuário clica "Gerar e Encaminhar Certificado" **[FE01]** **[FE02]**

4. **[RS]** Sistema valida se TODAS as faixas mínimas foram atingidas **[FE01]**

5. **[RS]** Se OK, faz requisição `POST /api/projects/{projectId}/compliance-certificate`:
   ```json
   {
     "projectId": "uuid",
     "iterationId": "uuid", // se iterativo
     "isepFinal": 78,
     "band": "A",
     "stakeholders": ["email1@company.com", "email2@company.com"]
   }
   ```

6. **[RS]** Backend:
   - Gera certificado PDF com:
     - Logo/brasão
     - Título: "Certificado de Compliance Ético"
     - Nome do Projeto
     - Data de Emissão
     - ISEP e Banda atingidos
     - Assinatura digital
     - QR Code para validação
     - Validade (ex: 1 ano)
   - Salva certificado em storage
   - Envia por email para todos stakeholders + projeto manager

7. **[RS]** Sistema exibe notificação [MSG01]

8. **[RS]** Sistema oferece:
   - Preview do certificado
   - Botão "Download Certificado"
   - Botão "Compartilhar" (gera link)
   - Botão "Voltar para Relatório"

9. **[EV]** Usuário pode visualizar/download certificado

10. **[RS]** Certificado é adicionado ao histórico do projeto

### Fluxo Alternativo

**FA01 - Certificado com Detalhe por Faixa:**
1. Se projeto tem múltiplos encargos (Segurança, Privacidade, etc.)
2. Certificado inclui:
   - Detalhamento de compliance por encargo
   - "Segurança: Nível A (95%)"
   - "Privacidade: Nível B (82%)"

**FA02 - Certificado com Assinatura Eletrônica:**
1. Sistema integrado com serviço de assinatura digital
2. Certificado assinado por responsável organizacional
3. Possibilita validação externa via portal

**FA03 - Renovação Automática:**
1. Se projeto é iterativo e completa próxima iteração com sucesso
2. Sistema oferece: "Renovar Certificado?" com botão direto

### Fluxo de Exceção

**FE01 - Uma ou mais faixas mínimas não foram atingidas:**
1. Ao validar, backend detecta que compliance < limite em alguma faixa
2. [RS] Exibe mensagem [MSG02]
3. Sistema redireciona para UC07 (Emitir Boletim)
4. Impossível gerar certificado neste momento

**FE02 - Erro na geração do certificado:**
1. Backend falha ao gerar PDF, salvar em storage ou enviar email
2. [RS] Exibe mensagem [MSG03]
3. Oferece:
   - Botão "Tentar Novamente"
   - Botão "Contactar Suporte"
   - Link para FAQ

**FE03 - Email de stakeholder inválido:**
1. Um ou mais emails não podem receber certificado
2. [RS] Certificado ainda é gerado
3. Email enviado apenas para endereços válidos
4. Aviso: "Certificado gerado mas não foi possível enviar para {email}"
5. Oferece "Reenviar para Email Específico"

### Pós-condição

- Certificado de compliance ético foi emitido
- Email foi enviado para todos stakeholders
- Certificado é armazenado no histórico do projeto
- Projeto pode ser marcado como "Concluído com Sucesso" no histórico
- QR Code no certificado permite validação

### Mensagens

- **MSG01:** "Certificado encaminhado com sucesso para os emails dos stakeholders cadastrados no sistema. Você também pode fazer download acima."

- **MSG02:** "O Sistema não pode gerar o certificado pois uma ou mais faixas mínimas de aceitação não foram atingidas. Considere emitir um Boletim de Não Conformidade."

- **MSG03:** "Ocorreu um problema ao gerar o certificado. Entre em contato com a equipe de suporte e tente novamente mais tarde. Código: ERROR_CERTIFICATE_GENERATION"

### Observações Técnicas

- **PDF Generation:** Backend gera PDF com libPDF ou similar
- **Assinatura Digital:** Certificado X.509 ou DSC (Digital Service Certificate)
- **QR Code:** Direciona para validação online (verificar autenticidade)
- **Email:** Anexa PDF certificado
- **Storage:** Backup em múltiplas regiões
- **Histórico:** Certificado rastreável e auditável

---

## UC09 - Notificar Agentes que Não Responderam o Questionário

**Ator(es):** Analista de qualidade e gerente de projeto.

**Referência:** BR10.

**Pré-condição:**
- Projeto está em progresso
- Questionário foi enviado
- Um ou mais agentes ainda não responderam

### Fluxo Principal

1. **[RS]** Usuário navega para dashboard do questionário

2. **[RS]** Sistema identifica respondentes com status "Pendente" ou "Em Progresso"

3. **[RS]** Sistema exibe widget "Respondentes Pendentes":
   - Lista agentes não respondidos
   - Email, data de convite, dias pendentes
   - Botão: "Notificar Respondentes"

4. **[EV]** Usuário clica "Notificar Respondentes" **[FE01]**

5. **[RS]** Sistema faz requisição `POST /api/projects/{projectId}/questionnaires/{questionnaireId}/notify-respondents`:
   ```json
   {
     "projectId": "uuid",
     "questionnaireId": "uuid",
     "recipientEmails": ["agent1@company.com", "agent2@company.com"]
   }
   ```

6. **[RS]** Backend:
   - Prepara email de lembrete com:
     - Nome do projeto
     - Data limite de resposta
     - Link direto para responder: `/projects/{projectId}/questionnaires/{questionnaireId}/respond`
     - Mensagem personalizada
     - Contador: "Você é um dos 3 agentes que ainda não respondeu"
   - Envia email para cada agente não respondido

7. **[RS]** Sistema exibe notificação [MSG02]

8. **[RS]** Sistema atualiza widget:
   - Exibe "Notificação enviada em [data/hora]"
   - Permite renotificar após 24 horas

### Fluxo Alternativo

**FA01 - Notificação Seletiva:**
1. Usuário seleciona apenas alguns agentes
2. Sistema permite escolher: "Notificar Todos" ou "Selecionar..."
3. Multi-select com checkboxes
4. Envia notificação apenas para selecionados

**FA02 - Mensagem Personalizada:**
1. Usuário escreve mensagem customizada
2. Pode incluir placeholders: {nome_agente}, {prazo}, {projeto}
3. Email inclui mensagem customizada

**FA03 - Renotificar Automaticamente:**
1. Sistema detecta que faltam 2 dias para deadline
2. Envia renotificação automática para não respondidos
3. Usuário pode desativar esta opção

### Fluxo de Exceção

**FE01 - Erro no envio da notificação:**
1. Serviço de email falha
2. [RS] Exibe mensagem [MSG01]
3. Oferece:
   - Botão "Tentar Novamente"
   - Opção "Usar email alternativo"

**FE02 - Nenhum agente pendente:**
1. Todos já responderam
2. [RS] Sistema desabilita botão "Notificar Respondentes"
3. Exibe mensagem: "Todos os agentes responderam!"

**FE03 - Limite de notificações atingido:**
1. Sistema limita 2 notificações por agente por questionário
2. [RS] Exibe aviso: "Limite de notificações atingido para alguns agentes"

### Pós-condição

- Email de notificação foi enviado para agentes pendentes
- Sistema registra que notificação foi enviada (data/hora)
- Agentes recebem link direto para responder
- Dashboard atualiza contador de respondentes em tempo real

### Mensagens

- **MSG01:** "Ocorreu um problema ao enviar a notificação para os agentes que ainda não responderam o questionário. Entre em contato com a equipe de suporte e tente novamente mais tarde."

- **MSG02:** "Notificação enviada com sucesso para {n} agente(s) que ainda não responderam!"

### Observações Técnicas

- **Email Templating:** Backend usa Handlebars ou similar para templates
- **Rastreamento:** Sistema registra delivery status
- **Rate Limiting:** Máximo de notificações por agente para não spam
- **Webhook:** Email service pode disparar webhook ao abrir/clicar

---

## UC10 - Alterar Peso por Membro de Equipe

**Ator(es):** Analista de qualidade.

**Referência:** BR02.

**Pré-condição:**
- Projeto está em criação ou edição
- Representantes foram adicionados
- Usuário tem permissão para editar projeto

### Fluxo Principal

1. **[RS]** Usuário está na seção "Representantes" da criação/edição de projeto

2. **[RS]** Sistema exibe tabela de representantes com colunas:
   - Nome
   - Email
   - Encargos
   - **Peso** (%)
   - Ações (Editar, Excluir)

3. **[RS]** Pesos são exibidos em formato numérico: "25%", "35%", etc.

4. **[RS]** Sistema calcula automaticamente:
   - Total de pesos deve somar 100%
   - Exibe validação: "Total: 95% (faltam 5%)" ou "Total: 100% ✓"

5. **[EV]** Usuário clica no ícone de edição (lápis) de um representante **[FA01]**

6. **[RS]** Sistema exibe modal "Editar Representante":
   - Campos: Nome, Sobrenome, Email, Peso, Encargos
   - Campo "Peso" é numérico (0-100)

7. **[EV]** Usuário modifica campo "Peso":
   - Exemplo: Muda de "25%" para "35%"

8. **[RS]** Sistema valida em tempo real:
   - Se total > 100%: Exibe aviso em vermelho
   - Se total < 100%: Exibe aviso em amarelo
   - Se total = 100%: Exibe check ✓ em verde

9. **[EV]** Usuário clica "Confirmar"

10. **[RS]** Sistema valida:
    - Total de pesos não pode ser > 100%
    - Se total < 100%: Oferece opção de "Distribuir Restante"
    - Se total = 100%: Prossegue normalmente

11. **[RS]** Sistema fecha modal e atualiza tabela

12. **[RS]** Pesos são refletidos nos cálculos de ISEP (later, no dashboard)

### Fluxo Alternativo

**FA01 - Editar Peso Inline:**
1. Usuário clica diretamente na célula "Peso"
2. [RS] Célula torna-se editável (input inline)
3. [EV] Usuário digita novo valor
4. [RS] Validação em tempo real
5. [EV] Usuário clica elsewhere ou pressiona Enter
6. [RS] Valor é salvo

**FA02 - Distribuição Automática:**
1. Usuário clica "Distribuir Igualmente" ou "Distribuir Restante"
2. [RS] Sistema calcula:
   - Restante = 100% - Total Atual
   - Distribui igualmente entre selecionados
3. Exemplo: Se 90% alocado e 2 agentes sem peso
   - Cada recebe 5% (10% / 2)

**FA03 - Importar Distribuição de Template:**
1. Usuário seleciona "Usar distribuição de template"
2. [RS] Sistema oferece template existente
3. Pesos são copiados do template (e ajustados se necessário)

### Fluxo de Exceção

**FE01 - Total de pesos > 100%:**
1. Usuário tenta salvar representantes com pesos > 100%
2. [RS] Exibe erro: "Total de pesos é 105%. Deve ser exatamente 100%."
3. Impede salvar até corrigir

**FE02 - Total de pesos < 100%:**
1. Usuário tenta salvar com pesos < 100%
2. [RS] Exibe aviso: "Total é 90%. Deseja distribuir os 10% restantes?"
3. Oferece opções:
   - "Distribuir Igualmente"
   - "Editar Manualmente"
   - "Salvar Mesmo Assim" (alguns cálculos podem sofrer)

**FE03 - Peso negativo ou não numérico:**
1. Usuário tenta digitar "-10" ou "abc"
2. [RS] Campo rejeita input
3. Aceita apenas 0-100

### Pós-condição

- Pesos de representantes foram atualizados
- Total é 100% (ou validado conforme acima)
- Pesos são considerados nos cálculos de ISEP do projeto
- Alterações são salvas quando projeto é salvo

### Mensagens

- **MSG01:** "Total de pesos é {total}%. O total deve ser 100%."

- **MSG02:** "Peso atualizado com sucesso!"

- **MSG03:** "Há {n}% ainda não alocados. Deseja distribuir aos demais membros?"

### Observações Técnicas

- **Validação:** Em tempo real com RxJS operators
- **Cálculo ISEP:** Agraga respostas por peso do representante
- **Persistência:** Salvo quando projeto é salvo (não auto-save)
- **UI:** Material Design com validação visual

---

## UC11 - Manter Questionário

**Ator(es):** Analista de qualidade.

**Referência:** BR06.

**Pré-condição:**
- Usuário está na seção "Questionários" de criação/edição de projeto
- Projeto tem etapas ou iterações definidas

### Fluxo Principal - Visão Cascata

1. **[RS]** Sistema exibe tabela de questionários com colunas:
   - Nome
   - Etapa (ex: "Design", "Desenvolvimento")
   - Faixa de Aplicação
   - Peso (%)
   - Status
   - Ações (Editar)

2. **[RS]** Tabela inicialmente vazia com botão "Importar Questionários"

3. **[EV]** Usuário clica "Importar Questionários" **[FA02]**

4. **[RS]** Sistema exibe modal "Importar Questionários":
   - Opção 1: "Importar de Template Existente"
   - Opção 2: "Importar de Arquivo Externo"
   - Botão "Cancelar"

5. **[EV]** Usuário seleciona "Importar de Template Existente"

6. **[RS]** Sistema exibe dropdown com templates:
   - "Template Segurança v1.0"
   - "Template Privacidade v2.1"
   - etc.

7. **[EV]** Usuário seleciona template

8. **[RS]** Sistema exibe preview:
   - Lista de questionários no template
   - Número de perguntas por questionário
   - Botão "Confirmar" **[FE02]** **[FE04]**

9. **[EV]** Usuário clica "Confirmar"

10. **[RS]** Sistema importa questionários:
    - Popula tabela com questionários do template
    - Associa questionários a etapas (cascata) ou iterações
    - Exibe notificação: [MSG03]

11. **[EV]** Usuário pode editar questionário importado:
    - Clica ícone de edição (lápis)

12. **[RS]** Sistema redireciona para `/projects/{projectId}/questionnaires/{questionnaireId}/edit`

13. **[RS]** Sistema carrega `QuestionnaireEditComponent`:
    - Exibe dados do questionário:
      - Nome
      - Descrição
      - Etapa (cascata) ou Iteração (iterativo)
      - Faixa de Aplicação
      - Peso (%)
    - Seção: "Perguntas" (lista de perguntas)
    - Botão: "Adicionar Pergunta"

14. **[EV]** Usuário pode editar informações do questionário

15. **[EV]** Usuário gerencia perguntas **(Ver FA04)**:
    - Adiciona novas perguntas
    - Edita perguntas existentes
    - Remove perguntas
    - Reordena (drag-and-drop)

16. **[EV]** Se iterativo, usuário pode clicar:
    - "Aplicar este questionário a todas as iterações" **[FE03]**
    - Sistema replica questionário para todas iterações

17. **[EV]** Usuário clica "Salvar" ou "Voltar"

18. **[RS]** Sistema:
    - Valida campos obrigatórios **[FE01]**
    - Salva alterações
    - Redireciona para lista de questionários

### Fluxo Principal - Visão Iterativa

*Idem Cascata, com diferenças:*

1. **[RS]** Tabela exibe:
   - Nome
   - Iteração (ex: "Iteração 1", "Iteração 2")
   - Faixa de Aplicação
   - Peso (%)
   - Status
   - Ações

16b. **[EV]** Usuário marca "Aplicar este questionário a todas as iterações"
    - Sistema replica para todas iterações do projeto
    - Exibe notificação: [MSG04]

### Fluxo Alternativo

**FA01 - Visão Cascata:**
- Se tipo de projeto é "Cascata"
- Questionários são associados a etapas específicas

**FA02 - Importar Questionários:**
1. Usuário clica "Importar questionários"
2. [RS] Sistema oferece opções:
   - Importar de template existente
   - Importar de arquivo externo (JSON/CSV)
3. Se arquivo: Sistema valida formato **[FE04]**

**FA03 - Editar Questionário:**
1. Usuário clica no ícone de edição
2. [RS] Sistema abre tela detalhada de edição
3. Usuário pode:
   - Editar nome, descrição, peso
   - Gerenciar perguntas (adicionar, editar, deletar)
   - Reordenar perguntas

**FA04 - Adicionar Nova Pergunta:**
1. Usuário clica "Adicionar Pergunta" em `/questionnaires/{id}/edit`
2. [RS] Sistema exibe modal: "Criar pergunta ou selecionar de template?"
3. Se "Criar nova":
   - [RS] Abre modal com campos:
     - Texto da pergunta
     - Tipo: (Sim/Não, Escala, Múltipla, Aberta)
     - Encargos relacionados (multi-select)
   - [EV] Usuário preenche e clica "Confirmar"
   - [RS] Pergunta é adicionada à lista
4. Se "Selecionar existente":
   - [RS] Abre interface de busca
   - [EV] Usuário busca e seleciona perguntas
   - [EV] Clica "Adicionar"

### Fluxo de Exceção

**FE01 - Campos obrigatórios não preenchidos:**
1. Ao salvar questionário, campos obrigatórios vazios
2. [RS] Exibe erro: [MSG01]
3. Destaca campos com borda vermelha
4. Bloqueia salvar

**FE02 - Erro ao importar template:**
1. Requisição para buscar template falha
2. [RS] Exibe erro: [MSG02]
3. Oferece "Tentar Novamente"

**FE03 - Erro ao aplicar questionário a todas iterações:**
1. `ProjectStore.applyQuestionnaireToAllIterations()` falha
2. [RS] Exibe erro: [MSG05]

**FE04 - Erro no arquivo importado:**
1. Arquivo JSON/CSV tem formato inválido
2. [RS] Exibe erro: [MSG06]
3. Oferece template de formato correto

**FE05 - Erro ao criar pergunta:**
1. Requisição ao backend falha
2. [RS] Exibe mensagem de erro genérica
3. Permite retry

### Pós-condição

- Questionários do projeto estão configurados
- Questionários associados a etapas (cascata) ou iterações (iterativo)
- Perguntas estão definidas
- Projeto pronto para enviar aos respondentes

### Mensagens

- **MSG01:** "Há campos obrigatórios que não foram preenchidos corretamente."

- **MSG02:** "Erro ao importar template. Por favor, tente novamente."

- **MSG03:** "Questionário importado com sucesso!"

- **MSG04:** "Questionário aplicado com sucesso a todas as iterações!"

- **MSG05:** "Erro ao aplicar questionário às iterações. Por favor, tente novamente."

- **MSG06:** "Erro ao processar arquivo importado. Verifique o formato e tente novamente."

### Observações Técnicas

- **Store:** `QuestionnaireQueryStore`, `ProjectStore`
- **Edição:** Modal ou página separada
- **Validação:** Campos obrigatórios marcados com "*"
- **Reusabilidade:** Perguntas podem ser reutilizadas de templates
- **Pesos:** Impactam cálculo de ISEP por iteração/etapa

---

## UC12 - Manter Representantes

**Ator(es):** Analista de qualidade.

**Referência:** BR02.

**Pré-condição:**
- Usuário está na seção "Representantes" de criação/edição de projeto
- Projeto tem encargos definidos

### Fluxo Principal

1. **[RS]** Sistema exibe tabela de representantes com colunas:
   - Nome
   - Sobrenome
   - Email
   - Data de Inclusão
   - Peso (%)
   - Encargos (ex: "Segurança, Privacidade")
   - Ações (Editar, Excluir)

2. **[RS]** Tabela inicialmente vazia com botão "+" (Adicionar)

3. **[EV]** Usuário clica "+" para adicionar novo representante

4. **[RS]** Sistema exibe modal "Adicionar Representante":
   - Campo: Nome (obrigatório)
   - Campo: Sobrenome (obrigatório)
   - Campo: Email (obrigatório, validação)
   - Campo: Peso (%) (obrigatório, número 0-100)
   - Multi-select: Encargos (obrigatório, pelo menos um)
   - Botão: "Cancelar", "Confirmar"

5. **[EV]** Usuário preenche campos **[FE02]**:
   - Nome: "João"
   - Sobrenome: "Silva"
   - Email: "joao@company.com"
   - Peso: "25"
   - Encargos: ["Segurança", "Testes"]

6. **[RS]** Sistema valida em tempo real:
   - Email formato válido
   - Peso é numérico (0-100)

7. **[EV]** Usuário clica "Confirmar"

8. **[RS]** Sistema valida:
   - Todos campos preenchidos
   - Email único (não duplicado)
   - Peso + pesos atuais não > 100% **[FE01]**

9. **[RS]** Sistema fecha modal e adiciona nova linha na tabela:
   - Data de Inclusão: Data de hoje (auto-preenchida)
   - Status: "Pendente Convite"

10. **[EV]** Usuário pode adicionar mais representantes (repete passos 3-9)

11. **[EV]** Após adicionar todos, usuário clica "Continuar"

12. **[RS]** Sistema valida:
    - Todos encargos do projeto estão representados **[FE01]**
    - Se algum encargo sem representante: Exibe alerta [MSG01] mas permite prosseguir

13. **[RS]** Sistema avança para próxima seção (Questionários)

### Fluxo Alternativo

**FA01 - Adicionar Novo Representante:**
1. Passos 3-9 do Fluxo Principal

**FA02 - Editar Representante:**
1. Usuário clica ícone de edição (lápis) em uma linha
2. [RS] Sistema exibe modal "Editar Representante" com dados preenchidos
3. [EV] Usuário modifica campos desejados
4. [EV] Usuário clica "Confirmar"
5. [RS] Sistema valida (mesmas validações de adição)
6. [RS] Modal fecha e tabela é atualizada

**FA03 - Excluir Representante:**
1. Usuário clica ícone de exclusão (lixeira)
2. [RS] Sistema exibe modal: "Tem certeza que deseja remover João Silva?"
3. [EV] Usuário clica "Confirmar"
4. [RS] Representante é removido da tabela
5. [RS] Se encargo fica sem representante: Exibe aviso

**FA04 - Buscar Representante Existente:**
1. Usuário clica "Importar Representantes de Projeto Anterior"
2. [RS] Sistema oferece lista de projetos anteriores
3. [EV] Usuário seleciona projeto
4. [RS] Sistema popula lista com representantes daquele projeto
5. [EV] Usuário seleciona os que deseja reusar
6. [RS] Representantes são adicionados (pesos podem variar)

**FA05 - Upload de CSV:**
1. Usuário clica "Importar de CSV"
2. [RS] Sistema oferece download de template CSV
3. [EV] Usuário preenche CSV e faz upload
4. [RS] Sistema parseia e valida
5. [RS] Se válido: Popula representantes
6. [RS] Se erro: Exibe mensagens específicas

### Fluxo de Exceção

**FE01 - Encargos não representados:**
1. Ao clicar "Continuar" com encargos sem representante
2. [RS] Exibe alerta [MSG01]: "Há encargos sem representante"
3. Lista encargos faltando
4. **Permite prosseguir** (aviso não-bloqueante, mas informativo)

**FE02 - Campos obrigatórios não preenchidos:**
1. Modal aberto, usuário tenta confirmar sem preencher campos
2. [RS] Exibe erro [MSG02]
3. Campos vazios recebem borda vermelha
4. Bloqueia confirmação

**FE03 - Email já existe:**
1. Usuário tenta adicionar email que já está na tabela
2. [RS] Exibe erro: "Este email já foi adicionado"
3. Impede adição

**FE04 - Email inválido:**
1. Usuário digita email malformado (ex: "joao@")
2. [RS] Validação rejeita em tempo real
3. Campo recebe borda vermelha

**FE05 - Peso > 100%:**
1. Total de pesos existentes + novo > 100%
2. [RS] Exibe aviso: "Pesos excedem 100%"
3. Oferece reduzir pesos automaticamente ou editar manualmente
4. **UC10 - Alterar Peso** oferece opções (distribuir, etc.)

**FE06 - Arquivo CSV inválido:**
1. Formato incorreto, colunas faltando, emails inválidos
2. [RS] Exibe erro com detalhes: "Linha 3: Email inválido"
3. Oferece redownload de template

### Pós-condição

- Lista de representantes foi configurada
- Todos representantes receberão email de convite quando projeto for criado
- Pesos são utilizados no cálculo de ISEP
- Encargos estão distribuídos entre representantes

### Mensagens

- **MSG01:** "Há encargos que não foram representados por nenhum envolvido. Encargo não representado: [Nome do encargo]. Você pode continuar, mas estes encargos não serão avaliados."

- **MSG02:** "Há campos obrigatórios que não foram preenchidos corretamente. Campos obrigatórios: Nome, Sobrenome, Email, Peso, Encargos."

- **MSG03:** "Representante adicionado com sucesso!"

- **MSG04:** "Representante removido com sucesso!"

### Observações Técnicas

- **Email Único:** Validação no frontend + backend
- **Pesos:** Agregado em UC10
- **Multi-tenancy:** Representantes associados ao projeto específico
- **Convites:** Enviados pelo backend após projeto criado
- **Papel:** Todos representantes começam com role "Respondent"

---

## UC13 - Manter Etapas

**Ator(es):** Analista de qualidade.

**Referência:** BR02.

**Pré-condição:**
- Usuário está na seção "Etapas" de criação/edição de projeto
- Usuário tem permissão para editar projeto

### Fluxo Principal

1. **[RS]** Sistema exibe seção "Etapas" com:
   - Tabela com colunas: Nome da Etapa, Peso (%), Ações (Editar, Excluir)
   - Tabela inicialmente vazia
   - Botão "+" (Adicionar Etapa)

2. **[EV]** Usuário clica botão "+"

3. **[RS]** Sistema exibe modal "Selecionar Origem da Etapa":
   - Opção 1: "Criar Nova Etapa"
   - Opção 2: "Selecionar Etapa Existente (de Template)"
   - Botões: "Cancelar"

4. **[EV]** Usuário seleciona "Criar Nova Etapa"

5. **[RS]** Sistema exibe modal "Criar Nova Etapa":
   - Campo: Nome (obrigatório, ex: "Design", "Desenvolvimento")
   - Campo: Peso (%) (obrigatório, 0-100)
   - Botões: "Cancelar", "Confirmar"

6. **[EV]** Usuário preenche:
   - Nome: "Design"
   - Peso: "25"

7. **[RS]** Sistema valida em tempo real:
   - Nome não vazio
   - Peso é número 0-100
   - Peso + pesos atuais não > 100% **[FE02]**

8. **[EV]** Usuário clica "Confirmar"

9. **[RS]** Sistema valida campos **[FE02]**

10. **[RS]** Modal fecha e nova etapa é adicionada à tabela

11. **[RS]** Sistema valida pesos:
    - Se total < 100%: Exibe aviso em amarelo
    - Se total = 100%: Exibe check ✓ em verde
    - Se total > 100%: Exibe erro em vermelho

12. **[EV]** Usuário adiciona mais etapas (repete 2-11)
    - "Desenvolvimento" - 40%
    - "Testes" - 20%
    - "Deployment" - 15%

13. **[EV]** Usuário pode editar etapa:
    - Clica ícone de edição (lápis)

14. **[RS]** Sistema habilita edição inline:
    - Células "Nome" e "Peso" ficam editáveis
    - Campo de input com bordas

15. **[EV]** Usuário modifica valores (ex: muda "Design" para "Análise e Design")

16. **[RS]** Sistema valida em tempo real **[FE02]**

17. **[EV]** Usuário clica ícone "Salvar" (disquete) para confirmar edição

18. **[RS]** Linha retorna ao modo visualização com novos valores

19. **[EV]** Usuário pode excluir etapa:
    - Clica ícone de exclusão (lixeira)

20. **[RS]** Sistema exibe modal: "Tem certeza que deseja remover 'Design'?"

21. **[EV]** Usuário clica "Confirmar"

22. **[RS]** Etapa é removida e tabela é atualizada

23. **[EV]** Ao final, usuário clica "Continuar" na seção

24. **[RS]** Sistema valida **[FE01]**:
    - Total de pesos = 100% (ou oferece ajuste)
    - Pelo menos uma etapa existe

25. **[RS]** Sistema minimiza "Etapas" e expande próxima seção

### Fluxo Alternativo

**FA01 - Criar Nova Etapa:**
1. Passos 2-11 do Fluxo Principal

**FA02 - Selecionar Etapa Existente:**
1. No passo 3, usuário seleciona "Selecionar Etapa Existente"
2. [RS] Sistema exibe modal com lista de etapas de templates
3. [EV] Usuário seleciona etapas desejadas (multi-select)
4. [RS] Preview com nome e peso
5. [EV] Usuário clica "Adicionar"
6. [RS] Etapas são adicionadas à tabela
7. Usuário pode editar pesos se necessário

**FA03 - Editar Etapa Inline:**
1. Usuário clica ícone de edição (lápis)
2. [RS] Célula fica editável
3. [EV] Usuário modifica e clica "Salvar"

**FA04 - Excluir Etapa:**
1. Usuário clica ícone de exclusão (lixeira)
2. [RS] Modal de confirmação
3. [EV] Usuário confirma
4. [RS] Etapa removida

**FA05 - Reordenar Etapas:**
1. Usuário arrasta linha para reordenar (drag-and-drop)
2. [RS] Etapas reordenadas
3. Ordem refletida nos questionários e dashboards

**FA06 - Importar Etapas de Template:**
1. Usuário clica "Importar Etapas de Template Anterior"
2. [RS] Sistema oferece lista de templates
3. [EV] Usuário seleciona template
4. [RS] Etapas do template são populadas
5. [EV] Usuário pode editar pesos

### Fluxo de Exceção

**FE01 - Campos obrigatórios não preenchidos:**
1. Modal aberto, usuário tenta confirmar sem preencher Nome ou Peso
2. [RS] Exibe erro [MSG01]
3. Campos vazios recebem borda vermelha
4. Bloqueia confirmação

**FE02 - Peso não é número ou > 100%:**
1. Usuário digita "abc" ou "150"
2. [RS] Campo rejeita ou exibe erro
3. [RS] Se total com novo peso > 100%: Exibe alerta
4. Oferece opções:
   - Reduzir automaticamente
   - Editar manualmente
   - Salvar mesmo assim

**FE03 - Nome de etapa duplicado:**
1. Usuário tenta criar etapa com mesmo nome
2. [RS] Exibe aviso: "Já existe uma etapa com este nome"
3. Oferece confirmar ou editar nome

**FE04 - Total de pesos não é 100%:**
1. Usuário clica "Continuar" com total ≠ 100%
2. [RS] Exibe erro [MSG02]
3. Oferece:
   - "Distribuir Restante" (distribui automaticamente)
   - "Editar Manualmente"
   - "Salvar Como Rascunho"

**FE05 - Nenhuma etapa foi criada:**
1. Usuário tenta continuar com tabela vazia
2. [RS] Exibe erro: "Crie pelo menos uma etapa"
3. Bloqueia avanço

### Pós-condição

- Lista de etapas foi configurada
- Total de pesos = 100%
- Etapas serão utilizadas para:
  - Associar questionários (cascata)
  - Calcular ISEP por etapa
  - Filtros em dashboards
- Projeto pode avançar para próxima seção

### Mensagens

- **MSG01:** "Há campos obrigatórios que não foram preenchidos corretamente. Campos obrigatórios: Nome, Peso."

- **MSG02:** "Total de pesos é {total}%. Deve ser 100%."

- **MSG03:** "Etapa criada com sucesso!"

- **MSG04:** "Etapa removida com sucesso!"

- **MSG05:** "Há {n}% ainda não alocados. Deseja distribuir aos pesos restantes?"

### Observações Técnicas

- **Validação de Pesos:** Em tempo real com RxJS operators
- **Armazenamento:** Etapas salvas quando projeto é salvo
- **Reuso:** Etapas podem ser templates para outros projetos
- **Cálculo ISEP:** Agregação por etapa no dashboard
- **UI:** Drag-and-drop com CDK Angular

---

# Observações Finais

## Arquitetura Implementada

O sistema foi implementado seguindo:
- **Angular 16+** com componentes standalone
- **RxJS** para reatividade
- **Padrão Store** customizado para gerenciamento de estado
- **Interceptadores HTTP** para contexto, tokens, spinner
- **Guardas de Rota** para autenticação e permissões
- **Serviços Singleton** para regras de negócio
- **SSR Ready** com verificações de browser

## Funcionalidades Avançadas

1. **Auto-save de Rascunhos:** Respostas salvas localmente antes de expiração
2. **Refresh Token Automático:** SessionExpirationService renova token antes de expirar
3. **Multi-tenancy:** Via `X-Project-Id` header
4. **IA Integrada:** SSE streaming para respostas em tempo real
5. **Exportação Flexível:** JSON, CSV, PDF (backend)
6. **Validação Forte:** Pesos, emails, senhas, campos obrigatórios

## Próximos Passos (Não Implementado)

- [ ] **UC08 Frontend:** Visualização de certificados no dashboard
- [ ] **UC07 Frontend:** Visualização de boletins
- [ ] **UC09 Frontend:** Modal de notificação com seleção
- [ ] **Offline Mode:** Funcionar sem conexão (com cache)
- [ ] **Real-time:** WebSocket para updates instantâneos
- [ ] **Acessibilidade:** WCAG 2.1 AA compliance
- [ ] **Mobile:** Layout responsivo (iniciado)

---

**Documento Atualizado:** 16 de maio de 2026
**Versão:** 2.0 (Implementação Angular)
**Status:** Pronto para Desenvolvimento Frontend
