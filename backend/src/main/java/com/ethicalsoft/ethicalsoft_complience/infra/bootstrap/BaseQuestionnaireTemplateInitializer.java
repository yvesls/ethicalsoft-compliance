package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateIterationDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateQuestionDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateQuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateRepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateStageDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.ProjectTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.StageSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionClassificationEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TemplateVisibilityEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaseQuestionnaireTemplateInitializer {

    private static final String STAGE_INICIACAO = "Iniciação";
    private static final String STAGE_REQUISITOS = "Requisitos";
    private static final String STAGE_PROJETO = "Projeto";
    private static final String STAGE_DESENVOLVIMENTO = "Desenvolvimento";
    private static final String STAGE_TESTES = "Testes";

    private static final String SPRINT_1 = "Sprint 1";
    private static final String SPRINT_2 = "Sprint 2";
    private static final String SPRINT_3 = "Sprint 3";
    private static final String SPRINT_4 = "Sprint 4";

    private static final String CASCATABASEID = "680205b7036c4f4afca55001";
    private static final String ITERATIVOBASEID = "680205b7036c4f4afca55002";
    private static final String SELECIARHBASEID = "6a39dfed3e2a434d0040f341";

    private static final List<String> LEGACY_TEMPLATE_IDS = List.of(
            "693605b7036c4f4afca55d5c",
            "69631cccbdd14322645548c8"
    );

    private static final long DESENVOLVEDOR = 1L;
    private static final long GERENTEPROJETO = 2L;
    private static final long CLIENTE = 3L;
    private static final long ANALISTAQUALIDADE = 4L;
    private static final long ANALISTAREQUISITOS = 5L;
    private static final long DESIGNER = 6L;
    private static final long RESPONSAVELNEGOCIO = 7L;
    private static final long SUPORTE = 8L;
    private static final long STAKEHOLDER = 9L;
    private static final long LIDEREQUIPE = 10L;
    private static final long ARQUITETOSOFTWARE = 11L;

    private final ProjectTemplateRepository repository;

    @PostConstruct
    public void seed() {
        try {
            removeLegacyTemplates();
            upsertBaseTemplate(markBaseQuestions(buildCascataBase()));
            upsertBaseTemplate(markBaseQuestions(buildIterativoBase()));
            upsertBaseTemplate(markBaseQuestions(buildSelecIaRhTemplate()));
        } catch (Exception e) {
            log.warn("template-init Não foi possível inicializar templates base no MongoDB. A aplicação continuará normalmente. Erro: {}", e.getMessage());
        }
    }

    private ProjectTemplate markBaseQuestions(ProjectTemplate template) {
        if (template == null || template.getQuestionnaires() == null) return template;
        template.getQuestionnaires().stream()
                .filter(q -> q.getQuestions() != null)
                .flatMap(q -> q.getQuestions().stream())
                .filter(question -> question.getType() != QuestionTypeEnum.CUSTOM)
                .forEach(question -> question.setType(QuestionTypeEnum.BASE));
        return template;
    }

    private void classifyCustomCurrentStage(List<TemplateQuestionDTO> questions) {
        questions.forEach(q -> {
            q.setType(QuestionTypeEnum.CUSTOM);
            q.setClassification(QuestionClassificationEnum.CURRENT_STAGE);
        });
    }

    private void removeLegacyTemplates() {
        for (String legacyTemplateId : LEGACY_TEMPLATE_IDS) {
            String templateId = Objects.requireNonNull(legacyTemplateId);
            if (!repository.existsById(templateId)) continue;
            repository.deleteById(templateId);
            log.info("template-init Template legado removido por ter versão base mais completa. templateId={}", templateId);
        }
    }

    private void upsertBaseTemplate(ProjectTemplate t) {
        if (t == null || t.getId() == null || t.getId().isBlank()) return;
        String templateId = Objects.requireNonNull(t.getId());
        boolean existed = repository.existsById(templateId);
        repository.save(t);
        log.info("template-init Template base '{}' {}.", t.getName(), existed ? "reconciliado" : "inserido");
    }

    private ProjectTemplate buildCascataBase() {
        ProjectTemplate t = new ProjectTemplate();
        t.setId(CASCATABASEID);
        t.setName("Base Cascata");
        t.setType(ProjectTypeEnum.CASCATA);
        t.setDescription("Template completo de maturidade ética para projetos em cascata. Cobre todas as roles e fases.");
        t.setVisibility(TemplateVisibilityEnum.PUBLIC);
        t.setStages(List.of(
                stage(STAGE_INICIACAO, new BigDecimal("2.00"), 0),
                stage(STAGE_REQUISITOS, new BigDecimal("3.00"), 1),
                stage(STAGE_PROJETO, new BigDecimal("2.00"), 2),
                stage(STAGE_DESENVOLVIMENTO, new BigDecimal("2.50"), 3),
                stage(STAGE_TESTES, new BigDecimal("1.50"), 4)
        ));
        t.setQuestionnaires(List.of(
                cascataQuestionnaire(STAGE_INICIACAO, 1, buildCascataIniciacaoQuestions(null)),
                cascataQuestionnaire(STAGE_REQUISITOS, 1, buildCascataRequisitosQuestions(null)),
                cascataQuestionnaire(STAGE_PROJETO, 1, buildCascataProjetoQuestions(null)),
                cascataQuestionnaire(STAGE_DESENVOLVIMENTO, 1, buildCascataDesenvolvimentoQuestions(null)),
                cascataQuestionnaire(STAGE_TESTES, 1, buildCascataTestesQuestions(null))
        ));
        t.setIterations(List.of());
        t.setRepresentatives(buildBaseRepresentatives());
        return t;
    }

    private ProjectTemplate buildIterativoBase() {
        ProjectTemplate t = new ProjectTemplate();
        t.setId(ITERATIVOBASEID);
        t.setName("Base Ágil Iterativo");
        t.setType(ProjectTypeEnum.ITERATIVO);
        t.setDescription("Template completo de maturidade ética para projetos iterativos. Organiza perguntas por projeto inteiro, por iteração e por sprint.");
        t.setVisibility(TemplateVisibilityEnum.PUBLIC);
        t.setDefaultIterationCount(4);
        t.setDefaultIterationDuration(10);

        List<TemplateStageDTO> stages = List.of(
                stage(STAGE_INICIACAO, new BigDecimal("2.00"), 0),
                stage(STAGE_REQUISITOS, new BigDecimal("3.00"), 1),
                stage(STAGE_PROJETO, new BigDecimal("2.00"), 2),
                stage(STAGE_DESENVOLVIMENTO, new BigDecimal("2.50"), 3),
                stage(STAGE_TESTES, new BigDecimal("1.50"), 4)
        );
        t.setStages(stages);

        List<StageSummaryResponseDTO> initiacaoStages = List.of(stageSummary(0, STAGE_INICIACAO));
        List<StageSummaryResponseDTO> requisitosStages = List.of(stageSummary(1, STAGE_REQUISITOS));
        List<StageSummaryResponseDTO> projetoStages = List.of(stageSummary(2, STAGE_PROJETO));
        List<StageSummaryResponseDTO> desenvolvimentoStages = List.of(stageSummary(3, STAGE_DESENVOLVIMENTO));
        List<StageSummaryResponseDTO> testesStages = List.of(stageSummary(4, STAGE_TESTES));
        List<StageSummaryResponseDTO> allStages = List.of(
                stageSummary(0, STAGE_INICIACAO),
                stageSummary(1, STAGE_REQUISITOS),
                stageSummary(2, STAGE_PROJETO),
                stageSummary(3, STAGE_DESENVOLVIMENTO),
                stageSummary(4, STAGE_TESTES)
        );

        List<TemplateQuestionDTO> sprint1 = new ArrayList<>();
        sprint1.addAll(buildIterativeProjectWideQuestions(initiacaoStages, requisitosStages, desenvolvimentoStages));
        sprint1.addAll(buildIterativePerIterationQuestions(allStages));
        sprint1.addAll(buildIterativeSprint1Questions(initiacaoStages, requisitosStages));

        List<TemplateQuestionDTO> sprint2 = new ArrayList<>();
        sprint2.addAll(buildIterativePerIterationQuestions(allStages));
        sprint2.addAll(buildIterativeSprint2Questions(requisitosStages, desenvolvimentoStages, projetoStages));

        List<TemplateQuestionDTO> sprint3 = new ArrayList<>();
        sprint3.addAll(buildIterativePerIterationQuestions(allStages));
        sprint3.addAll(buildIterativeSprint3Questions(desenvolvimentoStages, testesStages, initiacaoStages));

        List<TemplateQuestionDTO> sprint4 = new ArrayList<>();
        sprint4.addAll(buildIterativePerIterationQuestions(allStages));
        sprint4.addAll(buildIterativeSprint4Questions(testesStages, desenvolvimentoStages, initiacaoStages));

        t.setQuestionnaires(List.of(
                iterativeQuestionnaire(SPRINT_1, 1, SPRINT_1, sprint1),
                iterativeQuestionnaire(SPRINT_2, 1, SPRINT_2, sprint2),
                iterativeQuestionnaire(SPRINT_3, 1, SPRINT_3, sprint3),
                iterativeQuestionnaire(SPRINT_4, 1, SPRINT_4, sprint4)
        ));

        t.setIterations(List.of(
                iteration(SPRINT_1, 1.00),
                iteration(SPRINT_2, 1.00),
                iteration(SPRINT_3, 1.50),
                iteration(SPRINT_4, 2.00)
        ));
        t.setRepresentatives(buildBaseRepresentatives());
        return t;
    }

    private ProjectTemplate buildSelecIaRhTemplate() {
        ProjectTemplate t = new ProjectTemplate();
        t.setId(SELECIARHBASEID);
        t.setName("SelecIA RH");
        t.setType(ProjectTypeEnum.ITERATIVO);
        t.setDescription("Template iterativo para seleção com apoio de IA.");
        t.setVisibility(TemplateVisibilityEnum.PUBLIC);
        t.setDefaultIterationCount(4);
        t.setDefaultIterationDuration(10);

        List<TemplateStageDTO> stages = List.of(
                stage(STAGE_REQUISITOS, new BigDecimal("1.00"), 1),
                stage(STAGE_DESENVOLVIMENTO, new BigDecimal("1.00"), 2),
                stage(STAGE_PROJETO, new BigDecimal("1.00"), 3),
                stage(STAGE_TESTES, new BigDecimal("1.00"), 4),
                stage(STAGE_INICIACAO, new BigDecimal("1.00"), 5)
        );
        t.setStages(stages);

        List<StageSummaryResponseDTO> reqStages = List.of(stageSummary(16, STAGE_REQUISITOS));
        List<StageSummaryResponseDTO> devStages = List.of(stageSummary(17, STAGE_DESENVOLVIMENTO));
        List<StageSummaryResponseDTO> projStages = List.of(stageSummary(18, STAGE_PROJETO));
        List<StageSummaryResponseDTO> testStages = List.of(stageSummary(19, STAGE_TESTES));
        List<StageSummaryResponseDTO> inicStages = List.of(stageSummary(20, STAGE_INICIACAO));

        List<TemplateQuestionDTO> sprint1Governanca = new ArrayList<>();
        sprint1Governanca.add(question("Existe definição explícita de que toda recomendação da IA nesta sprint será apenas sugestão e nunca decisão automática final?", Set.of(2L, 3L, 9L), STAGE_INICIACAO, inicStages));
        sprint1Governanca.add(question("Foram definidas diretrizes sobre quais dados, credenciais ou informações internas não podem ser enviados para ferramentas externas de IA?", Set.of(2L, 1L, 9L), STAGE_INICIACAO, inicStages));
        classify(sprint1Governanca, QuestionClassificationEnum.WHOLE_PROJECT);

        List<TemplateQuestionDTO> sprint1Dominio = new ArrayList<>();
        sprint1Dominio.add(question("Os requisitos de triagem contemplam conformidade com LGPD e minimização de dados pessoais no processo seletivo?", Set.of(3L, 2L, 9L), STAGE_REQUISITOS, reqStages));
        sprint1Dominio.add(question("Foram identificados critérios de triagem que podem impactar negativamente grupos vulneráveis ou minorias?", Set.of(9L, 3L, 4L), STAGE_REQUISITOS, reqStages));
        sprint1Dominio.add(question("Foi aprovado o uso de idade, faculdade de origem ou tempo de experiência como atalho de ranqueamento inicial sem validação ética formal?", Set.of(3L, 9L, 1L), STAGE_REQUISITOS, reqStages));
        sprint1Dominio.add(question("Os dados utilizados no desenvolvimento respeitam as políticas de privacidade e consentimento?", Set.of(2L, 1L), STAGE_DESENVOLVIMENTO, devStages));
        classifyCustomCurrentStage(sprint1Dominio);

        List<TemplateQuestionDTO> sprint1Qs = new ArrayList<>();
        sprint1Qs.addAll(sprint1Governanca);
        sprint1Qs.addAll(sprint1Dominio);

        List<TemplateQuestionDTO> sprint2Qs = new ArrayList<>(List.of(
                question("Há rastreabilidade entre critérios de ranking, artefatos gerados com apoio de IA e a decisão final aprovada pela equipe?", Set.of(1L, 2L, 4L), STAGE_REQUISITOS, reqStages),
                question("As sugestões de IA usadas para refinar critérios de ranqueamento foram avaliadas quanto a vieses, ambiguidades e impactos éticos antes de entrar no escopo?", Set.of(2L, 9L, 4L), STAGE_REQUISITOS, reqStages),
                question("O recrutador consegue discordar do ranking da IA e registrar o motivo da intervenção humana?", Set.of(9L, 3L, 1L), STAGE_DESENVOLVIMENTO, devStages),
                question("A justificativa resumida exibida ao recrutador informa fatores de decisão sem expor atributos sensíveis do candidato?", Set.of(3L, 9L, 1L), STAGE_PROJETO, projStages)
        ));
        classifyCustomCurrentStage(sprint2Qs);

        List<TemplateQuestionDTO> sprint3Qs = new ArrayList<>(List.of(
                question("Código gerado com apoio de IA identificado para revisão humana antes de merge, entrega ou publicação?", Set.of(1L, 2L, 4L), STAGE_DESENVOLVIMENTO, devStages),
                question("As decisões técnicas de trade-off estão documentadas com justificativa ética e impacto de negócio?", Set.of(2L, 1L, 4L), STAGE_DESENVOLVIMENTO, devStages),
                question("O projeto possui condição mínima de governança ética para encerramento e emissão de certificado, ainda que com melhorias futuras mapeadas?", Set.of(2L, 3L, 4L, 9L), STAGE_INICIACAO, inicStages)
        ));
        classifyCustomCurrentStage(sprint3Qs);

        List<TemplateQuestionDTO> sprint4Qs = new ArrayList<>(List.of(
                question("Código, documentação ou cenários produzidos com IA foram submetidos aos mesmos testes e critérios de aceite aplicados ao restante do software?", Set.of(2L, 1L, 4L), STAGE_TESTES, testStages),
                question("Foram realizados testes específicos para detectar vieses nos resultados do software?", Set.of(4L, 1L), STAGE_TESTES, testStages),
                question("Os resultados dos testes éticos são compartilhados com os stakeholders para validação?", Set.of(2L, 4L, 9L), STAGE_TESTES, testStages),
                question("Falhas, alucinações ou sugestões inseguras geradas por IA foram registradas para prevenção em ciclos futuros?", Set.of(2L, 1L, 4L), STAGE_TESTES, testStages)
        ));
        classifyCustomCurrentStage(sprint4Qs);

        t.setQuestionnaires(List.of(
                iterativeQuestionnaire("Política de triagem e uso de dados", 1, SPRINT_1, sprint1Qs),
                iterativeQuestionnaire("Ranqueamento assistido por IA com explicação inicial", 2, SPRINT_2, sprint2Qs),
                iterativeQuestionnaire("Supervisão humana, trilha de auditoria e encerramento ético", 2, SPRINT_3, sprint3Qs),
                iterativeQuestionnaire("Validação ética final", 1, SPRINT_4, sprint4Qs)
        ));

        t.setIterations(List.of(
                iteration(SPRINT_1, 2.00),
                iteration(SPRINT_2, 1.00),
                iteration(SPRINT_3, 2.00),
                iteration(SPRINT_4, 2.00)
        ));
        t.setRepresentatives(List.of(
                representative("parsivalivgmail.com", "Paula", "Ribeiro", 2.00, Set.of(2L)),
                representative("yveslimasilvagmail.com", "Yves", "Silva", 2.00, Set.of(4L)),
                representative("martinsalessandrro643gmail.com", "Alessandro", "Martins", 3.00, Set.of(1L)),
                representative("darlanjanice8gmail.com", "Janice", "Darlan", 1.00, Set.of(9L)),
                representative("yves.silvaedu.ufes.br", "Josias", "Pereira", 2.00, Set.of(3L))
        ));
        return t;
    }

    private List<TemplateQuestionDTO> buildIterativeProjectWideQuestions(List<StageSummaryResponseDTO> initiacaoStages,
                                                                         List<StageSummaryResponseDTO> requisitosStages,
                                                                         List<StageSummaryResponseDTO> desenvolvimentoStages) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(question("Foi aprovado o uso de IA e definido em quais atividades ela será empregada?", Set.of(GERENTEPROJETO, CLIENTE, STAKEHOLDER), STAGE_INICIACAO, initiacaoStages));
        qs.add(question("Foram definidas diretrizes sobre quais dados, credenciais ou informações internas não podem ser enviados a ferramentas externas de IA?", Set.of(GERENTEPROJETO, DESENVOLVEDOR, LIDEREQUIPE, ARQUITETOSOFTWARE), STAGE_INICIACAO, initiacaoStages));
        qs.add(question("Foi definido quem realiza a revisão humana e a aprovação final de artefatos produzidos com apoio de IA?", Set.of(GERENTEPROJETO, LIDEREQUIPE, ANALISTAQUALIDADE), STAGE_INICIACAO, initiacaoStages));
        qs.add(question("Os valores éticos do projeto estão explicitamente documentados e acessíveis?", Set.of(GERENTEPROJETO, LIDEREQUIPE, CLIENTE), STAGE_INICIACAO, initiacaoStages));
        qs.add(question("Os riscos éticos do projeto foram documentados e comunicados aos envolvidos?", Set.of(GERENTEPROJETO, ANALISTAQUALIDADE, RESPONSAVELNEGOCIO), STAGE_INICIACAO, initiacaoStages));
        qs.add(question("Existe um canal definido para reportar preocupações éticas de forma anônima e segura?", Set.of(GERENTEPROJETO, LIDEREQUIPE, SUPORTE), STAGE_INICIACAO, initiacaoStages));
        qs.add(question("Foi definido quem toma decisões em nome da organização?", Set.of(CLIENTE, RESPONSAVELNEGOCIO), STAGE_INICIACAO, initiacaoStages));
        qs.add(question("Foi definido quem aprova a flexibilidade orçamentária e as anormalidades de escopo?", Set.of(CLIENTE, RESPONSAVELNEGOCIO, GERENTEPROJETO), STAGE_INICIACAO, initiacaoStages));
        qs.add(question("O projeto possui condição mínima de governança ética para avançar?", Set.of(GERENTEPROJETO, CLIENTE, ANALISTAQUALIDADE, STAKEHOLDER), STAGE_INICIACAO, initiacaoStages));
        classify(qs, QuestionClassificationEnum.WHOLE_PROJECT);
        return qs;
    }

    private List<TemplateQuestionDTO> buildIterativePerIterationQuestions(List<StageSummaryResponseDTO> allStages) {
        List<StageSummaryResponseDTO> todas = allStages;
        List<StageSummaryResponseDTO> reqProjDevTest = pick(allStages, STAGE_REQUISITOS, STAGE_PROJETO, STAGE_DESENVOLVIMENTO, STAGE_TESTES);
        List<StageSummaryResponseDTO> reqProjDev = pick(allStages, STAGE_REQUISITOS, STAGE_PROJETO, STAGE_DESENVOLVIMENTO);
        List<StageSummaryResponseDTO> devTest = pick(allStages, STAGE_DESENVOLVIMENTO, STAGE_TESTES);

        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(question("A sprint atual está alinhada aos limites éticos e operacionais definidos para o projeto?", Set.of(GERENTEPROJETO, LIDEREQUIPE, ANALISTAQUALIDADE), STAGE_INICIACAO, todas));
        qs.add(question("A equipe revisou riscos, dependências e impactos antes de iniciar a sprint?", Set.of(GERENTEPROJETO, ANALISTAQUALIDADE, LIDEREQUIPE), STAGE_INICIACAO, todas));
        qs.add(question("Artefatos ou decisões apoiados por IA passaram por revisão humana antes de serem aceitos nesta sprint?", Set.of(GERENTEPROJETO, DESENVOLVEDOR, ANALISTAQUALIDADE), STAGE_REQUISITOS, reqProjDevTest));
        qs.add(question("As decisões tomadas nesta sprint mantêm rastreabilidade com os requisitos éticos do projeto?", Set.of(GERENTEPROJETO, ANALISTAREQUISITOS, ANALISTAQUALIDADE), STAGE_REQUISITOS, reqProjDevTest));
        qs.add(question("Foram registrados aprendizados, falhas ou riscos para orientar a próxima sprint?", Set.of(GERENTEPROJETO, DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, devTest));
        qs.add(question("A equipe confirmou que não há uso indevido de dados, credenciais ou informações sensíveis nesta sprint?", Set.of(GERENTEPROJETO, DESENVOLVEDOR, LIDEREQUIPE, ARQUITETOSOFTWARE), STAGE_REQUISITOS, reqProjDev));
        classify(qs, QuestionClassificationEnum.RECURRING);
        return qs;
    }

    private List<TemplateQuestionDTO> buildIterativeSprint1Questions(List<StageSummaryResponseDTO> initiacaoStages,
                                                                      List<StageSummaryResponseDTO> requisitosStages) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(question("Os requisitos iniciais contemplam conformidade com LGPD e minimização de dados pessoais?", Set.of(CLIENTE, GERENTEPROJETO, STAKEHOLDER), STAGE_REQUISITOS, requisitosStages));
        qs.add(question("Foram identificados critérios de triagem que podem impactar negativamente grupos vulneráveis ou minorias?", Set.of(ANALISTAREQUISITOS, CLIENTE, STAKEHOLDER), STAGE_REQUISITOS, requisitosStages));
        qs.add(question("As sugestões de IA usadas na etapa de requisitos foram avaliadas quanto a vieses e ambiguidades antes de entrar no escopo?", Set.of(ANALISTAREQUISITOS, ANALISTAQUALIDADE), STAGE_REQUISITOS, requisitosStages));
        classifyCustomCurrentStage(qs);
        return qs;
    }

    private List<TemplateQuestionDTO> buildIterativeSprint2Questions(List<StageSummaryResponseDTO> requisitosStages,
                                                                      List<StageSummaryResponseDTO> desenvolvimentoStages,
                                                                      List<StageSummaryResponseDTO> projetoStages) {
        List<StageSummaryResponseDTO> reqDev = concat(requisitosStages, desenvolvimentoStages);
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(question("O recrutador consegue discordar do ranking da IA e registrar o motivo da intervenção humana?", Set.of(CLIENTE, STAKEHOLDER, DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, desenvolvimentoStages));
        qs.add(question("Há rastreabilidade entre critérios de ranking, artefatos gerados com apoio de IA e a decisão final aprovada pela equipe?", Set.of(DESENVOLVEDOR, GERENTEPROJETO, ANALISTAQUALIDADE), STAGE_REQUISITOS, reqDev));
        qs.add(question("A justificativa resumida exibida ao recrutador informa fatores de decisão sem expor atributos sensíveis do candidato?", Set.of(CLIENTE, STAKEHOLDER), STAGE_PROJETO, projetoStages));
        classifyCustomCurrentStage(qs);
        return qs;
    }

    private List<TemplateQuestionDTO> buildIterativeSprint3Questions(List<StageSummaryResponseDTO> desenvolvimentoStages,
                                                                      List<StageSummaryResponseDTO> testesStages,
                                                                      List<StageSummaryResponseDTO> initiacaoStages) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(question("Código gerado com apoio de IA é identificado para revisão humana antes de merge, entrega ou publicação?", Set.of(DESENVOLVEDOR, GERENTEPROJETO, ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, desenvolvimentoStages));
        qs.add(question("Decisões técnicas de trade-off estão documentadas com justificativa ética e impacto de negócio?", Set.of(GERENTEPROJETO, DESENVOLVEDOR, ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, desenvolvimentoStages));
        qs.add(question("Os dados utilizados no desenvolvimento respeitam as políticas de privacidade e consentimento?", Set.of(GERENTEPROJETO, DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, desenvolvimentoStages));
        classifyCustomCurrentStage(qs);
        return qs;
    }

    private List<TemplateQuestionDTO> buildIterativeSprint4Questions(List<StageSummaryResponseDTO> testesStages,
                                                                      List<StageSummaryResponseDTO> desenvolvimentoStages,
                                                                      List<StageSummaryResponseDTO> initiacaoStages) {
        List<StageSummaryResponseDTO> inicTest = concat(initiacaoStages, testesStages);
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(question("Código, documentação ou cenários produzidos com IA foram submetidos aos mesmos testes e critérios de aceite aplicados ao restante do software?", Set.of(GERENTEPROJETO, DESENVOLVEDOR), STAGE_TESTES, testesStages));
        qs.add(question("Foram realizados testes específicos para detectar vieses nos resultados do software?", Set.of(ANALISTAQUALIDADE, DESENVOLVEDOR), STAGE_TESTES, testesStages));
        qs.add(question("Os resultados dos testes éticos são compartilhados com os stakeholders para validação?", Set.of(GERENTEPROJETO, ANALISTAQUALIDADE, STAKEHOLDER), STAGE_TESTES, testesStages));
        qs.add(question("Falhas, alucinações ou sugestões inseguras geradas por IA foram registradas para prevenção em ciclos futuros?", Set.of(GERENTEPROJETO, DESENVOLVEDOR, ANALISTAQUALIDADE), STAGE_TESTES, testesStages));
        qs.add(question("O projeto possui condição mínima de encerramento ético, ainda que com melhorias futuras mapeadas?", Set.of(GERENTEPROJETO, CLIENTE, ANALISTAQUALIDADE, STAKEHOLDER), STAGE_INICIACAO, inicTest));
        classifyCustomCurrentStage(qs);
        return qs;
    }

    private void classify(List<TemplateQuestionDTO> questions, QuestionClassificationEnum classification) {
        questions.forEach(q -> {
            q.setType(QuestionTypeEnum.BASE);
            q.setClassification(classification);
        });
    }

    private List<StageSummaryResponseDTO> pick(List<StageSummaryResponseDTO> all, String... names) {
        Set<String> wanted = Set.of(names);
        return all.stream().filter(s -> wanted.contains(s.getName())).toList();
    }

    private List<StageSummaryResponseDTO> concat(List<StageSummaryResponseDTO> a, List<StageSummaryResponseDTO> b) {
        List<StageSummaryResponseDTO> merged = new ArrayList<>(a);
        merged.addAll(b);
        return merged;
    }

    private List<TemplateQuestionDTO> buildCascataIniciacaoQuestions(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> governance = new ArrayList<>();
        governance.add(q("Foi determinado quem toma as decisoes em nome da sua empresa?", Set.of(CLIENTE, RESPONSAVELNEGOCIO), STAGE_INICIACAO, stgs));
        governance.add(q("Foi determinado quem aprova a flexibilidade orcamentaria para o gerenciamento do projeto?", Set.of(CLIENTE, RESPONSAVELNEGOCIO), STAGE_INICIACAO, stgs));
        governance.add(q("Foi definido quem e a autoridade que trata de anormalidades no escopo do trabalho?", Set.of(CLIENTE, GERENTEPROJETO), STAGE_INICIACAO, stgs));
        governance.add(q("Os riscos eticos do projeto foram documentados e comunicados a todos os envolvidos?", Set.of(GERENTEPROJETO, ANALISTAQUALIDADE, RESPONSAVELNEGOCIO), STAGE_INICIACAO, stgs));
        governance.add(q("Existe um canal definido para reportar preocupacoes eticas de forma anonima e segura?", Set.of(GERENTEPROJETO, LIDEREQUIPE, SUPORTE), STAGE_INICIACAO, stgs));
        governance.add(q("Os valores eticos do projeto estao explicitamente documentados e acessiveis a todos?", Set.of(GERENTEPROJETO, LIDEREQUIPE, CLIENTE), STAGE_INICIACAO, stgs));
        governance.add(q("O projeto declarou se utiliza desenvolvimento assistido por IA e em quais atividades esse apoio sera empregado?", Set.of(GERENTEPROJETO, RESPONSAVELNEGOCIO, CLIENTE), STAGE_INICIACAO, stgs));
        governance.add(q("Foram definidas diretrizes sobre quais dados, credenciais ou informacoes internas nao podem ser enviados para ferramentas externas de IA?", Set.of(GERENTEPROJETO, LIDEREQUIPE, ARQUITETOSOFTWARE, DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        governance.add(q("Foi definido quem realiza a revisao humana e a aprovacao final de artefatos produzidos com apoio de IA antes do uso no projeto?", Set.of(GERENTEPROJETO, LIDEREQUIPE, ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        classify(governance, QuestionClassificationEnum.WHOLE_PROJECT);

        List<TemplateQuestionDTO> recurring = new ArrayList<>();
        recurring.add(q("Voce entendeu os objetivos do software?", Set.of(CLIENTE, STAKEHOLDER), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta ciente das politicas eticas que a equipe de desenvolvimento seguira durante todo o projeto?", Set.of(CLIENTE, STAKEHOLDER), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta ciente das possiveis implicacoes eticas do software na seguranca e privacidade dos usuarios?", Set.of(CLIENTE, STAKEHOLDER), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a fornecer feedback para a equipe de desenvolvimento durante todo o processo?", Set.of(CLIENTE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce tem alguma preocupacao com relacao ao desenvolvimento etico do software?", Set.of(CLIENTE, STAKEHOLDER), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta ciente de quaisquer requisitos legais ou regulatorios relacionados ao desenvolvimento do software?", Set.of(CLIENTE, RESPONSAVELNEGOCIO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a trabalhar em colaboracao com a equipe de desenvolvimento para garantir o desenvolvimento etico do software?", Set.of(CLIENTE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce definiu politicas eticas claras que devem ser seguidas durante o desenvolvimento do software?", Set.of(GERENTEPROJETO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce identificou possiveis conflitos eticos que possam surgir durante o desenvolvimento do software?", Set.of(GERENTEPROJETO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce forneceu orientacoes claras para a equipe de desenvolvimento sobre como lidar com possiveis conflitos eticos?", Set.of(GERENTEPROJETO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a apoiar a equipe de desenvolvimento na resolucao de possiveis conflitos eticos?", Set.of(GERENTEPROJETO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a fornecer recursos adicionais para garantir que o software seja desenvolvido de maneira etica?", Set.of(GERENTEPROJETO, RESPONSAVELNEGOCIO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta comprometido em garantir que o software seja desenvolvido de maneira etica?", Set.of(GERENTEPROJETO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce identificou possiveis implicacoes eticas do software na seguranca e privacidade dos usuarios?", Set.of(GERENTEPROJETO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a trabalhar com a equipe de desenvolvimento para garantir que o software seja seguro e proteja a privacidade dos usuarios?", Set.of(GERENTEPROJETO), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce definiu politicas eticas claras que devem ser seguidas durante o desenvolvimento do software?", Set.of(LIDEREQUIPE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce compartilhou essas politicas eticas com sua equipe de desenvolvimento?", Set.of(LIDEREQUIPE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta garantindo que a equipe esteja ciente dos possiveis conflitos eticos que possam surgir durante o desenvolvimento do software?", Set.of(LIDEREQUIPE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta ciente sobre a necessidade de incentivar sua equipe a levantar questoes eticas e a discutir possiveis solucoes para conflitos eticos?", Set.of(LIDEREQUIPE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a apoiar a equipe de desenvolvimento na resolucao de possiveis conflitos eticos?", Set.of(LIDEREQUIPE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta ciente sobre a necessidade de incentivar sua equipe a levantar questoes eticas e a discutir possiveis solucoes para conflitos eticos?", Set.of(ARQUITETOSOFTWARE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce entendeu as politicas eticas estabelecidas pelo administrador?", Set.of(ARQUITETOSOFTWARE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a apoiar a equipe de desenvolvimento na resolucao de possiveis conflitos eticos relacionados ao design do software?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce compartilhou as politicas eticas com sua equipe de desenvolvimento?", Set.of(ARQUITETOSOFTWARE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta comprometido em seguir as politicas eticas estabelecidas e em desenvolver o software de maneira etica?", Set.of(ARQUITETOSOFTWARE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a reportar possiveis problemas eticos identificados durante o desenvolvimento do software?", Set.of(ARQUITETOSOFTWARE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta trabalhando com a equipe de desenvolvimento para garantir que as questoes de usabilidade e acessibilidade sejam consideradas durante o design do software?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce entendeu as politicas eticas estabelecidas pelo administrador?", Set.of(DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce entendeu os objetivos do software?", Set.of(DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        recurring.add(q("Orientacoes claras sobre como lidar com possiveis conflitos eticos foram fornecidas?", Set.of(DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce tem as habilidades tecnicas e o conhecimento adequado para implementar o software de maneira etica?", Set.of(DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        recurring.add(q("O codigo fonte do software esta livre de quaisquer tipos de vies ou discriminacao?", Set.of(DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        recurring.add(q("As possiveis implicacoes eticas do software na seguranca e privacidade dos usuarios foram consideradas?", Set.of(DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta comprometido em seguir as politicas eticas estabelecidas e em desenvolver o software de maneira etica?", Set.of(DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a reportar possiveis problemas eticos identificados durante o desenvolvimento do software?", Set.of(DESENVOLVEDOR), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce entendeu as politicas eticas estabelecidas pelo administrador?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce entendeu os objetivos eticos do software?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("Foram fornecidas orientacoes claras sobre como lidar com possiveis conflitos eticos durante o processo de desenvolvimento?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce tem as habilidades tecnicas e o conhecimento adequado para avaliar se o software esta sendo desenvolvido de maneira etica?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta ciente das possiveis implicacoes eticas do software na seguranca e privacidade dos usuarios?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("O plano de qualidade inclui criterios eticos para avaliar o software?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta disposto a reportar possiveis problemas eticos identificados durante o desenvolvimento do software?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("Voce esta comprometido em garantir que o software seja desenvolvido de maneira etica?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("Existem criterios para aprovacao de iniciacao de produto, que devem ser particularmente exigentes para areas nas quais a organizacao ou desenvolvedor nao e especialista?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        recurring.add(q("Foi determinado quem aprova a flexibilidade orcamentaria para o gerenciamento projeto?", Set.of(ANALISTAQUALIDADE), STAGE_INICIACAO, stgs));
        classify(recurring, QuestionClassificationEnum.RECURRING);

        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.addAll(governance);
        qs.addAll(recurring);
        return qs;
    }

    private List<TemplateQuestionDTO> buildCascataRequisitosQuestions(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(q("Voce esta considerando as implicacoes eticas dos requisitos que esta solicitando?", Set.of(CLIENTE), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta disposto a ajustar os requisitos caso haja preocupacoes eticas levantadas pela equipe de desenvolvimento?", Set.of(CLIENTE), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta garantindo que o software a ser desenvolvido respeite a privacidade dos usuarios finais e seus dados?", Set.of(CLIENTE), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta considerando a inclusao de recursos de acessibilidade no software para atender as necessidades de usuarios com deficiencias?", Set.of(CLIENTE), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta considerando as implicacoes de longo prazo do software para as pessoas e a sociedade como um todo?", Set.of(CLIENTE, STAKEHOLDER), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta garantindo que os requisitos eticos estejam sendo incorporados ao processo de desenvolvimento de software?", Set.of(CLIENTE), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta disposto a trabalhar com a equipe de desenvolvimento para encontrar solucoes que atendam aos requisitos eticos e aos objetivos do projeto?", Set.of(CLIENTE), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta aberto a receber feedback sobre as implicacoes eticas dos requisitos que esta solicitando?", Set.of(CLIENTE), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta assegurando que os requisitos estejam em conformidade com as politicas da organizacao?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("Foi determinado quem define os requisitos?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta considerando a viabilidade e os recursos necessarios para implementar os requisitos solicitados?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta garantindo que os requisitos possam ser medidos e testados para verificacao de conformidade?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta considerando a seguranca cibernetica como parte dos requisitos do software a ser desenvolvido?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta assegurando que as especificacoes de requisitos sejam claras e precisas para evitar mal-entendidos?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta disposto a trabalhar com a equipe de desenvolvimento para encontrar solucoes que atendam aos requisitos e aos objetivos do projeto?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta disposto a reavaliar os requisitos se as preocupacoes eticas ou outras preocupacoes forem levantadas pela equipe de desenvolvimento?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta garantindo que os requisitos sejam atualizados e modificados, se necessario, a medida que o projeto evolui?", Set.of(GERENTEPROJETO), STAGE_REQUISITOS, stgs));
        qs.add(q("O escopo do projeto esta claro e completo?", Set.of(LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos sao claramente definidos e verificaveis?", Set.of(LIDEREQUIPE, ANALISTAREQUISITOS), STAGE_REQUISITOS, stgs));
        qs.add(q("O prazo para a entrega dos requisitos e razoavel e viavel?", Set.of(LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        qs.add(q("O orcamento para a implementacao dos requisitos e adequado e realista?", Set.of(LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos estao alinhados com as necessidades do cliente?", Set.of(LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        qs.add(q("O cliente forneceu feedback sobre os requisitos propostos?", Set.of(LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        qs.add(q("Existem requisitos de seguranca, privacidade e protecao de dados que precisam ser considerados?", Set.of(LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        qs.add(q("O cliente esta disposto a trabalhar em conjunto para a definicao e revisao dos requisitos?", Set.of(LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta garantindo que o software a ser desenvolvido respeite a privacidade dos usuarios finais e seus dados?", Set.of(LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        qs.add(q("A documentacao dos requisitos e completa e precisa?", Set.of(LIDEREQUIPE, ANALISTAREQUISITOS), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos foram entendidos e documentados de maneira clara?", Set.of(ARQUITETOSOFTWARE), STAGE_REQUISITOS, stgs));
        qs.add(q("O design proposto e coerente com os requisitos do cliente?", Set.of(ARQUITETOSOFTWARE), STAGE_REQUISITOS, stgs));
        qs.add(q("O design considera aspectos de usabilidade e experiencia do usuario?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_REQUISITOS, stgs));
        qs.add(q("O design proposto e viavel em termos de implementacao tecnica?", Set.of(ARQUITETOSOFTWARE), STAGE_REQUISITOS, stgs));
        qs.add(q("O design considera aspectos de escalabilidade e manutenibilidade?", Set.of(ARQUITETOSOFTWARE), STAGE_REQUISITOS, stgs));
        qs.add(q("As expectativas do cliente em relacao ao design foram claramente comunicadas e estao sendo atendidas?", Set.of(ARQUITETOSOFTWARE), STAGE_REQUISITOS, stgs));
        qs.add(q("O designer esta disposto a trabalhar em conjunto com outras partes interessadas para a definicao e revisao dos requisitos?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta considerando a inclusao de recursos de acessibilidade no software para atender as necessidades de usuarios com deficiencias?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos sao claros e compreensiveis?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("A documentacao dos requisitos e completa e precisa?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos sao viaveis tecnicamente?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos atendem as necessidades do usuario?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("Existe um processo de revisao e aprovacao dos requisitos?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("Ha um processo formal de mudanca de requisitos em vigor?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("O prazo para a conclusao dos requisitos e realista?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos estao alinhados com as expectativas do cliente?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("Voce esta garantindo que o software a ser desenvolvido respeite a privacidade dos usuarios finais e seus dados?", Set.of(DESENVOLVEDOR), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos do projeto sao claros e precisos?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos estao documentados?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos sao verificaveis e mensuraveis?", Set.of(ANALISTAQUALIDADE, ANALISTAREQUISITOS), STAGE_REQUISITOS, stgs));
        qs.add(q("As necessidades dos usuarios foram levantadas e consideradas nos requisitos?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos estao alinhados com as expectativas do cliente?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos sao factiveis dentro do cronograma e do orcamento estabelecidos?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos foram validados com todas as partes interessadas?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Existem mecanismos para determinar o caminho a seguir quando se descobre que um ou mais requisitos funcionais nao podem ser alcancados?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Existem mecanismos para determinar o caminho da acao quando o cliente insiste?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Existe um procedimento para definir o curso de acao se, durante as fases de planejamento dos requisitos, for descoberto que o atendimento a demanda do cliente levara a consequencias negativas e possivelmente resultados contraditorios e ilegais?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("O processo de requisitos esta de acordo com as politicas e normas da organizacao?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Existe um mecanismo para priorizar os requisitos?", Set.of(ANALISTAQUALIDADE, ANALISTAREQUISITOS), STAGE_REQUISITOS, stgs));
        qs.add(q("O prazo para a conclusao dos requisitos e realista?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("O cliente forneceu feedback sobre os requisitos propostos?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos estao alinhados com as necessidades do cliente?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("O cliente esta disposto a trabalhar em conjunto para a definicao e revisao dos requisitos?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("O orcamento para a implementacao dos requisitos e adequado e realista?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("A documentacao dos requisitos e completa e precisa?", Set.of(ANALISTAQUALIDADE), STAGE_REQUISITOS, stgs));
        qs.add(q("Foram identificados requisitos que possam impactar negativamente grupos vulneraveis ou minorias?", Set.of(ANALISTAREQUISITOS, CLIENTE, STAKEHOLDER), STAGE_REQUISITOS, stgs));
        qs.add(q("Os requisitos contemplam conformidade com legislacao de protecao de dados (ex: LGPD, GDPR)?", Set.of(GERENTEPROJETO, ANALISTAREQUISITOS, RESPONSAVELNEGOCIO), STAGE_REQUISITOS, stgs));
        qs.add(q("Requisitos, historias ou criterios de aceite sugeridos por IA sao identificados e revisados por um responsavel humano antes da aprovacao?", Set.of(ANALISTAREQUISITOS, GERENTEPROJETO, CLIENTE), STAGE_REQUISITOS, stgs));
        qs.add(q("As sugestoes de IA usadas na etapa de requisitos foram avaliadas quanto a vieses, ambiguidades e impactos eticos antes de entrar no escopo?", Set.of(ANALISTAREQUISITOS, ANALISTAQUALIDADE, STAKEHOLDER), STAGE_REQUISITOS, stgs));
        qs.add(q("Ha rastreabilidade entre o artefato de requisitos produzido com apoio de IA e a decisao final aprovada pela equipe?", Set.of(ANALISTAREQUISITOS, GERENTEPROJETO, LIDEREQUIPE), STAGE_REQUISITOS, stgs));
        classify(qs, QuestionClassificationEnum.RECURRING);
        return qs;
    }

    private List<TemplateQuestionDTO> buildCascataProjetoQuestions(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(q("Os requisitos do software foram claramente entendidos?", Set.of(CLIENTE), STAGE_PROJETO, stgs));
        qs.add(q("Os requisitos estao claramente descritos na documentacao?", Set.of(CLIENTE), STAGE_PROJETO, stgs));
        qs.add(q("Foram definidos prazos realistas para o projeto?", Set.of(CLIENTE), STAGE_PROJETO, stgs));
        qs.add(q("A equipe de desenvolvimento tem conhecimento tecnico suficiente para o projeto?", Set.of(CLIENTE), STAGE_PROJETO, stgs));
        qs.add(q("O projeto esta alinhado com as expectativas do cliente?", Set.of(CLIENTE, STAKEHOLDER), STAGE_PROJETO, stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(CLIENTE), STAGE_PROJETO, stgs));
        qs.add(q("O orcamento do projeto foi definido?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("A equipe de desenvolvimento esta disponivel para o projeto?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("O cronograma do projeto e factivel?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("O escopo do projeto esta claramente definido?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("Os recursos necessarios para o projeto foram identificados?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de todas as metodologias de padroes?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("Alguem que nao seja especialista pode orientar os diagramas?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("Voce acha que deveria usar mais diagramas de projeto?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("Os diagramas sao suficientemente claros?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("Seria facil se orientar nos diagramas de projeto?", Set.of(GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("Os membros da equipe possuem as habilidades necessarias para o projeto?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("A equipe tem conhecimento suficiente da tecnologia a ser utilizada no projeto?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("As tarefas foram distribuidas de forma equilibrada na equipe?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("O plano de comunicacao da equipe foi definido?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("O lider da equipe tem autoridade suficiente para tomar decisoes?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de todas as metodologias de padroes?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("Os diagramas sao suficientemente claros?", Set.of(LIDEREQUIPE), STAGE_PROJETO, stgs));
        qs.add(q("O design proposto atende as expectativas do cliente?", Set.of(ARQUITETOSOFTWARE), STAGE_PROJETO, stgs));
        qs.add(q("O design e viavel do ponto de vista tecnico?", Set.of(ARQUITETOSOFTWARE), STAGE_PROJETO, stgs));
        qs.add(q("O design atende aos requisitos do projeto?", Set.of(ARQUITETOSOFTWARE), STAGE_PROJETO, stgs));
        qs.add(q("O design segue as boas praticas de usabilidade e acessibilidade?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_PROJETO, stgs));
        qs.add(q("O design esta de acordo com as diretrizes de marca da empresa?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_PROJETO, stgs));
        qs.add(q("O design leva em consideracao possiveis problemas eticos, como vieses ou discriminacao?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_PROJETO, stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(ARQUITETOSOFTWARE), STAGE_PROJETO, stgs));
        qs.add(q("O design inclui opcoes de personalizacao para atender a diferentes perfis de usuarios?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_PROJETO, stgs));
        qs.add(q("O design inclui opcoes de feedback para que os usuarios possam fornecer suas opinioes e sugestoes?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_PROJETO, stgs));
        qs.add(q("Os requisitos do projeto sao claros e compreensiveis?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("As tecnologias utilizadas sao apropriadas para o projeto?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("As funcionalidades do software sao desenvolvidas seguindo as especificacoes dos requisitos?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("O codigo produzido segue as boas praticas de programacao?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("O software inclui medidas de seguranca para proteger os dados do usuario?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("O software e projetado para ser escalavel e extensivel, permitindo a adicao de novas funcionalidades no futuro?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("O software e projetado para ser de facil manutencao e solucao de problemas?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("Os diagramas sao suficientemente claros?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("O desenvolvimento inclui testes unitarios e de integracao para garantir a qualidade do codigo?", Set.of(DESENVOLVEDOR), STAGE_PROJETO, stgs));
        qs.add(q("Os requisitos do projeto foram corretamente interpretados e documentados?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("Os testes de software sao realizados de acordo com as especificacoes dos requisitos?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("Os testes sao realizados em diferentes cenarios e condicoes para garantir a robustez do software?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("Os resultados dos testes sao documentados e compartilhados com a equipe para acompanhamento e correcao de problemas?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("O software e testado em diferentes plataformas e dispositivos para garantir sua compatibilidade?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("O software e testado em diferentes cenarios de uso, considerando diferentes perfis de usuario?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("O nivel de detalhamento e suficiente?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("As condicoes sao definidas para determinar o nivel de comprometimento com o desenvolvimento de uma abordagem de reuso?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("Os diagramas sao suficientemente claros?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("As metricas de qualidade, como taxa de defeitos e satisfacao do usuario, sao monitoradas e avaliadas regularmente?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("Sao realizados testes de seguranca para detectar possiveis vulnerabilidades e prevenir ataques ciberneticos?", Set.of(ANALISTAQUALIDADE), STAGE_PROJETO, stgs));
        qs.add(q("As decisoes arquiteturais consideram o impacto etico de longo prazo sobre usuarios e sociedade?", Set.of(ARQUITETOSOFTWARE, GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("Existe rastreabilidade entre decisoes de projeto e requisitos eticos documentados?", Set.of(ANALISTAQUALIDADE, ANALISTAREQUISITOS), STAGE_PROJETO, stgs));
        qs.add(q("Decisoes arquiteturais ou de design sugeridas por IA foram justificadas e revisadas antes da adocao no projeto?", Set.of(ARQUITETOSOFTWARE, DESIGNER, GERENTEPROJETO), STAGE_PROJETO, stgs));
        qs.add(q("O uso de IA em arquitetura ou design respeita restricoes de seguranca, licenciamento e confidencialidade definidas para o projeto?", Set.of(ARQUITETOSOFTWARE, GERENTEPROJETO, RESPONSAVELNEGOCIO), STAGE_PROJETO, stgs));
        classify(qs, QuestionClassificationEnum.RECURRING);
        return qs;
    }

    private List<TemplateQuestionDTO> buildCascataDesenvolvimentoQuestions(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(q("Os requisitos estao sendo atendidos?", Set.of(CLIENTE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O projeto esta dentro do orcamento planejado?", Set.of(CLIENTE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O projeto esta dentro do cronograma planejado?", Set.of(CLIENTE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As funcionalidades estao sendo desenvolvidas de acordo com o que foi especificado?", Set.of(CLIENTE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As expectativas estao sendo atendidas em relacao a qualidade do produto?", Set.of(CLIENTE, STAKEHOLDER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O projeto esta seguindo o cronograma estabelecido?", Set.of(GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As tarefas dos membros da equipe estao sendo bem definidas?", Set.of(GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Os recursos necessarios para o desenvolvimento estao sendo providenciados?", Set.of(GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O projeto esta respeitando as normas e politicas de seguranca da empresa?", Set.of(GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As etapas de testes estao sendo bem documentadas?", Set.of(GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O progresso do projeto esta sendo relatado de forma clara e concisa?", Set.of(GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As alteracoes de escopo estao sendo gerenciadas adequadamente?", Set.of(GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Existem condicoes para integrar os recursos avancados do gerenciamento de codigo? Horarios? Parametros ambientais?", Set.of(GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Existem condicoes para a integracao de desenvolvedores jovens e menos experientes?", Set.of(GERENTEPROJETO, LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("A comunicacao entre os membros da equipe esta sendo eficaz?", Set.of(GERENTEPROJETO, LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O cronograma de desenvolvimento esta sendo cumprido?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("A equipe tem recursos suficientes para desenvolver o projeto dentro do prazo e orcamento?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O progresso do projeto esta sendo monitorado regularmente e comunicado a equipe e partes interessadas?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("A equipe esta seguindo as melhores praticas de desenvolvimento de software?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("A equipe esta aderindo as normas e padroes definidos para o projeto?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As condicoes foram definidas para selecionar o idioma de codificacao?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As mudancas de requisitos sao gerenciadas e controladas adequadamente?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As falhas e defeitos sao registrados e tratados corretamente?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As expectativas dos stakeholders em relacao a qualidade do produto estao sendo atendidas?", Set.of(LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As interfaces de usuario estao sendo desenvolvidas com base nas especificacoes de design?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As telas de interface estao sendo testadas e revisadas conforme o feedback dos usuarios e a conformidade com os padroes de design?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As animacoes e efeitos visuais estao sendo integrados de acordo com as diretrizes de design?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As fontes, cores e icones estao sendo utilizados de acordo com o manual de identidade visual da empresa?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As interfaces estao sendo desenvolvidas para serem responsivas e adaptaveis a diferentes tamanhos de tela?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O design da interface esta sendo desenvolvido de forma a facilitar a usabilidade e a acessibilidade para pessoas com deficiencia?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O design da interface esta sendo desenvolvido de forma a promover a coerencia e a consistencia em todas as telas?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As animacoes e transicoes de interface estao sendo desenvolvidas de forma a proporcionar uma experiencia de usuario agradavel?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Os requisitos estao claramente definidos e documentados?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Os casos de teste foram definidos e documentados para cada requisito?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("A arquitetura do software foi definida e documentada?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Os padroes de codificacao foram definidos e documentados?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Foi definido um processo de revisao de codigo?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O codigo e compativel com os diagramas de projeto?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Os comentarios tem clareza suficiente?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("E facil descrever para outra pessoa o codigo que voce escreveu?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O controle de versao esta sendo utilizado adequadamente?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O ambiente de desenvolvimento esta configurado corretamente?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Foram definidos e documentados criterios de aceitacao para a entrega do software?", Set.of(DESENVOLVEDOR), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O codigo fonte esta sendo versionado adequadamente?", Set.of(ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Os testes unitarios estao sendo executados e cobrindo a maioria das funcionalidades do software?", Set.of(ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As correcoes de bugs estao sendo registradas e tratadas de maneira apropriada?", Set.of(ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Os requisitos de desempenho estao sendo atendidos de acordo com as expectativas?", Set.of(ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As metricas de qualidade do codigo (ex: analise estatica) estao sendo monitoradas e utilizadas para orientar as decisoes do time?", Set.of(ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("O processo de deploy esta sendo automatizado e documentado adequadamente?", Set.of(ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Existe revisao de codigo com foco em identificar vieses algoritmicos ou praticas discriminatorias?", Set.of(DESENVOLVEDOR, ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Os dados utilizados no desenvolvimento respeitam as politicas de privacidade e consentimento?", Set.of(DESENVOLVEDOR, GERENTEPROJETO), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("As decisoes tecnicas de trade-off estao sendo documentadas com justificativa etica?", Set.of(DESENVOLVEDOR, ARQUITETOSOFTWARE, LIDEREQUIPE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Codigo gerado com apoio de IA e identificado para revisao humana antes de merge, entrega ou publicacao?", Set.of(DESENVOLVEDOR, LIDEREQUIPE, ANALISTAQUALIDADE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Sugestoes de IA para codigo foram verificadas quanto a licenciamento, seguranca e aderencia aos padroes tecnicos do projeto?", Set.of(DESENVOLVEDOR, ANALISTAQUALIDADE, ARQUITETOSOFTWARE), STAGE_DESENVOLVIMENTO, stgs));
        qs.add(q("Credenciais, dados pessoais ou informacoes internas foram excluidos de prompts enviados a ferramentas de IA durante o desenvolvimento?", Set.of(DESENVOLVEDOR, GERENTEPROJETO, ARQUITETOSOFTWARE), STAGE_DESENVOLVIMENTO, stgs));
        classify(qs, QuestionClassificationEnum.RECURRING);
        return qs;
    }

    private List<TemplateQuestionDTO> buildCascataTestesQuestions(List<StageSummaryResponseDTO> stgs) {
        List<TemplateQuestionDTO> qs = new ArrayList<>();
        qs.add(q("Os requisitos estabelecidos foram atendidos corretamente?", Set.of(CLIENTE), STAGE_TESTES, stgs));
        qs.add(q("O software apresenta erros ou comportamentos inesperados?", Set.of(CLIENTE), STAGE_TESTES, stgs));
        qs.add(q("As funcionalidades foram testadas de maneira abrangente?", Set.of(CLIENTE), STAGE_TESTES, stgs));
        qs.add(q("A interface do software esta intuitiva e facil de usar?", Set.of(CLIENTE), STAGE_TESTES, stgs));
        qs.add(q("O desempenho do software atende as expectativas estabelecidas?", Set.of(CLIENTE), STAGE_TESTES, stgs));
        qs.add(q("O nivel de erros permitidos foi determinado antes do produto ir para teste de aceitacao?", Set.of(CLIENTE), STAGE_TESTES, stgs));
        qs.add(q("O sistema foi testado em diferentes ambientes?", Set.of(GERENTEPROJETO), STAGE_TESTES, stgs));
        qs.add(q("Foi verificado se o sistema e compativel com diferentes dispositivos?", Set.of(GERENTEPROJETO), STAGE_TESTES, stgs));
        qs.add(q("Foram realizados testes de seguranca no sistema?", Set.of(GERENTEPROJETO), STAGE_TESTES, stgs));
        qs.add(q("O sistema foi testado quanto a sua capacidade de lidar com altas cargas de trafego?", Set.of(GERENTEPROJETO), STAGE_TESTES, stgs));
        qs.add(q("Foram realizados testes de backup e recuperacao de dados?", Set.of(GERENTEPROJETO), STAGE_TESTES, stgs));
        qs.add(q("O nivel de erros permitidos foi determinado antes do produto ir para teste de aceitacao?", Set.of(GERENTEPROJETO), STAGE_TESTES, stgs));
        qs.add(q("Existe um mecanismo que distingue entre um produto de qualidade versus produto correto?", Set.of(GERENTEPROJETO), STAGE_TESTES, stgs));
        qs.add(q("Todos os requisitos foram implementados corretamente?", Set.of(LIDEREQUIPE), STAGE_TESTES, stgs));
        qs.add(q("As funcionalidades estao atendendo as expectativas?", Set.of(LIDEREQUIPE), STAGE_TESTES, stgs));
        qs.add(q("Os resultados dos testes unitarios estao satisfatorios?", Set.of(LIDEREQUIPE), STAGE_TESTES, stgs));
        qs.add(q("Todos os testes foram concluidos com sucesso?", Set.of(LIDEREQUIPE), STAGE_TESTES, stgs));
        qs.add(q("Foram encontrados bugs ou problemas?", Set.of(LIDEREQUIPE), STAGE_TESTES, stgs));
        qs.add(q("O sistema esta respondendo adequadamente as solicitacoes de entrada?", Set.of(LIDEREQUIPE), STAGE_TESTES, stgs));
        qs.add(q("Os usuarios estao conseguindo navegar facilmente na interface?", Set.of(LIDEREQUIPE), STAGE_TESTES, stgs));
        qs.add(q("O sistema esta compativel com diferentes dispositivos e navegadores?", Set.of(LIDEREQUIPE), STAGE_TESTES, stgs));
        qs.add(q("Todos os requisitos de design foram implementados no produto?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_TESTES, stgs));
        qs.add(q("A interface do usuario esta intuitiva e facil de usar?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_TESTES, stgs));
        qs.add(q("O design e consistente em todas as telas e funcionalidades do produto?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_TESTES, stgs));
        qs.add(q("As cores, fontes e elementos visuais estao em conformidade com a identidade visual da empresa?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_TESTES, stgs));
        qs.add(q("Todas as funcionalidades foram testadas para garantir que estejam funcionando corretamente?", Set.of(ARQUITETOSOFTWARE), STAGE_TESTES, stgs));
        qs.add(q("O design esta em conformidade com as melhores praticas de usabilidade e acessibilidade?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_TESTES, stgs));
        qs.add(q("O design esta adequado para todas as resolucoes de tela e dispositivos?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_TESTES, stgs));
        qs.add(q("Todas as informacoes e feedbacks foram considerados e implementados no design do produto?", Set.of(ARQUITETOSOFTWARE, DESIGNER), STAGE_TESTES, stgs));
        qs.add(q("Voce entende as especificacoes do teste?", Set.of(DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Voce implementou todos os requisitos de teste?", Set.of(DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Voce atualizou o codigo-fonte para corrigir erros encontrados nos testes?", Set.of(DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Voce documentou os resultados dos testes de unidade?", Set.of(DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Voce implementou testes de integracao para garantir que o software funcione em conjunto?", Set.of(DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Voce executou testes de estresse para garantir que o software possa lidar com cargas de trabalho pesadas?", Set.of(DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Voce trabalhou com outros desenvolvedores para garantir que todos os modulos do software foram testados?", Set.of(DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Voce realizou testes de regressao para garantir que correcoes e atualizacoes nao afetaram outras areas do software?", Set.of(DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Todas as funcionalidades especificadas nos requisitos foram testadas?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Todas as funcionalidades testadas passaram nos testes unitarios?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Foram realizados testes de integracao entre as diferentes partes do sistema?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("As metricas para determinar o nivel de treinamento exigido do testador estao definidas?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Foram realizados testes de performance do sistema?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Existe um mecanismo que determina se os testes necessarios foram realizados?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Existe um conjunto de testes obrigatorios?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Foram identificados e corrigidos todos os bugs e erros encontrados durante os testes?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Foram realizados testes de seguranca para identificar possiveis vulnerabilidades no sistema?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Todas as funcionalidades do sistema estao em conformidade com as normas e padroes estabelecidos?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("O nivel de erros permitidos foi determinado antes do produto ir para teste de aceitacao?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Existe um mecanismo que distingue entre um produto de qualidade versus produto correto?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Foram realizados testes de usabilidade para avaliar a experiencia do usuario com o sistema?", Set.of(ANALISTAQUALIDADE), STAGE_TESTES, stgs));
        qs.add(q("Foram realizados testes especificos para detectar vieses nos resultados do software?", Set.of(ANALISTAQUALIDADE, DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Os cenarios de teste incluem perfis de usuarios vulneraveis ou com necessidades especiais?", Set.of(ANALISTAQUALIDADE, DESIGNER), STAGE_TESTES, stgs));
        qs.add(q("Os resultados dos testes eticos sao compartilhados com os stakeholders para validacao?", Set.of(ANALISTAQUALIDADE, GERENTEPROJETO, STAKEHOLDER), STAGE_TESTES, stgs));
        qs.add(q("Artefatos de teste gerados com apoio de IA foram revisados e validados antes do uso na verificacao do software?", Set.of(ANALISTAQUALIDADE, DESENVOLVEDOR), STAGE_TESTES, stgs));
        qs.add(q("Codigo, documentacao ou cenarios produzidos com IA foram submetidos aos mesmos testes e criterios de aceite aplicados ao restante do software?", Set.of(ANALISTAQUALIDADE, DESENVOLVEDOR, GERENTEPROJETO), STAGE_TESTES, stgs));
        qs.add(q("Falhas, alucinacoes ou sugestoes inseguras geradas por IA foram registradas para prevencao em ciclos futuros?", Set.of(ANALISTAQUALIDADE, LIDEREQUIPE, SUPORTE), STAGE_TESTES, stgs));
        classify(qs, QuestionClassificationEnum.RECURRING);
        return qs;
    }

    private TemplateQuestionnaireDTO cascataQuestionnaire(String stageName, int weight, List<TemplateQuestionDTO> questions) {
        TemplateQuestionnaireDTO q = new TemplateQuestionnaireDTO();
        q.setName(stageName);
        q.setWeight(weight);
        q.setStageName(stageName);
        q.setQuestions(questions);
        return q;
    }

    private TemplateQuestionnaireDTO iterativeQuestionnaire(String name, int weight, String iterationRefName, List<TemplateQuestionDTO> questions) {
        TemplateQuestionnaireDTO q = new TemplateQuestionnaireDTO();
        q.setName(name);
        q.setWeight(weight);
        q.setIterationRefName(iterationRefName);
        q.setQuestions(questions);
        return q;
    }

    private TemplateStageDTO stage(String name, BigDecimal weight, int sequence) {
        TemplateStageDTO s = new TemplateStageDTO();
        s.setName(name);
        s.setWeight(weight);
        s.setSequence(sequence);
        return s;
    }

    private StageSummaryResponseDTO stageSummary(Integer id, String name) {
        StageSummaryResponseDTO s = new StageSummaryResponseDTO();
        s.setId(id);
        s.setName(name);
        return s;
    }

    private TemplateQuestionDTO question(String value, Set<Long> roleIds, String stageName, List<StageSummaryResponseDTO> stages) {
        TemplateQuestionDTO q = new TemplateQuestionDTO();
        q.setValue(value);
        q.setStageName(stageName);
        q.setStages(stages);
        q.setRoles(roleIds.stream().map(this::role).collect(java.util.stream.Collectors.toSet()));
        return q;
    }

    private RoleSummaryResponseDTO role(Long id) {
        return new RoleSummaryResponseDTO(id, roleName(id));
    }

    private String roleName(long id) {
        return switch ((int) id) {
            case 1 -> "Desenvolvedor";
            case 2 -> "Gerente de Projeto";
            case 3 -> "Cliente";
            case 4 -> "Analista de Qualidade";
            case 5 -> "Analista de Requisitos";
            case 6 -> "Designer";
            case 7 -> "Responsável do Negócio";
            case 8 -> "Suporte";
            case 9 -> "Stakeholder";
            case 10 -> "Líder de Equipe";
            case 11 -> "Arquiteto de Software";
            default -> "Desconhecido";
        };
    }

    private TemplateQuestionDTO q(String value, Set<Long> roleIds, String stageName, List<StageSummaryResponseDTO> stages) {
        return question(value, roleIds, stageName, stages);
    }

    private List<TemplateRepresentativeDTO> buildBaseRepresentatives() {
        return List.of(
                representative("parsivaliv@gmail.com", "Paula", "Ribeiro", 2.00, Set.of(2L)),
                representative("yveslimasilva@gmail.com", "Yves", "Silva", 2.00, Set.of(4L)),
                representative("martinsalessandrro643@gmail.com", "Alessandro", "Martins", 3.00, Set.of(1L)),
                representative("darlanjanice8@gmail.com", "Janice", "Darlan", 1.00, Set.of(9L)),
                representative("yves.silva@edu.ufes.br", "Josias", "Pereira", 2.00, Set.of(3L))
        );
    }

    private TemplateIterationDTO iteration(String name, double weight) {
        TemplateIterationDTO i = new TemplateIterationDTO();
        i.setName(name);
        i.setWeight(BigDecimal.valueOf(weight));
        return i;
    }

    private TemplateRepresentativeDTO representative(String email, String firstName, String lastName,
                                                    double weight, Set<Long> roleIds) {
        TemplateRepresentativeDTO r = new TemplateRepresentativeDTO();
        r.setEmail(email);
        r.setFirstName(firstName);
        r.setLastName(lastName);
        r.setWeight(BigDecimal.valueOf(weight));
        r.setRoles(roleIds.stream().map(this::role).collect(java.util.stream.Collectors.toSet()));
        return r;
    }
}
