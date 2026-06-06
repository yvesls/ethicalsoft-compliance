package com.ethicalsoft.ethicalsoft_complience.infra.bootstrap;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.ProjectTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.StageSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TemplateVisibilityEnum;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectTemplateInitializer {

    private static final String CASCATA_TEMPLATE_ID = "693605b7036c4f4afca55d5c";
    private static final String ITERATIVE_TEMPLATE_ID = "69631cccbdd14322645548c8";

    private final ProjectTemplateRepository repository;

    @PostConstruct
    public void seedTemplates() {
        try {
            templatesToSeed().forEach(this::insertIfMissing);
        } catch (Exception e) {
            log.warn("[template-init] Não foi possível inicializar templates de projeto no MongoDB. " +
                    "A aplicação continuará normalmente. Erro: {}", e.getMessage());
        }
    }

    private List<ProjectTemplate> templatesToSeed() {
        return List.of(buildCascataTemplate(), buildIterativeTemplate());
    }

    private void insertIfMissing(ProjectTemplate template) {
        if (template == null || template.getId() == null || template.getId().isBlank()) {
            return;
        }
        if (repository.existsById(template.getId())) {
            return;
        }
        repository.save(template);
    }

    private ProjectTemplate buildCascataTemplate() {
        ProjectTemplate template = new ProjectTemplate();
        template.setId(CASCATA_TEMPLATE_ID);
        template.setName("Template Cascata Padrão (SISP)");
        template.setType(ProjectTypeEnum.CASCATA);
        template.setDescription("Modelo base para projetos em cascata, focado nas 5 etapas do SISP.");
        template.setVisibility(TemplateVisibilityEnum.PUBLIC);
        template.setStages(List.of(
                stage("Iniciação", "2.00", 0),
                stage("Desenvolvimento", "1.00", 0)
        ));

        TemplateQuestionDTO initiQuestion1 = question(
                "O Termo de Abertura do Projeto (TAP) foi aprovado?",
                Set.of(role(2L, "Gerente de Projeto"), role(4L, "Analista de Qualidade")),
                null,
                null
        );
        TemplateQuestionDTO initiQuestion2 = question(
                "Os stakeholders iniciais foram identificados?",
                Set.of(role(2L, "Gerente de Projeto"), role(4L, "Analista de Qualidade")),
                null,
                null
        );

        TemplateQuestionnaireDTO iniciacao = new TemplateQuestionnaireDTO();
        iniciacao.setName("Iniciação");
        iniciacao.setStageName("Iniciação");
        iniciacao.setQuestions(List.of(initiQuestion1, initiQuestion2));

        TemplateQuestionDTO desenvolvimentoRole1 = question(null, Set.of(role(2L, "Gerente de Projeto")), null, null);
        TemplateQuestionDTO desenvolvimentoRole2 = question(null, Set.of(role(4L, "Analista de Qualidade")), null, null);
        TemplateQuestionnaireDTO desenvolvimento = new TemplateQuestionnaireDTO();
        desenvolvimento.setName("Desenvolvimento");
        desenvolvimento.setStageName("Desenvolvimento");
        desenvolvimento.setQuestions(List.of(desenvolvimentoRole1, desenvolvimentoRole2));

        template.setQuestionnaires(List.of(iniciacao, desenvolvimento));
        template.setIterations(List.of());
        template.setRepresentatives(List.of(
                representative(
                        "yves.silva@edu.ufes.br",
                        "Pedro",
                        "Costa",
                        "100.00",
                        Set.of(role(2L, "Gerente de Projeto"), role(4L, "Analista de Qualidade"))
                ),
                representative(
                        "yveslimasilva@gmail.com",
                        "Sampaio",
                        "Jerônimo",
                        "100.00",
                        Set.of(role(3L, "Cliente"), role(1L, "Desenvolvedor"))
                )
        ));
        return template;
    }

    private ProjectTemplate buildIterativeTemplate() {
        ProjectTemplate template = new ProjectTemplate();
        template.setId(ITERATIVE_TEMPLATE_ID);
        template.setName("Meu Template Ágil Pessoal");
        template.setType(ProjectTypeEnum.ITERATIVO);
        template.setDescription("Meu template privado para projetos ágeis com 4 Sprints de 2 semanas.");
        template.setVisibility(TemplateVisibilityEnum.PUBLIC);
        template.setUserId(1L);
        template.setDefaultIterationCount(4);
        template.setDefaultIterationDuration(10);
        template.setStages(List.of(
                stage("Testes", "1.00", 0),
                stage("Requisitos", "4.00", 0),
                stage("Desenvolvimento", "2.00", 0),
                stage("Iniciação", "1.00", 0)
        ));

        TemplateQuestionDTO sprint1Question1 = question(
                "A 'Definition of Ready' foi verificada?",
                Set.of(role(4L, "Analista de Qualidade"), role(2L, "Gerente de Projeto")),
                "Testes",
                List.of(stageSummary(8, "Testes"), stageSummary(7, "Desenvolvimento"))
        );
        TemplateQuestionDTO sprint1Question2 = question(
                "As histórias de usuário estão priorizadas no backlog da Sprint?",
                Set.of(role(1L, "Desenvolvedor"), role(3L, "Cliente")),
                "Requisitos",
                List.of(stageSummary(6, "Requisitos"), stageSummary(5, "Iniciação"))
        );

        TemplateQuestionnaireDTO sprint1 = new TemplateQuestionnaireDTO();
        sprint1.setName("Sprint 1");
        sprint1.setIterationRefName("Sprint 1");
        sprint1.setQuestions(List.of(sprint1Question1, sprint1Question2));

        TemplateQuestionDTO sprint2Question1 = question(
                "A 'Definition of Done' foi alcançada para todas as histórias?",
                Set.of(role(2L, "Gerente de Projeto"), role(4L, "Analista de Qualidade")),
                "Testes",
                List.of(
                        stageSummary(8, "Testes"),
                        stageSummary(6, "Requisitos"),
                        stageSummary(7, "Desenvolvimento"),
                        stageSummary(5, "Iniciação")
                )
        );

        TemplateQuestionnaireDTO sprint2 = new TemplateQuestionnaireDTO();
        sprint2.setName("Sprint 2");
        sprint2.setIterationRefName("Sprint 2");
        sprint2.setQuestions(List.of(sprint2Question1));

        template.setQuestionnaires(List.of(sprint1, sprint2));
        template.setIterations(List.of(
                iteration("Sprint 2", "1.00"),
                iteration("Sprint 1", "1.00")
        ));
        template.setRepresentatives(List.of(
                representative(
                        "parsivaliv@gmail.com",
                        "Pedro",
                        "Costa",
                        "1.00",
                        Set.of(role(2L, "Gerente de Projeto"), role(4L, "Analista de Qualidade"))
                ),
                representative(
                        "yves.silva@edu.ufes.br",
                        "YVES",
                        "SILVA",
                        "2.00",
                        Set.of(role(1L, "Desenvolvedor"), role(3L, "Cliente"))
                )
        ));
        return template;
    }

    private TemplateStageDTO stage(String name, String weight, int sequence) {
        TemplateStageDTO stage = new TemplateStageDTO();
        stage.setName(name);
        stage.setWeight(new BigDecimal(weight));
        stage.setSequence(sequence);
        return stage;
    }

    private TemplateIterationDTO iteration(String name, String weight) {
        TemplateIterationDTO iteration = new TemplateIterationDTO();
        iteration.setName(name);
        iteration.setWeight(new BigDecimal(weight));
        return iteration;
    }

    private TemplateRepresentativeDTO representative(String email,
                                                     String firstName,
                                                     String lastName,
                                                     String weight,
                                                     Set<RoleSummaryResponseDTO> roles) {
        TemplateRepresentativeDTO representative = new TemplateRepresentativeDTO();
        representative.setEmail(email);
        representative.setFirstName(firstName);
        representative.setLastName(lastName);
        representative.setWeight(new BigDecimal(weight));
        representative.setRoles(roles);
        return representative;
    }

    private TemplateQuestionDTO question(String value,
                                         Set<RoleSummaryResponseDTO> roles,
                                         String stageName,
                                         List<StageSummaryResponseDTO> stages) {
        TemplateQuestionDTO question = new TemplateQuestionDTO();
        question.setValue(value);
        question.setRoles(roles);
        question.setStageName(stageName);
        question.setStages(stages);
        return question;
    }

    private RoleSummaryResponseDTO role(Long id, String name) {
        return new RoleSummaryResponseDTO(id, name);
    }

    private StageSummaryResponseDTO stageSummary(int id, String name) {
        return new StageSummaryResponseDTO(id, name);
    }
}
