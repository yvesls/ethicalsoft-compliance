# APÊNDICE II - DESCRIÇÃO EXPANDIDA DOS CASOS DE USO

> Documento atualizado para refletir a interface atual do frontend Angular do sistema em 20/06/2026.
> Esta versão descreve apenas fluxos efetivamente expostos pela aplicação atual.
> O fluxo de arquivamento de projeto não está disponível na interface atual e, por isso, não integra esta versão do apêndice.

---

## UC01 - Registrar-se no Sistema

**Ator(es):** Novo usuário.

**Referência:** Não há.

**Pré-condição:** O usuário não possui sessão autenticada.

### Fluxo Principal

1. **[EV]** O usuário acessa a tela de login e clica em Registre-se.
2. **[RS]** O sistema exibe a tela de cadastro.
3. **[EV]** O usuário preenche os campos Nome, Sobrenome, E-mail, Senha e Confirmar Senha. **[FE01]** **[FE02]** **[FE03]**
4. **[EV]** O usuário lê os termos de participação e marca o aceite. **[FE04]**
5. **[EV]** O usuário clica em Cadastrar. **[FE05]**
6. **[RS]** O sistema registra a conta, exibe a mensagem **[MSG01]** e redireciona o usuário para a tela de login.

### Fluxo Alternativo

**FA01 - Feedback de senha durante o preenchimento:**

1. Durante o preenchimento da senha, o sistema valida os critérios mínimos em tempo real.
2. **[RS]** O sistema informa ao usuário se a senha atende aos critérios de segurança exigidos.

### Fluxo de Exceção

**FE01 - E-mail já cadastrado:**

1. No passo 5 do Fluxo Principal, o backend rejeita o cadastro por duplicidade.
2. **[RS]** O sistema exibe a mensagem **[MSG02]**.

**FE02 - Senhas não coincidem:**

1. No passo 3 do Fluxo Principal, os campos Senha e Confirmar Senha divergem.
2. **[RS]** O sistema exibe a mensagem **[MSG03]**.

**FE03 - Senha fraca:**

1. No passo 3 do Fluxo Principal, a senha não atende aos critérios mínimos.
2. **[RS]** O sistema informa a regra não atendida.

**FE04 - Termos não aceitos:**

1. No passo 4 do Fluxo Principal, o usuário não marca o aceite.
2. **[RS]** O botão de cadastro permanece desabilitado e o envio não é realizado.

**FE05 - Erro no cadastro:**

1. No passo 5 do Fluxo Principal, ocorre falha na operação de registro.
2. **[RS]** O sistema exibe a mensagem retornada pela API.

### Pós-condição

Uma nova conta foi criada e o usuário pode autenticar-se no sistema.

### Mensagens

- **MSG01:** Registrado com sucesso.
- **MSG02:** E-mail já cadastrado.
- **MSG03:** As senhas não coincidem.

---

## UC02 - Realizar Login com E-mail e Senha

**Ator(es):** Representante e/ou Administrador de projetos.

**Referência:** BR03.

**Pré-condição:** O usuário possui cadastro ativo e está na tela de login.

### Fluxo Principal

1. **[EV]** O usuário preenche o campo E-mail.
2. **[EV]** O usuário preenche o campo Senha.
3. **[EV]** Opcionalmente, o usuário marca Manter sessão.
4. **[EV]** O usuário clica em Entrar. **[FE01]**
5. **[RS]** O sistema autentica o usuário, armazena access token e refresh token de acordo com a opção de persistência de sessão e identifica seus papéis.
6. **[RS]** Se o usuário estiver com primeiro acesso pendente, o sistema redireciona para Configurações > Trocar senha. Caso contrário, redireciona para a página inicial.

### Fluxo Alternativo

**FA01 - Manter sessão ativa:**

1. No passo 3 do Fluxo Principal, o usuário marca Manter sessão.
2. **[RS]** O sistema persiste os tokens em armazenamento durável e tenta renovar a sessão automaticamente enquanto o refresh token for válido.

### Fluxo de Exceção

**FE01 - Credenciais inválidas:**

1. No passo 4 do Fluxo Principal, o backend rejeita a autenticação.
2. **[RS]** O sistema exibe a mensagem **[MSG01]**.

### Pós-condição

Uma sessão autenticada foi iniciada e o contexto do usuário foi carregado.

### Mensagens

- **MSG01:** E-mail ou senha incorretos.

---

## UC03 - Realizar Login com Google

**Ator(es):** Representante e/ou Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está sem sessão autenticada e possui conta Google elegível para autenticação.

### Fluxo Principal

1. **[EV]** O usuário acessa a tela de login ou cadastro.
2. **[EV]** O usuário clica no botão do Google.
3. **[RS]** O sistema inicializa o provedor Google Identity Services e apresenta o fluxo de autenticação do Google.
4. **[EV]** O usuário seleciona sua conta Google e concede a autenticação.
5. **[RS]** O sistema envia o token de identidade ao backend, autentica o usuário e armazena os tokens da sessão.
6. **[RS]** Se houver primeiro acesso pendente, o sistema redireciona para Configurações > Trocar senha; caso contrário, redireciona para a página inicial.

### Fluxo Alternativo

**FA01 - Primeiro acesso via Google:**

1. Após o passo 5 do Fluxo Principal, o sistema identifica que a conta ainda precisa concluir aceite de termos.
2. **[RS]** O sistema redireciona o usuário para o fluxo de primeiro acesso em Configurações.

### Fluxo de Exceção

**FE01 - Falha na autenticação federada:**

1. No passo 5 do Fluxo Principal, o backend ou o provedor externo rejeita o token recebido.
2. **[RS]** O sistema exibe a mensagem de erro retornada pela API.

### Pós-condição

Uma sessão autenticada por provedor Google foi iniciada.

### Mensagens

Não há mensagens fixas além das notificações de erro retornadas pela API.

---

## UC04 - Recuperar Conta e Redefinir Senha

**Ator(es):** Usuário sem sessão autenticada.

**Referência:** Não há.

**Pré-condição:** O usuário está na tela de login e não consegue acessar a conta.

### Fluxo Principal

