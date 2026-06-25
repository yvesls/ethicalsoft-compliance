package com.ethicalsoft.ethicalsoft_complience.strategy;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.application.service.strategy.IterativeIsepStrategy;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.IsepCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IterativeIsepStrategyTest {

    @Mock
    private QuestionMetadataRepository questionMetadataRepository;

    @InjectMocks
    private IterativeIsepStrategy strategy;

    private Project project;
    private Questionnaire questionnaire;
    private Representative representative;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        
        Stage stage = new Stage();
        stage.setId(10);
        stage.setWeight(BigDecimal.ONE);
        project.setStages(Set.of(stage));

        questionnaire = new Questionnaire();
        questionnaire.setId(1);
        questionnaire.setProject(project);

        representative = new Representative();
        representative.setId(100L);
    }

    @Test
    void calculate_shouldCalculateIemBasedOnAnsweredQuestionsOnly() {
        QuestionnaireResponse.AnswerDocument ans1 = new QuestionnaireResponse.AnswerDocument();
        ans1.setQuestionId(1L);
        ans1.setStageIds(List.of(10));
        ans1.setResponse(true);

        QuestionnaireResponse.AnswerDocument ans2 = new QuestionnaireResponse.AnswerDocument();
        ans2.setQuestionId(2L);
        ans2.setStageIds(List.of(10));
        ans2.setResponse(null);

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setRepresentativeId(100L);
        response.setAnswers(List.of(ans1, ans2));

        IsepCalculationResult result = strategy.calculate(
                project, questionnaire, List.of(response), List.of(representative));

        BigDecimal iem = result.memberStageComplianceIndex().get(100L).get(10);
        assertThat(iem).isEqualByComparingTo("1.00");
    }
}
