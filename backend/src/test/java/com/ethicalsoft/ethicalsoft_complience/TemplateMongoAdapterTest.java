package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.query.TemplateMongoAdapter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.ProjectTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Iteration;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Stage;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.CreateTemplateRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TemplateVisibilityEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.CurrentUserPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateMongoAdapterTest {

    @Mock
    private ProjectTemplateRepository projectTemplateRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CurrentUserPort currentUserPort;

    @InjectMocks
    private TemplateMongoAdapter adapter;

    private User currentUser;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        currentUser = new User();
        currentUser.setId(99L);
        when(currentUserPort.getCurrentUser()).thenReturn(currentUser);
        when(projectTemplateRepository.save(any(ProjectTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createTemplateFromProject_shouldPreserveWaterfallQuestionnaireWeightAndStageSchedule() {
        Project project = new Project();
        project.setId(10L);
        project.setType(ProjectTypeEnum.CASCATA);

        Stage stage = new Stage();
        stage.setName("Requisitos");
        stage.setWeight(new BigDecimal("3.00"));
        stage.setSequence(1);
        stage.setDurationDays(12);
        stage.setApplicationStartDate(LocalDate.of(2025, Month.JANUARY, 10));
        stage.setApplicationEndDate(LocalDate.of(2025, Month.JANUARY, 21));
        project.setStages(new LinkedHashSet<>(Set.of(stage)));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setName("Questionario de Requisitos");
        questionnaire.setWeight(BigDecimal.valueOf(7));
        questionnaire.setStage(stage);
        questionnaire.setApplicationStartDate(LocalDate.of(2025, Month.JANUARY, 11));
        questionnaire.setApplicationEndDate(LocalDate.of(2025, Month.JANUARY, 18));
        questionnaire.setDomain("PROCESS");
        questionnaire.setDescription("Avalia rastreabilidade e governanca.");

        Question question = new Question();
        question.setValue("Existe rastreabilidade entre requisitos e entregas?");
        question.setStages(new LinkedHashSet<>(Set.of(stage)));
        questionnaire.setQuestions(new LinkedHashSet<>(Set.of(question)));
        project.setQuestionnaires(new LinkedHashSet<>(Set.of(questionnaire)));
        project.setIterations(new LinkedHashSet<>());
        project.setRepresentatives(new LinkedHashSet<>());

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        CreateTemplateRequestDTO request = new CreateTemplateRequestDTO();
        request.setName("Template Cascata");
        request.setDescription("Snapshot do projeto cascata");
        request.setVisibility(TemplateVisibilityEnum.PRIVATE);

        ProjectTemplate saved = Objects.requireNonNull(adapter.createTemplateFromProject(10L, request));

        assertThat(saved.getUserId()).isEqualTo(99L);
        assertThat(saved.getStages()).singleElement().satisfies(savedStage -> {
            assertThat(savedStage.getName()).isEqualTo("Requisitos");
            assertThat(savedStage.getWeight()).isEqualByComparingTo("3.00");
            assertThat(savedStage.getSequence()).isEqualTo(1);
            assertThat(savedStage.getDurationDays()).isEqualTo(12);
            assertThat(savedStage.getApplicationStartDate()).isEqualTo(LocalDate.of(2025, Month.JANUARY, 10));
            assertThat(savedStage.getApplicationEndDate()).isEqualTo(LocalDate.of(2025, Month.JANUARY, 21));
        });
        assertThat(saved.getQuestionnaires()).singleElement().satisfies(savedQuestionnaire -> {
            assertThat(savedQuestionnaire.getName()).isEqualTo("Questionario de Requisitos");
            assertThat(savedQuestionnaire.getWeight()).isEqualByComparingTo(BigDecimal.valueOf(7));
            assertThat(savedQuestionnaire.getStageName()).isEqualTo("Requisitos");
            assertThat(savedQuestionnaire.getApplicationStartDate()).isEqualTo(LocalDate.of(2025, Month.JANUARY, 11));
            assertThat(savedQuestionnaire.getApplicationEndDate()).isEqualTo(LocalDate.of(2025, Month.JANUARY, 18));
            assertThat(savedQuestionnaire.getDomain()).isEqualTo("PROCESS");
            assertThat(savedQuestionnaire.getDescription()).isEqualTo("Avalia rastreabilidade e governanca.");
        });
    }

    @Test
    void createTemplateFromProject_shouldPreserveIterativeIterationScheduleAndQuestionnaireLink() {
        Project project = new Project();
        project.setId(20L);
        project.setType(ProjectTypeEnum.ITERATIVO);
        project.setIterationCount(4);
        project.setIterationDuration(14);
        project.setStages(new LinkedHashSet<>());

        Iteration iteration = new Iteration();
        iteration.setName("Sprint 2");
        iteration.setWeight(new BigDecimal("1.50"));
        iteration.setApplicationStartDate(LocalDate.of(2025, Month.FEBRUARY, 1));
        iteration.setApplicationEndDate(LocalDate.of(2025, Month.FEBRUARY, 14));
        project.setIterations(new LinkedHashSet<>(Set.of(iteration)));

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setName("Questionario Sprint 2");
        questionnaire.setWeight(BigDecimal.valueOf(5));
        questionnaire.setIterationRef(iteration);
        questionnaire.setApplicationStartDate(LocalDate.of(2025, Month.FEBRUARY, 2));
        questionnaire.setApplicationEndDate(LocalDate.of(2025, Month.FEBRUARY, 10));
        questionnaire.setQuestions(new LinkedHashSet<>());
        project.setQuestionnaires(new LinkedHashSet<>(Set.of(questionnaire)));

        Representative representative = new Representative();
        representative.setWeight(new BigDecimal("2.50"));
        representative.setUser(buildUser("ana@empresa.com", "Ana", "Silva"));
        representative.setRoles(new LinkedHashSet<>(Set.of(buildRole(1L, "PO"))));
        project.setRepresentatives(new LinkedHashSet<>(Set.of(representative)));

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));

        CreateTemplateRequestDTO request = new CreateTemplateRequestDTO();
        request.setName("Template Iterativo");
        request.setDescription("Snapshot do projeto iterativo");
        request.setVisibility(TemplateVisibilityEnum.PUBLIC);

        ProjectTemplate saved = Objects.requireNonNull(adapter.createTemplateFromProject(20L, request));

        assertThat(saved.getDefaultIterationCount()).isEqualTo(4);
        assertThat(saved.getDefaultIterationDuration()).isEqualTo(14);
        assertThat(saved.getIterations()).singleElement().satisfies(savedIteration -> {
            assertThat(savedIteration.getName()).isEqualTo("Sprint 2");
            assertThat(savedIteration.getWeight()).isEqualByComparingTo("1.50");
            assertThat(savedIteration.getApplicationStartDate()).isEqualTo(LocalDate.of(2025, Month.FEBRUARY, 1));
            assertThat(savedIteration.getApplicationEndDate()).isEqualTo(LocalDate.of(2025, Month.FEBRUARY, 14));
        });
        assertThat(saved.getQuestionnaires()).singleElement().satisfies(savedQuestionnaire -> {
            assertThat(savedQuestionnaire.getWeight()).isEqualByComparingTo(BigDecimal.valueOf(5));
            assertThat(savedQuestionnaire.getIterationRefName()).isEqualTo("Sprint 2");
        });
        assertThat(saved.getRepresentatives()).singleElement().satisfies(savedRepresentative -> {
            assertThat(savedRepresentative.getEmail()).isEqualTo("ana@empresa.com");
            assertThat(savedRepresentative.getWeight()).isEqualByComparingTo("2.50");
            assertThat(savedRepresentative.getRoles()).extracting("name").containsExactly("PO");
        });
    }

    private User buildUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    private Role buildRole(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }
}