1. **[EV]** O usuário clica em Esqueci minha senha.
2. **[RS]** O sistema exibe a tela de recuperação de conta.
3. **[EV]** O usuário informa o e-mail e solicita o envio do código. **[FE01]**
4. **[RS]** O sistema envia um código de verificação e redireciona para a tela de confirmação do código.
5. **[EV]** O usuário informa o código recebido. **[FE02]**
6. **[RS]** O sistema valida o código e redireciona para a tela de redefinição de senha.
7. **[EV]** O usuário informa a nova senha e sua confirmação. **[FE03]** **[FE04]**
8. **[EV]** O usuário confirma a redefinição.
9. **[RS]** O sistema redefine a senha, exibe a mensagem **[MSG01]** e redireciona para o login.

### Fluxo Alternativo

**FA01 - Colagem do código completo:**

1. No passo 5 do Fluxo Principal, o usuário cola o código completo de verificação.
2. **[RS]** O sistema distribui automaticamente os dígitos nos campos do formulário.

### Fluxo de Exceção

**FE01 - E-mail inválido ou indisponível:**

1. No passo 3 do Fluxo Principal, o endereço informado não é aceito pelo backend.
2. **[RS]** O sistema exibe a mensagem correspondente.

**FE02 - Código inválido ou expirado:**

1. No passo 5 do Fluxo Principal, o código informado não é validado.
2. **[RS]** O sistema exibe a mensagem **[MSG02]**.

**FE03 - Senhas não coincidem:**

1. No passo 7 do Fluxo Principal, os campos de senha divergem.
2. **[RS]** O sistema exibe a mensagem **[MSG03]**.

**FE04 - Senha fora dos critérios de segurança:**

1. No passo 7 do Fluxo Principal, a nova senha é considerada fraca.
2. **[RS]** O sistema exibe a regra de validação não atendida.

### Pós-condição

A senha do usuário foi redefinida e a conta pode ser acessada normalmente.

### Mensagens

- **MSG01:** Senha redefinida com sucesso.
- **MSG02:** Código inválido. Reenvie o código ou informe outro e-mail.
- **MSG03:** As senhas não coincidem.

---

## UC05 - Alterar Senha e Concluir Primeiro Acesso

**Ator(es):** Representante e/ou Administrador de projetos autenticado.

**Referência:** Não há.

**Pré-condição:** O usuário está autenticado e acessa Configurações > Trocar senha.

### Fluxo Principal

1. **[EV]** O usuário acessa Configurações.
2. **[EV]** O usuário clica em Trocar senha.
3. **[RS]** O sistema exibe o formulário com Senha atual, Nova senha e Confirmar nova senha.
4. **[RS]** Se o usuário estiver em primeiro acesso, o sistema também exibe modal explicativo e o aceite dos termos. **[FA01]**
5. **[EV]** O usuário preenche os campos necessários. **[FE01]** **[FE02]** **[FE03]**
6. **[EV]** O usuário confirma a operação.
7. **[RS]** O sistema valida a senha atual, redefine a senha com indicador de primeiro acesso e, quando aplicável, marca o aceite de termos.
8. **[RS]** O sistema renova o token da sessão, exibe a mensagem **[MSG01]** e redireciona o usuário para Configurações.

### Fluxo Alternativo

**FA01 - Primeiro acesso de usuário autenticado via Google:**

1. No passo 4 do Fluxo Principal, o sistema identifica que o usuário é do provedor Google.
2. **[RS]** O sistema exibe apenas o formulário de aceite de termos, sem solicitar senha atual ou nova senha.
3. **[EV]** O usuário aceita os termos e confirma.
4. **[RS]** O sistema conclui o primeiro acesso, renova a sessão, exibe a mensagem **[MSG02]** e redireciona para a página inicial.

### Fluxo de Exceção

**FE01 - Senha atual incorreta:**

1. No passo 7 do Fluxo Principal, a reautenticação falha.
2. **[RS]** O sistema exibe a mensagem **[MSG03]**.

**FE02 - Nova senha fraca:**

1. No passo 5 do Fluxo Principal, a senha não atende aos critérios mínimos.
2. **[RS]** O sistema exibe a regra de validação não atendida.

**FE03 - Confirmação divergente:**

1. No passo 5 do Fluxo Principal, Nova senha e Confirmar nova senha divergem.
2. **[RS]** O sistema exibe a mensagem de validação correspondente.

### Pós-condição

A senha foi atualizada ou o primeiro acesso foi concluído com aceite de termos.

### Mensagens

- **MSG01:** Senha atualizada com sucesso.
- **MSG02:** Termos aceitos com sucesso. Bem-vindo ao sistema.
- **MSG03:** Senha atual não confere.

---

## UC06 - Listar e Filtrar Projetos

**Ator(es):** Representante e/ou Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está autenticado e possui acesso ao menu Projetos.

### Fluxo Principal

1. **[EV]** O usuário acessa Projetos pelo menu principal.
2. **[RS]** O sistema carrega a listagem paginada de projetos.
3. **[EV]** O usuário pode filtrar por Nome, Código, Tipo e Status.
4. **[EV]** O usuário aplica os filtros desejados.
5. **[RS]** O sistema atualiza a listagem conforme os critérios informados.
6. **[EV]** O usuário navega entre as páginas de resultado.
7. **[EV]** O usuário seleciona um projeto da lista para visualizar seus detalhes.

### Fluxo Alternativo

**FA01 - Criar novo projeto a partir da listagem:**

1. Na tela de listagem, um administrador clica em Criar projeto.
2. **[RS]** O sistema redireciona para o fluxo de criação de projeto.

### Fluxo de Exceção

**FE01 - Falha ao carregar projetos:**

1. No passo 2 do Fluxo Principal, ocorre erro de comunicação com a API.
2. **[RS]** O sistema exibe mensagem de falha de carregamento.

### Pós-condição

O usuário visualizou a lista de projetos e pode iniciar navegação para um item específico.

### Mensagens

Não há mensagens fixas além das mensagens de erro de carregamento.

---

## UC07 - Criar Projeto

**Ator(es):** Administrador de projetos.

**Referência:** BR02.

**Pré-condição:** O usuário está autenticado com perfil administrativo.

### Fluxo Principal

1. **[EV]** O usuário acessa Projetos e clica em Criar projeto.
2. **[RS]** O sistema exibe o seletor de tipo de projeto com as opções Cascata e Iterativo.
3. **[EV]** O usuário escolhe o tipo do projeto. **[FA01]** **[FA02]**
4. **[RS]** O sistema exibe o formulário correspondente com as seções Projeto, Etapas, Representantes e Questionários.
5. **[EV]** O usuário preenche os dados básicos do projeto, inclusive template quando desejar reutilizar uma estrutura existente. **[FA03]** **[FE01]**
6. **[EV]** O usuário configura etapas, representantes e questionários. **[FE02]**
7. **[EV]** O usuário clica em Salvar projeto.
8. **[RS]** O sistema exibe uma confirmação informando que criará o projeto e também um template derivado dele.
9. **[EV]** O usuário confirma a criação.
10. **[RS]** O sistema cria o projeto, gera automaticamente um template a partir da configuração informada, exibe a mensagem **[MSG01]** e redireciona para a listagem de projetos.

