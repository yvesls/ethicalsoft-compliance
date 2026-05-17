# Casos de Uso (Refatorado) — Visão Geral e Funcionalidades

> Documento refatorado para descrever funcionalidades implementadas de forma genérica, com foco em comportamento e fluxos de usuário, sem exposição de detalhes de implementação técnica.

---

## Escopo
Este documento descreve os principais casos de uso do sistema de compliance ético, cobrindo fluxos de autenticação, gestão de projetos, resposta a questionários, dashboards analíticos e recursos assistidos por IA. As descrições privilegiam a intenção e o comportamento do sistema em vez de nomes de componentes ou endpoints.

---

## UC01 - Cadastro de Usuário

Ator(es): Novo usuário (analista, membro da equipe).

Pré-condição: O usuário não possui conta.

Fluxo principal:
- Usuário acessa a tela de cadastro e preenche os dados obrigatórios (nome, email, senha, confirmação de senha) e aceita os termos quando necessário.
- O sistema valida os dados (formato de email, força de senha, confirmação) e exibe feedback amigável em tempo real.
- Ao confirmar, o sistema cria a conta e informa sucesso, direcionando para a tela de login.

Fluxos alternativos e exceções:
- Email já cadastrado → mensagem clara e orientações.
- Senhas não coincidem / senha fraca → feedback imediato e instruções para correção.
- Termos não aceitos quando exigido → impedimento do registro.

Pós-condição:
- Conta criada; usuário instruído sobre próximo passo (login ou primeiro acesso).

---

## UC02 - Autenticação

Ator(es): Usuário autenticado (analista, gerente, membro da equipe).

Pré-condição: Usuário possui credenciais.

Fluxo principal:
- Usuário entra com email e senha, opcionalmente escolhendo manter sessão ativa.
- Sistema autentica e inicia uma sessão com duração configurável.
- Se necessário, o sistema redireciona para tela de primeira configuração (por exemplo, redefinir senha no primeiro acesso).

Fluxos alternativos e exceções:
- Credenciais inválidas → mensagem genérica de erro.
- Problemas de conexão → opção de tentar novamente.
- Contas bloqueadas/desativadas → orientação para suporte.

Pós-condição:
- Sessão iniciada; tokens/session management tratados de forma segura.

---

## UC03 - Gestão de Projetos

Ator(es): Analista de qualidade, gerente de projeto.

Pré-condição: Usuário autenticado com permissão para gerenciar projetos.

Fluxo principal:
- Usuário acessa listagem de projetos, cria novo projeto escolhendo tipo (ex.: cascata ou iterativo), define informações básicas, componentes do projeto (etapas, responsáveis, questionários) e salva.
- O sistema valida campos obrigatórios e oferece opções de template para pré-popular dados.
- Ao confirmar, o projeto é criado; participantes podem receber convites para participar e acessar o projeto.

Fluxos alternativos:
- Salvar como rascunho para continuar depois.
- Seleção/uso de template para popular estruturas.
- Cancelar criação com confirmação de descarte de mudanças.

Exceções:
- Dados obrigatórios faltando → impedimento até correção.
- Nome de projeto duplicado ou erro no salvamento → mensagem clara.

Pós-condição:
- Projeto registrado no sistema com estado apropriado (aberto/rascunho), pronto para atribuir participantes e questionários.

---

## UC04 - Recuperação e Redefinição de Senha

Ator(es): Usuário que precisa redefinir senha.

Fluxo principal:
- Usuário solicita recuperação de conta informando email.
- Sistema envia instruções (por email) com link ou código temporário para validar identidade.
- Após validação, usuário define nova senha com critérios de segurança.

Exceções:
- Token/código expirado ou inválido → opção para reenviar instruções.
- Limite de tentativas excedido → bloqueio temporário e orientação.

Pós-condição:
- Senha atualizada; usuário pode autenticar normalmente.

---

## UC05 - Responder Questionário

Ator(es): Representante do projeto (membro, especialista).

Pré-condição: Usuário autenticado e autorizado no projeto; questionário disponível.

Fluxo principal:
- Usuário abre o questionário, responde perguntas sequencialmente ou em revisão, anexa evidências quando necessário e salva progresso.
- O sistema oferece salvamento automático (rascunho) e recuperação de rascunhos ao retornar.
- Ao finalizar, o usuário submete as respostas; o sistema valida completude quando exigido e registra as respostas.

