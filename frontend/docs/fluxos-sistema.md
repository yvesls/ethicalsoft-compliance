# Fluxos do Sistema — EthicalSoft Compliance

Documento de referência para o TCC — descreve os principais fluxos operacionais do sistema de avaliação de conformidade ética em software.

---

## 1. Cadastro e Autenticação

### 1.1 Cadastro de novo usuário

O acesso ao sistema requer uma conta registrada. O fluxo de cadastro percorre as seguintes etapas:

1. O usuário acessa a tela de cadastro (`/register`) e preenche os campos:
   - **Nome** e **sobrenome**
   - **E-mail** (validado como endereço de e-mail válido)
   - **Senha** e **confirmação de senha** (validadas por regras customizadas de força e correspondência)
   - **Aceite dos termos de uso** (obrigatório; o texto pode ser lido em modal antes de confirmar)
2. O formulário só é submetido se todos os campos forem válidos; caso contrário, todos os campos inválidos são marcados com erro visualmente.
3. Ao submeter, o sistema registra o usuário e redireciona automaticamente para a tela de login.

Também é possível realizar o cadastro e o login via **Google OAuth**, renderizado diretamente no formulário. A credencial Google é capturada e enviada ao backend para autenticação federada.

### 1.2 Login

Na tela de login (`/login`), o usuário informa e-mail e senha. A opção "manter sessão" controla se o token de autenticação persiste entre sessões do navegador.

### 1.3 Recuperação de senha

Caso o usuário esqueça a senha, pode iniciar o fluxo de recuperação (`/recover-account`), que envia um código por e-mail. O código é validado na tela de verificação (`/code-verification`) e, em seguida, o usuário redefine a senha em `/reset-password`.

---

## 2. Criação de Projeto

O sistema suporta dois tipos de projeto, que diferem no modelo de processo de desenvolvimento avaliado:

| Tipo | Modelo de processo | Divisão interna |
|---|---|---|
| **Iterativo** | Ágil/Scrum | Sprints (iterações) |
| **Cascata** | Waterfall/clássico | Fases (estágios) |

Ambos os fluxos de criação são divididos em **painéis sequenciais** (wizard por etapas).

### 2.1 Projeto Iterativo

**Painel 1 — Informações básicas**
- Nome do projeto, descrição e datas de início e fim.
- Seleção do **template base** a ser utilizado. O template define o conjunto inicial de perguntas éticas que comporão os questionários.

**Painel 2 — Sprints (iterações)**
- Definição do número de sprints e nome de cada uma.
- Cada sprint resultará em um questionário independente para avaliação.

**Painel 3 — Papéis e etapas**
- Definição dos **papéis** (roles) existentes no projeto (ex.: desenvolvedor, analista, gerente).
- Definição das **etapas** (stages) do processo (ex.: levantamento de requisitos, desenvolvimento, testes).
- Papéis e etapas são usados para direcionar perguntas específicas aos respondentes corretos.

**Painel 4 — Representantes**
- Cadastro dos **representantes** (membros da equipe que responderão aos questionários).
- Cada representante é vinculado a um ou mais papéis e etapas.
- O vínculo determina quais perguntas cada representante verá em sua lista de resposta.

**Painel 5 — Perguntas e distribuição**
- Exibição das perguntas do template base, organizadas por questionário (sprint).
- As perguntas já vêm pré-distribuídas conforme a **classificação** de cada pergunta:
  - **`WHOLE_PROJECT`** — exibida apenas na primeira sprint.
  - **`RECURRING`** — exibida em todas as sprints (deduplicada automaticamente no frontend).
  - **`CURRENT_STAGE`** ou `null` — exibida somente no questionário correspondente à etapa/sprint.
- O administrador pode adicionar perguntas customizadas (`CUSTOM`) a qualquer questionário, com classificação opcional.
- Perguntas do tipo `BASE` (vindas do template) têm o texto bloqueado para edição; apenas os vínculos de papel e etapa podem ser ajustados.

### 2.2 Projeto Cascata

O fluxo de criação cascata segue a mesma estrutura de painéis, com as adaptações abaixo:

- No lugar de sprints, são definidas **fases** (ex.: Análise, Design, Implementação, Testes).
- A distribuição de perguntas segue as mesmas regras de classificação, com `WHOLE_PROJECT` restrito à primeira fase e `RECURRING` presente em todas.
- O painel de perguntas agrupa as questões por fase em vez de por sprint.

---

## 3. Acesso do Representante (Usuário Temporário)

Representantes não possuem conta permanente no sistema. O acesso é concedido de forma temporária, vinculado ao projeto:

1. O administrador cadastra o representante no projeto informando o **e-mail**.
2. O representante recebe um convite por e-mail com um link de acesso.
3. Ao acessar o link, o sistema valida o e-mail do representante contra os registros do projeto e inicia uma sessão temporária.
4. O representante é direcionado diretamente para a tela de resposta do questionário ao qual foi associado.
5. A sessão do representante é gerenciada pelo `SessionExpirationService`. Caso a sessão expire antes do envio, um rascunho local é salvo automaticamente (`saveDraftLocally`) para evitar perda de respostas.

---

## 4. Resposta ao Questionário

O fluxo de resposta é o núcleo da avaliação ética. Cada representante responde às perguntas filtradas conforme seu papel e suas etapas.

### 4.1 Visualização das perguntas

- As perguntas exibidas ao respondente são filtradas pelo backend com base nos papéis e etapas do representante.
- Cada pergunta exibe:
  - Texto da pergunta
  - Badge de classificação (`WHOLE_PROJECT`, `RECURRING`, `CURRENT_STAGE`), quando aplicável
  - Ícone de cadeado e badge "Base" para perguntas do tipo `BASE` (imutáveis)
  - Botões de resposta **Sim** e **Não**

### 4.2 Resposta Sim/Não

- **Sim** — O respondente deve fornecer uma **evidência** que comprove a conformidade (descrição e/ou URL).
- **Não** — O respondente deve fornecer uma **justificativa** que explique a não conformidade (descrição obrigatória; URL opcional).
- Ao selecionar "Sim", o campo de justificativa é limpo automaticamente; ao selecionar "Não", o campo de evidência é limpo.
- Os anexos (evidências e justificativas) são inseridos via modal de anexo, que abre em modo `positive` (evidência) ou `negative` (justificativa).

### 4.3 Regras de completude

Para que o questionário possa ser **submetido** (enviado como resposta final), **todas** as perguntas devem estar respondidas e satisfazer as regras:
- Toda resposta "Sim" precisa ter evidência com descrição preenchida.
- Toda resposta "Não" precisa ter justificativa com descrição preenchida.
- Nenhuma pergunta pode estar sem resposta (`null`).

### 4.4 Rascunho

O respondente pode salvar um **rascunho** a qualquer momento, sem precisar completar todas as perguntas. O rascunho é enviado ao backend com status `InProgress`. Apenas respostas com conteúdo são incluídas no rascunho.

Adicionalmente, ao expirar a sessão, o `SessionExpirationService` aciona automaticamente `saveDraftLocally`, que persiste as respostas no cache local do navegador (via `DraftCacheService`) para recuperação posterior.

### 4.5 Envio final

Ao submeter (botão "Enviar"), o sistema envia todas as respostas com status `Completed`. O questionário é marcado como concluído para aquele representante e não pode ser editado novamente. Após o envio bem-sucedido, o rascunho em cache local é removido.

---

## 5. Encerramento do Questionário

O questionário de uma sprint/fase pode ser encerrado de duas formas:

1. **Encerramento automático** — quando **todos** os representantes associados ao questionário enviam suas respostas com status `Completed`.
2. **Encerramento forçado pelo administrador** — o administrador pode encerrar o questionário mesmo que nem todos os representantes tenham respondido. Isso é necessário quando um representante está indisponível ou quando o prazo é atingido.

Após o encerramento, o backend calcula o **ISEQ** (Índice de Software Ético do Questionário) para aquele questionário, atribuindo uma banda de conformidade.

---

## 6. Dashboard e Métricas

O sistema oferece múltiplas visões de dashboard após o cálculo do ISEQ.

### 6.1 Dashboard de Projeto

Acessível pelo administrador, consolida os resultados de **todos** os questionários do projeto.

Métricas exibidas:
- **ISEP do projeto** (`projectIsep` / `projectIsepPercent`) — índice consolidado de conformidade ética de todos os questionários
- **Banda do projeto** (`projectBand`) — classificação de A a E
- **Média simples da equipe** e **desvio padrão** (em percentual)
- **Histórico de ISEQ por questionário** — evolução ao longo das sprints/fases
- **Pontuações por dimensão ética**: ética geral, processos, equidade (fairness) e ESG
- **Dívida ética** e **dívida técnica** (em percentual)
- **Contagem de questionários** (total e concluídos)

O botão de **encerrar projeto** é habilitado enquanto `projectStatus !== 'CONCLUIDO'`. Ao encerrar, o backend calcula o ISEP final consolidado e transiciona o projeto para o status `CONCLUIDO`.

O botão de **emitir certificado** só é exibido quando `projectIsep !== null` **e** `projectStatus === 'CONCLUIDO'`.