### Fluxo Alternativo

**FA01 - Projeto Cascata:**

1. No passo 3 do Fluxo Principal, o usuário seleciona Cascata.
2. **[RS]** O sistema exige nome, data de início e prazo limite, e organiza o projeto em etapas com sequência e duração em dias úteis.

**FA02 - Projeto Iterativo:**

1. No passo 3 do Fluxo Principal, o usuário seleciona Iterativo.
2. **[RS]** O sistema exige nome, data de início e duração das iterações, calcula o número de iterações e utiliza questionários associados às iterações.

**FA03 - Salvar como rascunho:**

1. Antes do passo 7 do Fluxo Principal, o usuário clica em Salvar como Rascunho.
2. **[RS]** O sistema salva o projeto com status Rascunho, exibe a mensagem **[MSG02]** e permite continuidade futura da configuração.

### Fluxo de Exceção

**FE01 - Dados básicos insuficientes:**

1. No passo 5 do Fluxo Principal, faltam campos obrigatórios ou há conflito de datas.
2. **[RS]** O sistema informa os problemas encontrados e impede a criação.

**FE02 - Estrutura inválida do projeto:**

1. No passo 6 do Fluxo Principal, não há etapas, representantes ou perguntas suficientes, ou há inconsistências entre papéis, etapas e questionários.
2. **[RS]** O sistema exibe a relação de pendências e impede a criação.

### Pós-condição

Um novo projeto foi criado ou salvo como rascunho.

### Mensagens

- **MSG01:** Projeto criado e template gerado com sucesso.
- **MSG02:** Rascunho salvo com sucesso.

---

## UC08 - Editar Projeto

**Ator(es):** Administrador de projetos.

**Referência:** BR02.

**Pré-condição:** O usuário está autenticado com perfil administrativo e o projeto existe.

### Fluxo Principal

1. **[EV]** O usuário acessa os detalhes do projeto e clica em Editar projeto.
2. **[RS]** Se o projeto estiver em Rascunho, o sistema reabre o fluxo de criação com os dados preenchidos; se estiver Aberto, o sistema abre a tela dedicada de edição.
3. **[EV]** O usuário altera dados do projeto, etapas, representantes e questionários.
4. **[EV]** O usuário solicita o salvamento.
5. **[RS]** O sistema valida as alterações, consolida o impacto da operação e persiste as mudanças.
6. **[RS]** O sistema exibe a mensagem **[MSG01]** e redireciona para a tela de detalhes do projeto.

### Fluxo Alternativo

**FA01 - Projeto em rascunho:**

1. No passo 2 do Fluxo Principal, o projeto está em Rascunho.
2. **[RS]** O sistema reaproveita o mesmo fluxo de configuração utilizado na criação.

### Fluxo de Exceção

**FE01 - Campos obrigatórios ou estrutura inválida:**

1. No passo 5 do Fluxo Principal, a edição viola regras de consistência.
2. **[RS]** O sistema informa as pendências e não conclui o salvamento.

**FE02 - Erro ao persistir alterações:**

1. No passo 5 do Fluxo Principal, ocorre erro de backend.
2. **[RS]** O sistema exibe a mensagem retornada pela API.

### Pós-condição

As alterações válidas do projeto foram salvas.

### Mensagens

- **MSG01:** Projeto atualizado com sucesso.

---

## UC09 - Publicar Projeto

**Ator(es):** Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O projeto está com status Rascunho e o usuário está na tela de detalhes do projeto.

### Fluxo Principal

1. **[EV]** O usuário clica em Publicar projeto.
2. **[RS]** O sistema exibe uma confirmação de publicação.
3. **[EV]** O usuário confirma a operação.
4. **[RS]** O sistema altera o status do projeto para Aberto, recarrega os dados e exibe a mensagem **[MSG01]**.

### Fluxo Alternativo

Não há.

### Fluxo de Exceção

**FE01 - Erro na publicação:**

1. No passo 4 do Fluxo Principal, a API rejeita a operação.
2. **[RS]** O sistema exibe a mensagem de erro retornada.

### Pós-condição

O projeto foi publicado e passa a aceitar o fluxo operacional normal.

### Mensagens

- **MSG01:** Projeto publicado com sucesso.

---

## UC10 - Excluir Projeto

**Ator(es):** Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está na tela de detalhes do projeto e o projeto não está concluído.

### Fluxo Principal

1. **[EV]** O usuário clica em Excluir projeto.
2. **[RS]** O sistema exibe mensagem de confirmação com o nome do projeto.
3. **[EV]** O usuário confirma a exclusão.
4. **[RS]** O sistema remove o projeto, exibe a mensagem **[MSG01]** e redireciona para a listagem de projetos.

### Fluxo Alternativo

Não há.

### Fluxo de Exceção

**FE01 - Erro na exclusão:**

1. No passo 4 do Fluxo Principal, ocorre erro no backend.
2. **[RS]** O sistema exibe a mensagem retornada pela API.

### Pós-condição

O projeto foi removido do sistema.

### Mensagens

- **MSG01:** Projeto excluído com sucesso.

---

## UC11 - Visualizar Detalhes do Projeto

**Ator(es):** Representante e/ou Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está autenticado e o projeto existe.

### Fluxo Principal

1. **[EV]** O usuário seleciona um projeto na listagem.
2. **[RS]** O sistema exibe os dados gerais do projeto, seu status, situação atual e a lista paginada de questionários.
3. **[EV]** O usuário pode filtrar os questionários por Nome, Etapa ou Iteração, conforme o tipo do projeto.
4. **[RS]** O sistema atualiza a lista conforme os filtros aplicados.
5. **[EV]** O usuário escolhe uma ação disponível para um questionário ou para o projeto.

### Fluxo Alternativo

**FA01 - Ações disponíveis para representante:**

1. O usuário pode responder o questionário quando estiver associado e houver pendência.
2. O usuário pode visualizar respostas já enviadas.
3. O usuário pode acessar seu painel individual quando houver dashboard calculado.

