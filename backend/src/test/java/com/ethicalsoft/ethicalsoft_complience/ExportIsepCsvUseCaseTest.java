package com.ethicalsoft.ethicalsoft_complience;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.StageRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.IsepResultQueryPort;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.ExportIsepCsvUseCase;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportIsepCsvUseCaseTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private QuestionnaireRepository questionnaireRepository;
    @Mock
    private RepresentativeRepository representativeRepository;
    @Mock
    private StageRepository stageRepository;
    @Mock
    private IsepResultQueryPort isepResultQueryPort;

    @InjectMocks
    private ExportIsepCsvUseCase useCase;

    private Project project;
    private Questionnaire questionnaire;
    private QuestionnaireResult questionnaireResult;
    private MemberComplianceResult memberResult;
    private Stage stage;
    private Representative representative;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("João");
        user.setLastName("Silva");
        user.setEmail("joao@example.com");

        project = new Project();
        project.setId(1L);
        project.setName("Projeto Alpha");
        project.setType(ProjectTypeEnum.ITERATIVO);

        stage = new Stage();
        stage.setId(1);
        stage.setName("Iniciação");

        questionnaire = new Questionnaire();
        questionnaire.setId(10);
        questionnaire.setName("Sprint 1");
        questionnaire.setIteration("Sprint 1");
        questionnaire.setProject(project);

        memberResult = new MemberComplianceResult();
        memberResult.setRepresentativeId(100L);
        memberResult.setIcp(BigDecimal.valueOf(0.85));
        memberResult.setBand(EthicalComplianceBand.B.name());

        MemberStageComplianceResult stageResult = new MemberStageComplianceResult();
        stageResult.setRepresentativeId(100L);
        stageResult.setStageId(1);
        stageResult.setIem(BigDecimal.valueOf(0.85));

        questionnaireResult = new QuestionnaireResult();
        questionnaireResult.setQuestionnaireId(10);
        questionnaireResult.setProjectId(1L);
        questionnaireResult.setIseq(BigDecimal.valueOf(0.85));
        questionnaireResult.setBand(EthicalComplianceBand.B.name());
        questionnaireResult.setTeamSimpleAverage(BigDecimal.valueOf(0.80));
        questionnaireResult.setTeamStandardDeviation(BigDecimal.valueOf(0.05));
        questionnaireResult.setCalculatedAt(LocalDateTime.now());
        questionnaireResult.getMemberResults().add(memberResult);
        questionnaireResult.getStageResults().add(stageResult);

        representative = new Representative();
        representative.setId(100L);
        representative.setUser(user);
        representative.setProject(project);
    }

    @Test
    void execute_withAnonymize_doesNotIncludeMemberName() {
        when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(questionnaireRepository.findByIdAndProjectId(10, 1L)).thenReturn(java.util.Optional.of(questionnaire));
        when(isepResultQueryPort.findByQuestionnaireId(10)).thenReturn(java.util.Optional.of(questionnaireResult));
        when(stageRepository.findByProjectId(1L)).thenReturn(List.of(stage));
        when(representativeRepository.findByProjectId(1L)).thenReturn(List.of(representative));

        String csv = useCase.execute(1L, 10, true);

        assertNotNull(csv);
        assertTrue(csv.contains("Projeto"), "CSV deve ter cabeçalho com 'Projeto'");
        assertTrue(csv.contains("Projeto Alpha"), "CSV deve ter nome do projeto");
        assertFalse(csv.contains("João Silva"), "Nome do membro não deve aparecer quando anonimizado");
        assertTrue(csv.contains("MBR-"), "ID anonimizado deve ser incluído");
        assertTrue(csv.contains("85,00") || csv.contains("85.00") || csv.contains("85"),
                "ICP deve estar no CSV");
    }

    @Test
    void execute_withoutAnonymize_includesMemberName() {
        when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(questionnaireRepository.findByIdAndProjectId(10, 1L)).thenReturn(java.util.Optional.of(questionnaire));
        when(isepResultQueryPort.findByQuestionnaireId(10)).thenReturn(java.util.Optional.of(questionnaireResult));
        when(stageRepository.findByProjectId(1L)).thenReturn(List.of(stage));
        when(representativeRepository.findByProjectId(1L)).thenReturn(List.of(representative));

        String csv = useCase.execute(1L, 10, false);

        assertNotNull(csv);
        assertTrue(csv.contains("João Silva"), "Nome do membro deve aparecer sem anonimização");
    }

    @Test
    void execute_csvHeaderContainsStageColumns() {
        when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(questionnaireRepository.findByIdAndProjectId(10, 1L)).thenReturn(java.util.Optional.of(questionnaire));
        when(isepResultQueryPort.findByQuestionnaireId(10)).thenReturn(java.util.Optional.of(questionnaireResult));
        when(stageRepository.findByProjectId(1L)).thenReturn(List.of(stage));
        when(representativeRepository.findByProjectId(1L)).thenReturn(List.of(representative));

        String csv = useCase.execute(1L, 10, true);

        String header = csv.split("\n")[0];
        assertTrue(header.contains("ICP (%)"), "Cabeçalho deve incluir ICP");
        assertTrue(header.contains("ISEP Iteração (%)"), "Cabeçalho deve incluir ISEP da Iteração");
        assertTrue(header.contains("Iniciação"), "Cabeçalho deve incluir nome da etapa");
    }

    @Test
    void executeAll_noResults_returnsHeaderOnly() {
        when(projectRepository.findById(1L)).thenReturn(java.util.Optional.of(project));
        when(isepResultQueryPort.findByProjectId(1L)).thenReturn(List.of());

        String csv = useCase.executeAll(1L, true);

        assertNotNull(csv);
        assertTrue(csv.contains("Projeto"), "CSV deve ter cabeçalho mesmo sem dados");
    }
}

