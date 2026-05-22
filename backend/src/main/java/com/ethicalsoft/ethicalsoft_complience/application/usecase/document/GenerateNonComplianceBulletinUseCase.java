package com.ethicalsoft.ethicalsoft_complience.application.usecase.document;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.PdfDocumentConfigDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.PdfDocumentConfigRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionMetadataRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.pdf.DocumentPdfRenderer;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Role;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.GetQuestionnaireDashboardUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.MemberComplianceDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.QuestionnaireIsepDashboardDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateNonComplianceBulletinUseCase {

    private final GetQuestionnaireDashboardUseCase getQuestionnaireDashboardUseCase;
    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final RepresentativeRepository representativeRepository;
    private final QuestionnaireResponseRepository responseRepository;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final PdfDocumentConfigRepository configRepository;
    private final DocumentPdfRenderer pdfRenderer;

    public record GeneratedBulletin(byte[] content, String documentCode, String fileName,
                                    String questionnaireName, String projectName,
                                    String isepPercent, String band) {
    }

    @Transactional(readOnly = true)
    public GeneratedBulletin execute(Long projectId, Integer questionnaireId, String generatedBy) {
        log.info("[boletim] Gerando boletim de não conformidade projeto={} questionário={}",
                projectId, questionnaireId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + projectId));

        Questionnaire questionnaire = questionnaireRepository
                .findByIdAndProjectId(questionnaireId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionário não encontrado: " + questionnaireId));

        QuestionnaireIsepDashboardDTO dashboard =
                getQuestionnaireDashboardUseCase.execute(projectId, questionnaireId);

        if (EthicalComplianceBand.meetsMinimum(dashboard.band())) {
            throw new BusinessException(
                    "O questionário atingiu a faixa mínima aceitável (faixa " + dashboard.band()
                            + "). O boletim de não conformidade não se aplica a resultados conformes.");
        }

        PdfDocumentConfigDocument config = configRepository
                .findByKey("BOLETIM_NAO_CONFORMIDADE")
                .orElse(null);

        Map<String, Object> model = buildModel(project, questionnaire, dashboard, config, generatedBy);

        String templatePath = config != null && config.getTemplateLink() != null
                ? config.getTemplateLink()
                : "documents/boletim-nao-conformidade.ftl";

        byte[] pdf = pdfRenderer.render(templatePath, model);
        String documentCode = (String) model.get("documentCode");
        String fileName = "boletim-nao-conformidade-" + projectId + "-q" + questionnaireId + ".pdf";

        return new GeneratedBulletin(pdf, documentCode, fileName,
                questionnaire.getName(), project.getName(),
                DocumentFormatUtil.percent(dashboard.isepPercent()), dashboard.band());
    }

    private Map<String, Object> buildModel(Project project, Questionnaire questionnaire,
                                           QuestionnaireIsepDashboardDTO dashboard,
                                           PdfDocumentConfigDocument config, String generatedBy) {

        Map<Long, String> roleByRepresentative = representativeRepository.findByProjectId(project.getId())
                .stream()
                .collect(Collectors.toMap(Representative::getId, this::joinRoleNames, (a, b) -> a));

        List<Map<String, Object>> nonCompliantMembers = dashboard.memberResults().stream()
                .filter(m -> !EthicalComplianceBand.meetsMinimum(m.band()))
                .map(m -> memberRow(m, roleByRepresentative))
                .toList();

        List<Map<String, Object>> nonCompliantQuestions =
                loadNonCompliantQuestions(project.getId(), questionnaire.getId());

        String scopeLabel = dashboard.iterationName() != null ? "Iteração"
                : (dashboard.stageName() != null ? "Etapa" : "Questionário");

        String documentCode = DocumentFormatUtil.authenticityCode("BNC",
                project.getId(), questionnaire.getId(), dashboard.band(),
                dashboard.isepPercent(), dashboard.calculatedAt());

        Map<String, Object> model = new HashMap<>();
        model.put("documentTitle", configValue(config != null ? config.getDocumentTitle() : null,
                "Boletim de Não Conformidade Ética"));
        model.put("systemName", configValue(config != null ? config.getSystemName() : null,
                "EthicalSoft Compliance"));
        model.put("documentCode", documentCode);
        model.put("generatedAtFormatted", DocumentFormatUtil.dateTime(LocalDateTime.now()));
        model.put("generatedBy", configValue(generatedBy, "Analista de Qualidade"));

        model.put("projectName", project.getName());
        model.put("projectCode", "PRJ-" + project.getId());
        model.put("projectType", project.getType() != null ? project.getType().name() : "N/D");
        model.put("scopeLabel", scopeLabel);
        model.put("scopeName", questionnaire.getName());
        model.put("periodFormatted", DocumentFormatUtil.period(
                questionnaire.getApplicationStartDate(), questionnaire.getApplicationEndDate()));
        model.put("calculatedAtFormatted", DocumentFormatUtil.dateTime(dashboard.calculatedAt()));

        model.put("isepPercent", DocumentFormatUtil.percent(dashboard.isepPercent()));
        model.put("band", dashboard.band());
        model.put("bandLabel", DocumentFormatUtil.bandLabel(dashboard.band()));
        model.put("minimumBand", EthicalComplianceBand.MINIMUM_ACCEPTABLE.name());
        model.put("minimumBandLabel", EthicalComplianceBand.MINIMUM_ACCEPTABLE.getLabel());
        model.put("teamAveragePercent", DocumentFormatUtil.percent(dashboard.teamSimpleAveragePercent()));
        model.put("teamStandardDeviationPercent",
                DocumentFormatUtil.percent(dashboard.teamStandardDeviationPercent()));

        model.put("nonCompliantMembers", nonCompliantMembers);
        model.put("nonCompliantQuestions", nonCompliantQuestions);

        model.put("ethicsDebtPercent", DocumentFormatUtil.percent(dashboard.ethicsDebtPercent()));
        model.put("techDebtPercent", DocumentFormatUtil.percent(dashboard.techDebtPercent()));
        model.put("ethicsScorePercent", DocumentFormatUtil.percent(dashboard.ethicsScorePercent()));
        model.put("processScorePercent", DocumentFormatUtil.percent(dashboard.processScorePercent()));
        model.put("fairnessScorePercent", DocumentFormatUtil.percent(dashboard.fairnessScorePercent()));
        model.put("esgScorePercent", DocumentFormatUtil.percent(dashboard.esgScorePercent()));

        if (config != null && config.getImpactSummary() != null) {
            model.put("impactSummary", config.getImpactSummary());
        }
        if (config != null && config.getDefaultCorrectiveActions() != null
                && !config.getDefaultCorrectiveActions().isEmpty()) {
            model.put("correctiveActions", config.getDefaultCorrectiveActions());
        }
        model.put("reviewDeadlineFormatted",
                DocumentFormatUtil.date(questionnaire.getApplicationEndDate()));

        return model;
    }

    private Map<String, Object> memberRow(MemberComplianceDTO member, Map<Long, String> roleByRepresentative) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("representativeName", member.representativeName());
        row.put("role", roleByRepresentative.getOrDefault(member.representativeId(), "N/D"));
        row.put("icpPercent", DocumentFormatUtil.percent(member.icpPercent()));
        row.put("band", member.band());
        row.put("bandLabel", DocumentFormatUtil.bandLabel(member.band()));
        return row;
    }

    private String joinRoleNames(Representative representative) {
        if (representative.getRoles() == null || representative.getRoles().isEmpty()) {
            return "N/D";
        }
        return representative.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    private List<Map<String, Object>> loadNonCompliantQuestions(Long projectId, Integer questionnaireId) {
        List<QuestionnaireResponse> responses = responseRepository
                .findByProjectIdAndQuestionnaireIdExcludingTemplates(projectId, questionnaireId)
                .stream()
                .filter(r -> QuestionnaireResponseStatus.COMPLETED.equals(r.getStatus()))
                .toList();

        Set<Long> questionIds = responses.stream()
                .filter(r -> r.getAnswers() != null)
                .flatMap(r -> r.getAnswers().stream())
                .map(QuestionnaireResponse.AnswerDocument::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> domainByQuestion = questionIds.isEmpty()
                ? Collections.emptyMap()
                : questionMetadataRepository.findByQuestionIdIn(questionIds).stream()
                        .filter(m -> m.getDomain() != null)
                        .collect(Collectors.toMap(QuestionMetadataDocument::getQuestionId,
                                m -> m.getDomain().name(), (a, b) -> a));

        Map<Long, long[]> counts = new LinkedHashMap<>();
        Map<Long, String> textByQuestion = new HashMap<>();
        for (QuestionnaireResponse response : responses) {
            if (response.getAnswers() == null) {
                continue;
            }
            for (QuestionnaireResponse.AnswerDocument answer : response.getAnswers()) {
                Long questionId = answer.getQuestionId();
                if (questionId == null || answer.getResponse() == null) {
                    continue;
                }
                counts.computeIfAbsent(questionId, k -> new long[2]);
                if (Boolean.TRUE.equals(answer.getResponse())) {
                    counts.get(questionId)[0]++;
                } else {
                    counts.get(questionId)[1]++;
                }
                textByQuestion.putIfAbsent(questionId, answer.getQuestionText());
            }
        }

        record QuestionRow(String domain, String text, long yes, long no, BigDecimal compliance) {
        }

        BigDecimal threshold = EthicalComplianceBand.MINIMUM_ACCEPTABLE.getMinInclusive();
        List<QuestionRow> rows = new ArrayList<>();
        for (Map.Entry<Long, long[]> entry : counts.entrySet()) {
            long yes = entry.getValue()[0];
            long no = entry.getValue()[1];
            long total = yes + no;
            BigDecimal compliance = total > 0
                    ? BigDecimal.valueOf(yes * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            if (compliance.compareTo(threshold) >= 0) {
                continue;
            }
            rows.add(new QuestionRow(
                    domainByQuestion.getOrDefault(entry.getKey(), "N/D"),
                    textByQuestion.getOrDefault(entry.getKey(), "Questão " + entry.getKey()),
                    yes, no, compliance));
        }

        rows.sort(Comparator.comparing(QuestionRow::compliance));
        return rows.stream()
                .map(r -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("domain", r.domain());
                    row.put("questionText", r.text());
                    row.put("yesCount", r.yes());
                    row.put("noCount", r.no());
                    row.put("compliancePercent", DocumentFormatUtil.percent(r.compliance()));
                    return row;
                })
                .toList();
    }

    private String configValue(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