**FA02 - Ações disponíveis para administrador:**

1. O usuário pode editar, publicar ou excluir o projeto.
2. O usuário pode copiar o link de um questionário.
3. O usuário pode entrar em modo administrativo de visualização do questionário.
4. O usuário pode enviar lembretes, reagendar questionários, forçar encerramento e abrir dashboards.

### Fluxo de Exceção

**FE01 - Falha ao carregar projeto ou questionários:**

1. Nos passos 2 ou 4 do Fluxo Principal, ocorre erro de leitura.
2. **[RS]** O sistema exibe mensagem de falha e disponibiliza ação de nova tentativa.

### Pós-condição

O usuário visualizou o estado operacional do projeto e pode disparar fluxos complementares.

### Mensagens

Não há mensagens fixas além das mensagens de erro e confirmação da própria tela.

---

## UC12 - Manter Etapas

**Ator(es):** Administrador de projetos.

**Referência:** BR02.

**Pré-condição:** O usuário está no formulário de criação ou edição de projeto.

### Fluxo Principal

1. **[RS]** O sistema exibe a lista de etapas já configuradas.
2. **[EV]** O usuário escolhe adicionar, editar ou excluir uma etapa. **[FA01]** **[FA02]** **[FA03]**
3. **[RS]** O sistema valida os dados da etapa e atualiza a lista.

### Fluxo Alternativo

**FA01 - Adicionar etapa:**

1. O usuário clica em Adicionar etapa.
2. **[RS]** O sistema abre o modal correspondente ao tipo do projeto.
3. **[EV]** O usuário informa os campos obrigatórios.
4. **[RS]** O sistema adiciona a etapa à lista.

**FA02 - Editar etapa:**

1. O usuário clica em Editar na etapa desejada.
2. **[RS]** O sistema abre o modal com os dados preenchidos.
3. **[EV]** O usuário altera os dados e confirma.
4. **[RS]** O sistema atualiza a etapa na lista.

**FA03 - Excluir etapa:**

1. O usuário clica em Excluir na etapa desejada.
2. **[RS]** O sistema remove a etapa da configuração atual.

### Fluxo de Exceção

**FE01 - Campos inválidos:**

1. No passo 3 do Fluxo Principal, a etapa possui nome inválido, peso inadequado, sequência duplicada ou dados insuficientes.
2. **[RS]** O sistema exibe as mensagens de validação e não conclui a operação.

### Pós-condição

A estrutura de etapas do projeto foi atualizada.

### Mensagens

Não há mensagens globais fixas além das validações do modal.

---

## UC13 - Manter Representantes

**Ator(es):** Administrador de projetos.

**Referência:** BR02.

**Pré-condição:** O usuário está no formulário de criação ou edição de projeto.

### Fluxo Principal

1. **[RS]** O sistema exibe a lista de representantes configurados.
2. **[EV]** O usuário escolhe adicionar, editar ou excluir um representante. **[FA01]** **[FA02]** **[FA03]**
3. **[RS]** O sistema atualiza a lista com nome, sobrenome, e-mail, peso, papéis e data de inclusão.

### Fluxo Alternativo

**FA01 - Adicionar representante:**

1. O usuário clica em Adicionar representante.
2. **[RS]** O sistema abre um modal com os campos obrigatórios.
3. **[EV]** O usuário informa nome, sobrenome, e-mail, peso e ao menos um encargo.
4. **[RS]** O sistema adiciona o representante à lista.

**FA02 - Editar representante:**

1. O usuário clica em Editar no representante desejado.
2. **[RS]** O sistema abre o modal com os dados atuais.
3. **[EV]** O usuário altera os dados e confirma.
4. **[RS]** O sistema atualiza a linha correspondente.

**FA03 - Excluir representante:**

1. O usuário remove o representante da configuração atual.
2. **[RS]** O sistema atualiza a lista.

### Fluxo de Exceção

**FE01 - Dados obrigatórios inválidos:**

1. No passo 3 do Fluxo Principal, há campos ausentes ou fora do intervalo aceito.
2. **[RS]** O sistema mantém o modal aberto e exibe as mensagens de validação.

### Pós-condição

A composição de representantes do projeto foi atualizada.

### Mensagens

Não há mensagens globais fixas além das validações do modal.

---

## UC14 - Manter Questionários

**Ator(es):** Administrador de projetos.

**Referência:** BR02.

**Pré-condição:** O usuário está no formulário de criação ou edição de projeto.

### Fluxo Principal

1. **[RS]** O sistema exibe a lista de questionários do projeto.
2. **[EV]** O usuário escolhe adicionar, editar ou excluir um questionário. **[FA01]** **[FA02]** **[FA03]**
3. **[RS]** O sistema atualiza a configuração do questionário com seu nome, período e perguntas.

### Fluxo Alternativo

**FA01 - Adicionar questionário:**

1. O usuário clica em Adicionar questionário.
2. **[RS]** O sistema abre o formulário específico do tipo de projeto.
3. **[EV]** O usuário informa os dados do questionário e gerencia suas perguntas.
4. **[RS]** O sistema grava o questionário na configuração atual do projeto.

**FA02 - Editar questionário:**

1. O usuário clica em Editar no questionário desejado.
2. **[RS]** O sistema reabre o formulário com os dados atuais.
3. **[EV]** O usuário atualiza o questionário e salva.
4. **[RS]** O sistema atualiza a configuração do projeto.

**FA03 - Gerenciar perguntas do questionário:**

1. No formulário do questionário, o usuário pode criar pergunta nova, reutilizar perguntas existentes, editar perguntas e remover perguntas selecionadas.
2. **[RS]** O sistema atualiza a lista de perguntas associadas ao questionário.

### Fluxo de Exceção

**FE01 - Questionário sem perguntas ou dados inválidos:**

1. No passo 3 do Fluxo Principal, o questionário não possui perguntas válidas ou tem campos obrigatórios pendentes.
2. **[RS]** O sistema impede o salvamento e informa as inconsistências.

### Pós-condição

Os questionários do projeto foram configurados com sua estrutura de aplicação e perguntas.

### Mensagens

Não há mensagens globais fixas além das validações e avisos do formulário.

---

## UC15 - Responder Questionário

**Ator(es):** Representante.

**Referência:** Não há.

**Pré-condição:** O usuário está autenticado, associado ao questionário e possui permissão de resposta.

### Fluxo Principal

