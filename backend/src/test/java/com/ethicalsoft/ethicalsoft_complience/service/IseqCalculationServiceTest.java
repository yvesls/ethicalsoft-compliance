package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.application.service.IseqCalculationService;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IseqCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class IseqCalculationServiceTest {

    @Mock
    private QuestionMetadataRepository questionMetadataRepository;

    @InjectMocks
    private IseqCalculationService service;

    private Project project;
    private Questionnaire questionnaire;

    @BeforeEach
    void setUp() {
        lenient().when(questionMetadataRepository.findByQuestionIdIn(ArgumentMatchers.anySet()))
                .thenReturn(List.of());

        project = new Project();
        project.setId(1L);

        questionnaire = new Questionnaire();
        questionnaire.setId(1);
        questionnaire.setProject(project);
    }

    private Representative rep(long id, BigDecimal weight) {
        Representative r = new Representative();
        r.setId(id);
        r.setWeight(weight);
        return r;
    }

    private Representative repWithRoles(long id, BigDecimal weight, Long... roleIds) {
        Representative r = rep(id, weight);
        Set<Role> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            Role role = new Role();
            role.setId(roleId);
            roles.add(role);
        }
        r.setRoles(roles);
        return r;
    }

    private QuestionnaireResponse.AnswerDocument answer(long questionId, Boolean response, Integer... stageIds) {
        QuestionnaireResponse.AnswerDocument a = new QuestionnaireResponse.AnswerDocument();
        a.setQuestionId(questionId);
        a.setResponse(response);
        a.setStageIds(stageIds.length == 0 ? null : List.of(stageIds));
        return a;
    }

    private QuestionnaireResponse.AnswerDocument answerWithRoles(long questionId, Boolean response, Long... roleIds) {
        QuestionnaireResponse.AnswerDocument a = answer(questionId, response, 10);
        a.setRoleIds(List.of(roleIds));
        return a;
    }

    private QuestionnaireResponse response(long repId, QuestionnaireResponse.AnswerDocument... answers) {
        QuestionnaireResponse r = new QuestionnaireResponse();
        r.setRepresentativeId(repId);
        r.setAnswers(List.of(answers));
        return r;
    }

    @Test
    @DisplayName("ICP = conformes/elegíveis-respondidas; perguntas não respondidas (null) são ignoradas — 2 SIM, 1 NÃO, 1 em branco => 2/3 = 0,6667")
    void icp_isFlatConformesOverEligibleAnswered_ignoringUnanswered() {
        QuestionnaireResponse resp = response(100L,
                answer(1L, true, 10),
                answer(2L, true, 10),
                answer(3L, false, 20),
                answer(4L, null, 20));

        IseqCalculationResult result = service.calculate(
                project, questionnaire, List.of(resp), List.of(rep(100L, BigDecimal.ONE)));

        assertThat(result.memberComplianceIndex().get(100L))
                .as("ICP do representante 100 deve ser 2 conformes / 3 respondidas")
                .isEqualByComparingTo("0.6667");
    }

    @Test
    @DisplayName("ICP com todas as respostas NÃO é zero REAL (0/N) e penaliza — distinto de 'sem perguntas elegíveis'")
    void icp_allNaoIsRealZeroAndCounts() {
        QuestionnaireResponse resp = response(100L,
                answer(1L, false, 10),
                answer(2L, false, 10),
                answer(3L, false, 20));

        IseqCalculationResult result = service.calculate(
                project, questionnaire, List.of(resp), List.of(rep(100L, BigDecimal.ONE)));

        assertThat(result.memberComplianceIndex())
                .as("representante que respondeu (mesmo que tudo NÃO) deve constar no cálculo")
                .containsKey(100L);
        assertThat(result.memberComplianceIndex().get(100L))
                .as("ICP = 0 conformes / 3 respondidas")
                .isEqualByComparingTo("0.0000");
        assertThat(result.iseq())
                .as("ISEQ com um único representante = o próprio ICP (0)")
                .isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("ISEQ = média ponderada dos ICPs pelo peso do representante — A(1,0; peso 3) e B(0,5; peso 1) => (1,0·3 + 0,5·1)/4 = 0,8750")
    void iseq_isWeightedAverageOfIcpsByRepresentativeWeight() {
        QuestionnaireResponse respA = response(100L,
                answer(1L, true, 10), answer(2L, true, 10));
        QuestionnaireResponse respB = response(200L,
                answer(1L, true, 10), answer(2L, false, 10));

        IseqCalculationResult result = service.calculate(
                project, questionnaire, List.of(respA, respB),
                List.of(rep(100L, BigDecimal.valueOf(3)), rep(200L, BigDecimal.ONE)));

        assertThat(result.memberComplianceIndex().get(100L)).as("ICP A = 2/2").isEqualByComparingTo("1.0000");
        assertThat(result.memberComplianceIndex().get(200L)).as("ICP B = 1/2").isEqualByComparingTo("0.5000");
        assertThat(result.iseq())
                .as("ISEQ ponderado = (1,0·3 + 0,5·1)/4")
                .isEqualByComparingTo("0.8750");
    }

    @Test
    @DisplayName("Elegibilidade: resposta sem interseção de papéis é excluída do ICP — rep tem papel 1; Q2(papel 2) é descartada => ICP = 1/1")
    void icp_excludesAnswersNotEligibleByRoleIntersection() {
        QuestionnaireResponse resp = response(100L,
                answerWithRoles(1L, true, 1L),
                answerWithRoles(2L, false, 2L));

        IseqCalculationResult result = service.calculate(
                project, questionnaire, List.of(resp),
                List.of(repWithRoles(100L, BigDecimal.ONE, 1L)));

        assertThat(result.memberComplianceIndex().get(100L))
                .as("apenas Q1 (papel 1) é elegível => ICP = 1/1")
                .isEqualByComparingTo("1.0000");
    }

    @Test
    @DisplayName("Cobertura (regra 10): representante SEM perguntas elegíveis (0/0) é EXCLUÍDO do ISEQ, sem penalizar a equipe")
    void iseq_excludesRepresentativeWithoutEligibleQuestions() {
        QuestionnaireResponse respA = response(100L, answerWithRoles(1L, true, 1L));
        QuestionnaireResponse respB = response(200L, answerWithRoles(1L, true, 1L));

        IseqCalculationResult result = service.calculate(
                project, questionnaire, List.of(respA, respB),
                List.of(repWithRoles(100L, BigDecimal.ONE, 1L),
                        repWithRoles(200L, BigDecimal.ONE, 9L)));

        assertThat(result.memberComplianceIndex())
                .as("rep B (sem perguntas elegíveis) não deve constar; rep A deve constar")
                .containsKey(100L)
                .doesNotContainKey(200L);
        assertThat(result.iseq())
                .as("ISEQ considera apenas o rep A elegível => 1,0 (rep B não penaliza)")
                .isEqualByComparingTo("1.0000");
    }

    @Test
    @DisplayName("Etapa é dimensão analítica: NÃO pondera o ICP — Q1(etapa10)=SIM, Q2(etapa20)=NÃO => ICP plano = 0,5; breakdown etapa10=1,0 / etapa20=0,0")
    void stageCompliance_isAnalyticalBreakdownNotPartOfIcp() {
        QuestionnaireResponse resp = response(100L,
                answer(1L, true, 10),
                answer(2L, false, 20));

        IseqCalculationResult result = service.calculate(
                project, questionnaire, List.of(resp), List.of(rep(100L, BigDecimal.ONE)));

        assertThat(result.memberComplianceIndex().get(100L))
                .as("ICP plano = 1/2 (não ponderado por etapa)")
                .isEqualByComparingTo("0.5000");
        assertThat(result.memberStageComplianceIndex().get(100L).get(10))
                .as("conformidade analítica da etapa 10 = 1/1")
                .isEqualByComparingTo("1.0000");
        assertThat(result.memberStageComplianceIndex().get(100L).get(20))
                .as("conformidade analítica da etapa 20 = 0/1")
                .isEqualByComparingTo("0.0000");
    }
}