Fluxos alternativos:
- Upload de evidências com limite de tamanho e tipos suportados.
- Salvar e continuar depois (rascunho local/servidor).
- Visualização de respostas por perfis com permissão de leitura.

Exceções:
- Sessão expirada durante a resposta → rascunho preservado e usuário orientado a relogar.
- Erro ao enviar respostas → opção de retry ou salvar como rascunho.

Pós-condição:
- Respostas persistidas; indicadores de progresso atualizados; cálculos preliminares de índices de compliance atualizados.

---

## UC06 - Visualizar Dashboard e Relatórios

Ator(es): Analista de qualidade, gerente de projeto.

Pré-condição: Usuário autenticado; dados suficientes para análise.

Fluxo principal:
- Usuário acessa o dashboard do projeto para ver KPIs, distribuição de bandos/compliance, progresso de respostas e tendências.
- É possível navegar para dashboards por questionário, por agente (individual) ou consolidados.
- Ferramentas de exportação permitem gerar relatórios em formatos usuais (PDF, imagens, JSON).

Fluxos alternativos:
- Drill-down em gráficos, filtros por etapa/perfil e visualização fullscreen de widgets.
- Consolidação e comparação entre iterações quando aplicável.

Exceções:
- Ausência de respondentes → mensagem indicando aguardando respostas.
- Erro ao carregar dados → opção de tentar novamente.

Pós-condição:
- Usuário obtém visão consolidada do estado do projeto; ações definidas a partir dos indicadores (notificar respondentes, emitir relatórios, etc.).

---

## UC06A - Assistente de IA: Insights, Relatórios de Risco e Chat Interativo

Ator(es): Analista, gerente de projeto.

Descrição geral:
- Recursos assistidos por IA fornecem sumarizações automáticas, identificação de áreas de risco e um canal de perguntas e respostas em linguagem natural sobre os dados do projeto.
- O assistente pode operar de duas formas: geração sob demanda (resumo/relatório) e interação em tempo real (chat com resposta progressiva).

Fluxos principais:
1. Insights Automáticos
   - Usuário solicita um resumo ou insight sobre um questionário ou o projeto.
   - O sistema processa dados (localmente ou via serviço de IA) e retorna um resumo com achados, tendências e recomendações.

2. Relatório de Risco
   - Usuário solicita análise de risco para um questionário/etapa.
   - O sistema gera um relatório destacando áreas com baixa conformidade e sugerindo ações mitigatórias.

3. Chat Interativo
   - Usuário faz perguntas em linguagem natural (ex.: "Quais são os 3 maiores riscos?").
   - O sistema responde em tempo real, podendo transmitir a resposta progressivamente para melhorar a experiência de leitura.
   - O usuário pode interromper a resposta, reformular ou pedir detalhes complementares.

Fluxos alternativos e considerações:
- Falha na integração com serviço de IA → o sistema deve exibir mensagem amigável e oferecer tentativa posterior.
- Respostas parciais/streaming → permitir cancelamento de solicitação pelo usuário.
- Controle de acesso e contexto: resultados devem considerar o projeto ativo e permissões do usuário.

Privacidade e segurança:
- Dados sensíveis não devem ser enviados a serviços externos sem conformidade e autorização explícita.
- Logs e transcrições devem ser tratados conforme políticas de privacidade.

Pós-condição:
- Insights e relatórios ficam disponíveis para visualização e exportação; interações de chat podem ser registradas conforme política.

---

## UC07 - Emitir Boletim de Não Conformidade

(Descrição geral mantida: geração de boletim quando índices de compliance não atingem limites; inclui geração de documento, armazenamento e envio a stakeholders.)

---

## UC08 - Emitir Certificado de Compliance

(Descrição geral mantida: gerar certificado quando limites de compliance são atingidos; inclui geração de documento, armazenamento, envio e opções de verificação.)

---

## Boas Práticas de Redação e Escopo Técnico
- Evitar referências a nomes de classes, stores ou endpoints no documento de casos de uso; manter foco em comportamentos e regras de negócio.
- Notas técnicas devem ser mantidas em documento separado (guia de implementação) quando necessário.
- Este documento deve ser fonte para redigir critérios de aceite, material de treinamento e comunicações não-técnicas.

---

## Próximos Passos sugeridos
- Revisar o documento com stakeholders para validar termos e limites de aceitação.
- Gerar versão reduzida para o manual do usuário.
- Manter um arquivo técnico separado com mapeamento de endpoints e componentes para desenvolvedores.