1. **[EV]** Na tela de detalhes do projeto, o usuário clica em Responder agora.
2. **[RS]** O sistema carrega o questionário, as perguntas, o status do participante e o progresso de respostas.
3. **[EV]** Para cada pergunta, o usuário informa Sim ou Não. **[FA01]** **[FA02]**
4. **[EV]** O usuário pode salvar rascunho a qualquer momento. **[FA03]**
5. **[EV]** Ao concluir, o usuário clica em Enviar respostas. **[FE01]**
6. **[RS]** O sistema valida o preenchimento, persiste as respostas, exibe a mensagem **[MSG01]** e retorna à tela de detalhes do projeto.

### Fluxo Alternativo

**FA01 - Adicionar evidência ou justificativa:**

1. No passo 3 do Fluxo Principal, o usuário abre o modal de anotações da resposta.
2. **[EV]** O usuário informa observações e, opcionalmente, links de evidência.
3. **[RS]** O sistema associa a anotação à resposta.

**FA02 - Revisar respostas já preenchidas:**

1. Durante o fluxo, o usuário pode voltar às perguntas anteriores.
2. **[RS]** O sistema mantém em memória local e em cache de rascunho as respostas em andamento.

**FA03 - Salvar rascunho:**

1. O usuário clica em Salvar rascunho.
2. **[RS]** O sistema salva o andamento localmente e no servidor e exibe a mensagem **[MSG02]**.

### Fluxo de Exceção

**FE01 - Perguntas não respondidas:**

1. No passo 5 do Fluxo Principal, ainda existem perguntas sem resposta.
2. **[RS]** O sistema exibe a mensagem **[MSG03]** e destaca as pendências.

### Pós-condição

As respostas foram submetidas ou permaneceram registradas como rascunho.

### Mensagens

- **MSG01:** Respostas salvas com sucesso.
- **MSG02:** Rascunho salvo com sucesso.
- **MSG03:** Existem perguntas não respondidas.

---

## UC16 - Visualizar Questionário em Modo Administrativo

**Ator(es):** Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está autenticado como administrador e acessa um questionário a partir da tela de detalhes do projeto.

### Fluxo Principal

1. **[EV]** O administrador clica em Visualizar questionário.
2. **[RS]** O sistema identifica automaticamente o tipo do projeto e redireciona para o formulário correto do questionário.
3. **[RS]** O sistema carrega o questionário em modo somente leitura, indicando ao usuário que se trata de visualização administrativa.
4. **[EV]** O administrador revisa respostas, observações e anexos existentes.

### Fluxo Alternativo

Não há.

### Fluxo de Exceção

**FE01 - Identificação inválida do projeto ou questionário:**

1. No passo 2 do Fluxo Principal, os parâmetros são inválidos ou o projeto não pode ser carregado.
2. **[RS]** O sistema redireciona para a listagem de projetos.

### Pós-condição

O questionário foi aberto para consulta sem permitir edição administrativa direta das respostas.

### Mensagens

Não há mensagens fixas além dos avisos de contexto da tela.

---

## UC17 - Reagendar Questionário

**Ator(es):** Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está na tela de detalhes do projeto; o questionário está Pendente; já existe ao menos um questionário concluído no projeto; o item selecionado é o primeiro questionário pendente da ordem cronológica.

### Fluxo Principal

1. **[EV]** O usuário clica em Reagendar no questionário desejado.
2. **[RS]** O sistema abre o modal de reagendamento com as datas atuais.
3. **[EV]** O usuário informa a nova data de início e a nova data de término.
4. **[EV]** O usuário confirma a operação. **[FE01]** **[FE02]**
5. **[RS]** O sistema atualiza o período do questionário, recarrega a lista e exibe a mensagem **[MSG01]**.

### Fluxo Alternativo

**FA01 - Início imediato do questionário:**

1. No passo 3 do Fluxo Principal, a nova data de início é hoje ou data passada.
2. **[RS]** O sistema informa que o questionário passará imediatamente a Em andamento.

### Fluxo de Exceção

**FE01 - Ordem de datas inválida:**

1. No passo 4 do Fluxo Principal, a data final é anterior à data inicial.
2. **[RS]** O sistema exibe validação no modal e impede a confirmação.

**FE02 - Erro ao reagendar:**

1. No passo 5 do Fluxo Principal, a API rejeita a operação.
2. **[RS]** O sistema exibe a mensagem **[MSG02]**.

### Pós-condição

O questionário passou a ter novo período de aplicação.

### Mensagens

- **MSG01:** Questionário reagendado com sucesso.
- **MSG02:** Erro ao reagendar o questionário.

---

## UC18 - Notificar Representantes Pendentes

**Ator(es):** Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está na tela de detalhes do projeto e há questionário em andamento com representantes pendentes que possuam e-mail.

### Fluxo Principal

1. **[EV]** O usuário clica em Enviar lembrete para um questionário em andamento.
2. **[RS]** O sistema identifica os representantes pendentes aptos a receber notificação.
3. **[EV]** O usuário confirma o envio do lembrete.
4. **[RS]** O sistema envia as notificações e exibe a mensagem **[MSG01]**.

### Fluxo Alternativo

Não há.

### Fluxo de Exceção

**FE01 - Não há pendentes com e-mail:**

1. No passo 2 do Fluxo Principal, nenhum destinatário elegível é encontrado.
2. **[RS]** O sistema exibe a mensagem **[MSG02]**.

**FE02 - Erro no envio:**

1. No passo 4 do Fluxo Principal, ocorre falha ao acionar o envio.
2. **[RS]** O sistema exibe a mensagem **[MSG03]**.

### Pós-condição

Os representantes pendentes elegíveis foram notificados ou o usuário foi informado da impossibilidade de envio.

### Mensagens

- **MSG01:** Lembrete enviado com sucesso.
- **MSG02:** Não há representantes pendentes com endereço de e-mail disponível para este questionário.
- **MSG03:** Falha ao enviar o lembrete.

---

## UC19 - Encerrar Questionário

**Ator(es):** Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está na tela de detalhes do projeto ou no dashboard do questionário e possui permissão administrativa.

### Fluxo Principal

1. **[EV]** O usuário clica em Encerrar questionário.
2. **[RS]** O sistema exibe mensagem de confirmação.
3. **[EV]** O usuário confirma.
4. **[RS]** O sistema encerra o questionário, calcula o ISEP com as respostas existentes e atualiza o status para Concluído.
5. **[RS]** O sistema exibe a mensagem **[MSG01]**.