### 6.2 Dashboard de Questionário

Visão detalhada de uma sprint/fase específica.

Métricas exibidas:
- **ISEQ** do questionário (`iseq` / `iseqPercent`) e banda
- **Distribuição de bandas** entre os representantes (gráfico de pizza/barras)
- **Resultados por membro** (`memberResults`): ICP (Índice de Conformidade Pessoal) e banda de cada representante
- **Resultados por etapa** (`stageResults`): IEM (Índice de Ética por Membro/Estágio) e banda por papel/etapa
- **Nuvem de palavras** gerada a partir das justificativas dos respondentes
- **Justificativas textuais** consolidadas
- **Pontuações por dimensão**: ética geral, processos, equidade, ESG
- **Dívida ética** e **dívida técnica**
- **Heatmap papel × etapa**: cruzamento visual de conformidade entre papéis e etapas
- **Painel de IA**: insights gerados automaticamente, chat contextual e relatório de risco com base nos resultados

### 6.3 Dashboard Individual

Visão do representante sobre seu próprio histórico de conformidade.

Métricas exibidas:
- **ICP pessoal** e banda
- **Média histórica pessoal**
- **Comparativo anônimo com a média da equipe**
- **Evolução pessoal** ao longo dos questionários (gráfico de linha)

### 6.4 Bandas de conformidade

| Banda | Faixa de ISEP | Interpretação |
|---|---|---|
| **A** | 90–100% | Excelente — atende com ampla margem |
| **B** | 75–89,9% | Bom — atende o mínimo com folga |
| **C** | 60–74,9% | Satisfatório — atende o mínimo |
| **D** | 45–59,9% | Insatisfatório — não atende |
| **E** | < 45% | Crítico — muito abaixo do mínimo |

Bandas A, B e C indicam conformidade mínima atingida. Bandas D e E indicam não conformidade.

---

## 7. Emissão de Documentos

### 7.1 Boletins de Não Conformidade

Boletins são gerados quando o resultado de um questionário é **não conforme** (bandas D ou E). Os botões de emissão ficam visíveis apenas nesses casos.

Ações disponíveis no dashboard de questionário:
1. **Baixar PDF** — gera e faz download do boletim de não conformidade em formato PDF.
2. **Emitir para representantes** — envia o boletim por e-mail para todos os representantes do questionário.
3. **Registrar emissão** — registra formalmente no sistema que o boletim foi emitido, sem envio de e-mail.

Ao emitir para representantes, o sistema retorna:
- `documentCode` — código único do documento emitido
- `totalRecipients` — total de destinatários
- `sent` — quantos e-mails foram enviados com sucesso
- `skipped` — quantos foram ignorados (ex.: e-mail já enviado anteriormente)

### 7.2 Certificados de Conformidade

Certificados são emitidos quando o **projeto** é encerrado com conformidade. O botão de certificado só aparece quando:
- O ISEP do projeto foi calculado (`projectIsep !== null`)
- O projeto está com status `CONCLUIDO`

O certificado comprova formalmente que o projeto atingiu o índice mínimo de conformidade ética e pode ser emitido para representantes ou baixado como PDF, de forma análoga ao boletim.

---

## 8. Visão Geral do Fluxo Completo

```
[Cadastro] → [Login]
     ↓
[Criar Projeto] → (Iterativo ou Cascata)
     ↓
[Configurar Sprints/Fases + Papéis + Etapas + Representantes + Perguntas]
     ↓
[Representantes recebem convite por e-mail]
     ↓
[Acesso temporário] → [Responder Questionário]
     |                       ↓
     |              [Salvar Rascunho] (a qualquer momento)
     |                       ↓
     └──────────────→ [Enviar Respostas] (quando todas completas)
                             ↓
              [Todos responderam?] ──Sim──→ [Encerramento automático]
                    |                                 ↓
                    Não                    [ISEQ calculado pelo backend]
                    ↓                                 ↓
           [Administrador encerra      [Dashboard de Questionário]
            o questionário manualmente]        ↓
                                     (Banda D/E?) → [Emitir Boletim]
                                     (Banda A-C?) → (nenhum boletim)
                                              ↓
                             [Todos os questionários encerrados?]
                                              ↓
                                   [Encerrar Projeto] → [ISEP do Projeto calculado]
                                              ↓
                                   [Dashboard de Projeto]
                                              ↓
                                   (Projeto CONCLUIDO + ISEP calculado)
                                              ↓
                                   [Emitir Certificado de Conformidade]
```

---

*Documento gerado com base na implementação do frontend Angular 19 + backend Spring Boot do sistema EthicalSoft Compliance.*