### Fluxo Alternativo

Não há.

### Fluxo de Exceção

**FE01 - Erro no encerramento:**

1. No passo 4 do Fluxo Principal, a operação falha.
2. **[RS]** O sistema exibe a mensagem **[MSG02]**.

### Pós-condição

O questionário foi encerrado e disponibilizou seus dashboards e exportações.

### Mensagens

- **MSG01:** Questionário encerrado. O ISEP foi calculado.
- **MSG02:** Erro ao encerrar o questionário. Verifique se ele possui respostas registradas.

---

## UC20 - Visualizar Dashboard do Projeto

**Ator(es):** Administrador de projetos, Analista de qualidade e Gerente de projeto.

**Referência:** Não há.

**Pré-condição:** O usuário está autenticado e o projeto possui dados suficientes para o dashboard consolidado.

### Fluxo Principal

1. **[EV]** Na tela de detalhes do projeto, o usuário clica em Dashboard do projeto.
2. **[RS]** O sistema carrega o painel consolidado com KPIs de ISEP, progresso de questionários, média da equipe, desvio padrão, dimensões de governança, dívida ética e dívida técnica.
3. **[EV]** O usuário pode navegar para respostas consolidadas, exportar CSV ou abrir o dashboard de um questionário específico.
4. **[RS]** O sistema apresenta também os registros de emissão de certificado já existentes para o projeto.

### Fluxo Alternativo

**FA01 - Encerrar projeto a partir do dashboard:**

1. Se o usuário for administrador e o projeto ainda não estiver encerrado, ele pode clicar em Encerrar projeto.
2. **[RS]** O sistema segue o fluxo de encerramento do projeto.

### Fluxo de Exceção

**FE01 - Dashboard indisponível:**

1. No passo 2 do Fluxo Principal, não há dados suficientes ou ocorre erro de carregamento.
2. **[RS]** O sistema exibe mensagem de indisponibilidade do dashboard.

### Pós-condição

O usuário visualizou o desempenho consolidado do projeto.

### Mensagens

Não há mensagens fixas além dos erros de carregamento.

---

## UC21 - Encerrar Projeto

**Ator(es):** Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está no dashboard do projeto e o projeto ainda não possui ISEP consolidado calculado.

### Fluxo Principal

1. **[EV]** O usuário clica em Encerrar projeto.
2. **[RS]** O sistema exibe mensagem de confirmação do encerramento consolidado.
3. **[EV]** O usuário confirma.
4. **[RS]** O sistema calcula o ISEP consolidado do projeto, registra data e usuário do encerramento e atualiza o estado do painel.
5. **[RS]** O sistema exibe uma confirmação com ISEP, faixa e quantidade de questionários considerados.

### Fluxo Alternativo

Não há.

### Fluxo de Exceção

**FE01 - Erro ao encerrar o projeto:**

1. No passo 4 do Fluxo Principal, a operação falha.
2. **[RS]** O sistema exibe a mensagem de erro correspondente.

### Pós-condição

O projeto fica encerrado e apto à emissão ou download do certificado, quando aplicável.

### Mensagens

Não há mensagem fixa além do resumo de sucesso e dos erros de backend.

---

## UC22 - Visualizar Dashboard do Questionário

**Ator(es):** Administrador de projetos, Analista de qualidade e Gerente de projeto.

**Referência:** Não há.

**Pré-condição:** O questionário está encerrado e possui ISEP calculado.

### Fluxo Principal

1. **[EV]** O usuário abre o dashboard do questionário a partir do projeto ou de outro dashboard.
2. **[RS]** O sistema exibe KPIs do questionário, distribuição por faixa, dimensões de governança, heatmap por papel e etapa, barra por papel e etapa, nuvem de palavras, indicadores de dívida e widgets de IA.
3. **[EV]** O usuário pode visualizar respostas consolidadas, exportar CSV, exportar JSON e abrir o detalhamento de respostas por membro.
4. **[RS]** O sistema exibe também a área de emissões registradas do boletim de não conformidade.

### Fluxo Alternativo

**FA01 - Forçar encerramento a partir do dashboard:**

1. Se o usuário for administrador e o questionário ainda estiver aberto, ele pode usar a ação de encerramento.
2. **[RS]** O sistema segue o fluxo de encerramento do questionário.

### Fluxo de Exceção

**FE01 - ISEP indisponível:**

1. No passo 2 do Fluxo Principal, o questionário ainda não possui cálculo disponível ou há erro de consulta.
2. **[RS]** O sistema informa que o dashboard ainda não pode ser carregado.

### Pós-condição

O usuário visualizou os indicadores analíticos do questionário.

### Mensagens

Não há mensagens fixas além dos erros de carregamento.

---

## UC23 - Visualizar Dashboard Individual

**Ator(es):** Representante.

**Referência:** Não há.

**Pré-condição:** O usuário está autenticado, é participante do questionário e o dashboard individual está disponível.

### Fluxo Principal

1. **[EV]** Na tela de detalhes do projeto, o usuário clica em Meu painel.
2. **[RS]** O sistema identifica o representante autenticado no contexto do questionário.
3. **[RS]** O sistema exibe indicadores individuais, média da equipe, média histórica e gráfico radar comparativo.

### Fluxo Alternativo

Não há.

### Fluxo de Exceção

**FE01 - Representante não encontrado:**

1. No passo 2 do Fluxo Principal, o usuário autenticado não é localizado entre os participantes do questionário.
2. **[RS]** O sistema exibe mensagem de erro apropriada.

**FE02 - Erro ao carregar o painel:**

1. No passo 3 do Fluxo Principal, ocorre erro de consulta.
2. **[RS]** O sistema informa falha no carregamento do painel individual.

### Pós-condição

O representante visualizou sua própria posição no contexto do questionário.

### Mensagens

Não há mensagens fixas além das mensagens de erro.

---

## UC24 - Visualizar Respostas Consolidadas

**Ator(es):** Administrador de projetos, Analista de qualidade e Gerente de projeto.

**Referência:** Não há.

**Pré-condição:** Existem respostas registradas no projeto ou no questionário selecionado.

### Fluxo Principal

1. **[EV]** O usuário clica em Ver Todas as Respostas no dashboard do projeto ou do questionário.
2. **[RS]** O sistema exibe a grade paginada de respostas consolidadas.
3. **[EV]** O usuário pode filtrar por texto da pergunta, representante, papel, resposta e identificador do questionário.
4. **[EV]** O usuário aplica os filtros e navega pelas páginas.
5. **[EV]** O usuário abre uma resposta específica para ver detalhes completos.
6. **[RS]** O sistema exibe o conteúdo da pergunta, a resposta, a justificativa e os links anexados.

### Fluxo Alternativo

Não há.

### Fluxo de Exceção

**FE01 - Erro ao carregar as respostas:**

1. Nos passos 2 ou 6 do Fluxo Principal, ocorre erro de consulta.
2. **[RS]** O sistema informa a falha de carregamento.

### Pós-condição

O usuário visualizou as respostas individuais de forma consolidada.

### Mensagens

Não há mensagens fixas além das mensagens de erro.

---

## UC25 - Exportar Dados Analíticos

**Ator(es):** Administrador de projetos, Analista de qualidade e Gerente de projeto.

**Referência:** Não há.

**Pré-condição:** O usuário está em um dashboard com dados carregados.

### Fluxo Principal

1. **[EV]** O usuário clica em Relatório do Resultado (CSV) no dashboard do projeto ou do questionário.
2. **[RS]** O sistema gera o arquivo CSV e inicia o download.

### Fluxo Alternativo

**FA01 - Exportar JSON do questionário:**

1. No dashboard do questionário, o usuário clica em Relatório do Resultado (JSON).
2. **[RS]** O sistema gera o arquivo JSON e inicia o download.

### Fluxo de Exceção

**FE01 - Erro ao exportar:**

1. Durante a geração do arquivo, ocorre erro de backend ou processamento.
2. **[RS]** O sistema exibe mensagem de falha na exportação.

### Pós-condição

O arquivo de exportação foi baixado ou o usuário foi informado do erro.

### Mensagens

Não há mensagens fixas além das mensagens de erro de exportação.

---

## UC26 - Utilizar e Gerar Templates de Projeto

**Ator(es):** Administrador de projetos.

**Referência:** Não há.

**Pré-condição:** O usuário está criando um projeto ou configurando partes reutilizáveis da sua estrutura.

### Fluxo Principal

1. **[EV]** Durante a criação de um projeto, o usuário seleciona um template disponível para reaproveitar uma configuração anterior.
2. **[RS]** O sistema carrega o template escolhido e preenche automaticamente os dados compatíveis do formulário.
3. **[EV]** O usuário ajusta as informações conforme necessário.
4. **[EV]** Ao concluir a criação do projeto, o usuário confirma a operação.
5. **[RS]** O sistema cria o projeto e gera automaticamente um novo template derivado da configuração criada.

### Fluxo Alternativo

**FA01 - Reutilizar partes de template:**

1. Durante a configuração de etapas, representantes ou perguntas, o usuário escolhe Selecionar já existente.
2. **[RS]** O sistema exibe um modal de seleção de templates compatíveis com o tipo de projeto.
3. **[EV]** O usuário escolhe o item desejado.
4. **[RS]** O sistema importa a parte selecionada para a configuração atual.

### Fluxo de Exceção

**FE01 - Falha ao carregar templates:**

1. No passo 2 do Fluxo Principal ou no passo 2 do Fluxo Alternativo, o catálogo não pode ser carregado.
2. **[RS]** O sistema informa o erro e mantém o fluxo sem o reaproveitamento.

**FE02 - Falha ao gerar o template derivado:**

1. No passo 5 do Fluxo Principal, o projeto é criado, mas a clonagem para template falha.
2. **[RS]** O sistema exibe a mensagem de erro retornada pela API.

### Pós-condição

O usuário reaproveitou templates existentes e, ao criar um projeto, gerou automaticamente um novo template derivado.

### Mensagens

Não há mensagens fixas além das notificações de sucesso e erro do fluxo de criação.

---

## UC27 - Baixar e Registrar Emissão de Certificado

**Ator(es):** Administrador de projetos, Analista de qualidade e Gerente de projeto.

**Referência:** Não há.

**Pré-condição:** O usuário está no dashboard do projeto e o projeto possui ISEP consolidado calculado.

### Fluxo Principal

1. **[EV]** O usuário clica em Baixar Certificado (PDF).
2. **[RS]** O sistema gera o PDF do certificado e inicia o download. **[FE01]**

### Fluxo Alternativo

**FA01 - Registrar emissão do certificado:**

1. No dashboard do projeto, o usuário clica em Registrar Emissão do Certificado.
2. **[RS]** O sistema exibe uma confirmação informando que a ação criará um registro histórico auditável.
3. **[EV]** O usuário confirma.
4. **[RS]** O sistema registra a emissão, grava código de autenticidade, atualiza a lista de emissões e exibe a mensagem **[MSG01]**.

### Fluxo de Exceção

**FE01 - Erro ao gerar certificado:**

1. No passo 2 do Fluxo Principal, ocorre falha na geração do PDF.
2. **[RS]** O sistema exibe a mensagem correspondente.

**FE02 - Erro ao registrar emissão:**

1. No passo 4 do Fluxo Alternativo, o registro histórico falha.
2. **[RS]** O sistema exibe a mensagem correspondente.

### Pós-condição

O certificado foi baixado e/ou sua emissão foi registrada historicamente.

### Mensagens

- **MSG01:** Emissão registrada. Código: [Código de autenticidade].

---

## UC28 - Baixar, Registrar e Emitir Boletim de Não Conformidade

**Ator(es):** Administrador de projetos, Analista de qualidade e Gerente de projeto.

**Referência:** Não há.

**Pré-condição:** O usuário está no dashboard do questionário e o questionário possui ISEP calculado.

### Fluxo Principal

1. **[EV]** O usuário clica em Baixar Boletim (PDF).
2. **[RS]** O sistema gera o PDF do boletim e inicia o download. **[FE01]**

### Fluxo Alternativo

**FA01 - Emitir boletim aos representantes:**

1. O usuário clica em Emitir aos Representantes.
2. **[RS]** O sistema solicita confirmação do envio por e-mail.
3. **[EV]** O usuário confirma.
4. **[RS]** O sistema envia o boletim aos representantes elegíveis, registra a operação e exibe a mensagem **[MSG01]**.

**FA02 - Registrar emissão do boletim:**

1. O usuário clica em Registrar Emissão do Boletim.
2. **[RS]** O sistema exibe confirmação do registro histórico auditável.
3. **[EV]** O usuário confirma.
4. **[RS]** O sistema grava a emissão, atualiza a lista de emissões e exibe a mensagem **[MSG02]**.

### Fluxo de Exceção

**FE01 - Erro ao gerar o boletim:**

1. No passo 2 do Fluxo Principal, ocorre falha na geração do PDF.
2. **[RS]** O sistema exibe a mensagem correspondente.

**FE02 - Erro ao emitir aos representantes:**

1. No passo 4 do Fluxo Alternativo FA01, a operação falha.
2. **[RS]** O sistema exibe a mensagem correspondente.

**FE03 - Erro ao registrar emissão:**

1. No passo 4 do Fluxo Alternativo FA02, o registro histórico falha.
2. **[RS]** O sistema exibe a mensagem correspondente.

### Pós-condição

O boletim foi baixado, registrado e/ou emitido aos representantes, conforme a ação escolhida.

### Mensagens

- **MSG01:** Boletim emitido para os representantes elegíveis.
- **MSG02:** Emissão registrada. Código: [Código de autenticidade].

---

## UC29 - Configurar Token Pessoal de IA

**Ator(es):** Usuário autenticado.

**Referência:** Não há.

**Pré-condição:** O usuário está autenticado e acessa Configurações > Token de IA.

### Fluxo Principal

1. **[EV]** O usuário acessa a seção Token de IA nas Configurações.
2. **[RS]** O sistema carrega o status atual do token, o provedor configurado e uma dica parcial do valor já salvo, quando existir.
3. **[EV]** O usuário informa um novo token pessoal e escolhe se deseja visualizá-lo temporariamente na tela.
4. **[EV]** O usuário clica em Salvar.
5. **[RS]** O sistema persiste o token, atualiza o status para Configurado e exibe a mensagem **[MSG01]**.

### Fluxo Alternativo

**FA01 - Remover token configurado:**

1. Quando já existe token salvo, o usuário clica em Remover.
2. **[RS]** O sistema solicita confirmação.
3. **[EV]** O usuário confirma.
4. **[RS]** O sistema exclui o token salvo, atualiza o status para Não configurado e exibe a mensagem **[MSG02]**.

### Fluxo de Exceção

**FE01 - Token inválido ou fora do padrão aceito:**

1. No passo 4 do Fluxo Principal, o token não atende às validações mínimas de tamanho.
2. **[RS]** O sistema impede o envio e exibe as mensagens de validação.

**FE02 - Erro ao salvar ou remover:**

1. No passo 5 do Fluxo Principal ou no passo 4 do Fluxo Alternativo, a API falha.
2. **[RS]** O sistema exibe a mensagem de erro correspondente.

### Pós-condição

O token pessoal de IA foi configurado ou removido.

### Mensagens

- **MSG01:** Token salvo com sucesso.
- **MSG02:** Token removido com sucesso.

---

## UC30 - Consultar Insights com Inteligência Artificial

**Ator(es):** Administrador de projetos, Analista de qualidade e Gerente de projeto.

**Referência:** Não há.

**Pré-condição:** O usuário está no dashboard do questionário e os recursos de IA estão disponíveis para consulta.

### Fluxo Principal

1. **[RS]** O dashboard do questionário exibe os widgets de Explicação dos Resultados, Análise de Justificativas, Relatório de Risco e Chat com IA.
2. **[EV]** O usuário aciona um dos widgets analíticos ou envia uma pergunta pelo chat contextual.
3. **[RS]** O sistema envia o contexto do questionário ao serviço de IA e aguarda a resposta.
4. **[RS]** O sistema apresenta o texto analítico gerado na própria interface do widget ou do chat.

### Fluxo Alternativo

**FA01 - Limpar conversa do chat:**

1. No widget de chat, o usuário clica em Limpar conversa.
2. **[RS]** O sistema remove o histórico local da conversa e mantém o contexto do questionário para novas perguntas.

### Fluxo de Exceção

**FE01 - Serviço de IA indisponível:**

1. No passo 3 do Fluxo Principal, o serviço de IA não responde ou retorna erro.
2. **[RS]** O sistema exibe a mensagem de indisponibilidade no widget correspondente.

### Pós-condição

O usuário obteve explicações, análises ou respostas contextuais baseadas nos dados do questionário.

### Mensagens

Não há mensagens fixas além dos erros de indisponibilidade do serviço.

---

## UC31 - Gerenciar Expiração de Sessão

**Ator(es):** Usuário autenticado.

**Referência:** Não há.

**Pré-condição:** O usuário está com uma sessão autenticada prestes a expirar.

### Fluxo Principal

1. **[RS]** O sistema monitora a expiração do token de acesso e agenda um aviso próximo ao vencimento.
2. **[RS]** Ao se aproximar da expiração, o sistema exibe um aviso ao usuário. **[FA01]** **[FA02]**
3. **[EV]** O usuário escolhe a ação disponível no aviso.
4. **[RS]** O sistema executa a ação escolhida.

### Fluxo Alternativo

**FA01 - Estender a sessão:**

1. Quando não há rascunho pendente a ser preservado, o sistema oferece a opção Estender sessão.
2. **[EV]** O usuário escolhe estender a sessão.
3. **[RS]** O sistema utiliza o refresh token, agenda novo vencimento e exibe mensagem de sucesso.

**FA02 - Salvar rascunho antes do logout:**

1. Quando o usuário está respondendo questionário com alterações em andamento, o sistema oferece salvar rascunho antes do encerramento da sessão.
2. **[EV]** O usuário confirma o salvamento.
3. **[RS]** O sistema salva o rascunho, informa o sucesso e encerra a sessão em seguida.

### Fluxo de Exceção

**FE01 - Falha ao estender a sessão:**

1. No fluxo FA01, a renovação com refresh token falha.
2. **[RS]** O sistema informa que não foi possível estender a sessão e agenda o logout.

**FE02 - Falha ao salvar rascunho antes do logout:**

1. No fluxo FA02, ocorre erro ao persistir o rascunho.
2. **[RS]** O sistema alerta que os dados não salvos podem ser perdidos e encerra a sessão.

**FE03 - Expiração consumada sem ação do usuário:**

1. O usuário não responde ao aviso dentro do período de tolerância.
2. **[RS]** O sistema encerra a sessão automaticamente e redireciona para o login.

### Pós-condição

A sessão foi renovada ou encerrada de forma controlada, com tentativa de preservação do trabalho em andamento quando aplicável.

### Mensagens

Não há mensagens fixas além das notificações de sessão exibidas pela interface.
