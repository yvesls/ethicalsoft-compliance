package com.ethicalsoft.ethicalsoft_complience.application.usecase.document;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.PdfDocumentConfigDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.PdfDocumentConfigRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.pdf.DocumentPdfRenderer;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire.GetProjectIsepDashboardUseCase;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.IsepHistoryItemDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.ProjectIsepDashboardDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateComplianceCertificateUseCase {

    private final GetProjectIsepDashboardUseCase getProjectIsepDashboardUseCase;
    private final ProjectRepository projectRepository;
    private final PdfDocumentConfigRepository configRepository;
    private final DocumentPdfRenderer pdfRenderer;

    public record GeneratedCertificate(byte[] content, String certificateCode, String fileName,
                                       String projectName, String isepPercent, String band) {
    }

    public record CertificateMetadata(String certificateCode, String projectName,
                                      java.math.BigDecimal isepValue, String isepPercent, String band) {
    }

    @Transactional(readOnly = true)
    public CertificateMetadata prepareMetadata(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + projectId));

        if (project.getStatus() != ProjectStatusEnum.CONCLUIDO) {
            throw new BusinessException(
                    "O certificado de conformidade só pode ser emitido após o encerramento do projeto.");
        }

        ProjectIsepDashboardDTO dashboard = getProjectIsepDashboardUseCase.execute(projectId);

        if (dashboard.projectBand() == null) {
            throw new BusinessException(
                    "O ISEP consolidado do projeto ainda não foi calculado. Não é possível emitir o certificado.");
        }
        if (!EthicalComplianceBand.meetsMinimum(dashboard.projectBand())) {
            throw new BusinessException(
                    "O projeto não atingiu a faixa mínima aceitável (faixa " + dashboard.projectBand()
                            + "). Em caso de não conformidade, emita o Boletim de Não Conformidade Ética.");
        }

        String certificateCode = DocumentFormatUtil.authenticityCode("ESC",
                project.getId(), dashboard.projectBand(),
                dashboard.projectIsepPercent(), project.getClosingDate());

        return new CertificateMetadata(certificateCode, project.getName(),
                dashboard.projectIsepPercent(),
                DocumentFormatUtil.percent(dashboard.projectIsepPercent()),
                dashboard.projectBand());
    }

    @Transactional(readOnly = true)
    public GeneratedCertificate execute(Long projectId, String issuedBy) {
        log.info("[certificado] Gerando certificado de conformidade projeto={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado: " + projectId));

        if (project.getStatus() != ProjectStatusEnum.CONCLUIDO) {
            throw new BusinessException(
                    "O certificado de conformidade só pode ser emitido após o encerramento do projeto.");
        }

        ProjectIsepDashboardDTO dashboard = getProjectIsepDashboardUseCase.execute(projectId);

        if (dashboard.projectBand() == null) {
            throw new BusinessException(
                    "O ISEP consolidado do projeto ainda não foi calculado. Não é possível emitir o certificado.");
        }
        if (!EthicalComplianceBand.meetsMinimum(dashboard.projectBand())) {
            throw new BusinessException(
                    "O projeto não atingiu a faixa mínima aceitável (faixa " + dashboard.projectBand()
                            + "). Em caso de não conformidade, emita o Boletim de Não Conformidade Ética.");
        }

        PdfDocumentConfigDocument config = configRepository
                .findByKey("CERTIFICADO_CONFORMIDADE")
                .orElse(null);

        Map<String, Object> model = buildModel(project, dashboard, config, issuedBy);

        String templatePath = config != null && config.getTemplateLink() != null
                ? config.getTemplateLink()
                : "documents/certificado-conformidade.ftl";

        byte[] pdf = pdfRenderer.render(templatePath, model);
        String certificateCode = (String) model.get("certificateCode");
        String fileName = "certificado-conformidade-" + projectId + ".pdf";

        return new GeneratedCertificate(pdf, certificateCode, fileName,
                project.getName(),
                DocumentFormatUtil.percent(dashboard.projectIsepPercent()), dashboard.projectBand());
    }

    private Map<String, Object> buildModel(Project project, ProjectIsepDashboardDTO dashboard,
                                           PdfDocumentConfigDocument config, String issuedBy) {

        long approvedQuestionnaires = dashboard.isepHistory().stream()
                .filter(item -> EthicalComplianceBand.meetsMinimum(item.band()))
                .count();

        List<Map<String, Object>> iterations = dashboard.isepHistory().stream()
                .map(this::iterationRow)
                .toList();

        String certificateCode = DocumentFormatUtil.authenticityCode("ESC",
                project.getId(), dashboard.projectBand(),
                dashboard.projectIsepPercent(), project.getClosingDate());

        Map<String, Object> model = new HashMap<>();
        model.put("documentTitle", configValue(config != null ? config.getDocumentTitle() : null,
                "Certificado de Ciência Ética e Conformidade Declarada"));
        model.put("systemName", configValue(config != null ? config.getSystemName() : null,
                "EthicalSoft Compliance"));
        model.put("consolidado", true);

        model.put("projectName", project.getName());
        model.put("projectCode", "PRJ-" + project.getId());
        model.put("projectType", project.getType() != null ? project.getType().name() : "N/D");
        model.put("scopeLabel", "Projeto");
        model.put("periodFormatted", DocumentFormatUtil.period(
                project.getStartDate(), project.getClosingDate()));
        model.put("calculatedAtFormatted", DocumentFormatUtil.date(project.getClosingDate()));

        model.put("isepPercent", DocumentFormatUtil.percent(dashboard.projectIsepPercent()));
        model.put("band", dashboard.projectBand());
        model.put("bandLabel", DocumentFormatUtil.bandLabel(dashboard.projectBand()));
        model.put("minimumBand", EthicalComplianceBand.MINIMUM_ACCEPTABLE.name());
        model.put("minimumBandLabel", EthicalComplianceBand.MINIMUM_ACCEPTABLE.getLabel());
        model.put("teamAveragePercent", DocumentFormatUtil.percent(dashboard.teamSimpleAveragePercent()));
        model.put("teamStandardDeviationPercent",
                DocumentFormatUtil.percent(dashboard.teamStandardDeviationPercent()));

        model.put("ethicsScorePercent", DocumentFormatUtil.percent(dashboard.ethicsScorePercent()));
        model.put("processScorePercent", DocumentFormatUtil.percent(dashboard.processScorePercent()));
        model.put("fairnessScorePercent", DocumentFormatUtil.percent(dashboard.fairnessScorePercent()));
        model.put("esgScorePercent", DocumentFormatUtil.percent(dashboard.esgScorePercent()));

        model.put("totalQuestionnaires", dashboard.totalQuestionnaires());
        model.put("approvedQuestionnaires", approvedQuestionnaires);
        model.put("iterations", iterations);

        model.put("certificateCode", certificateCode);
        if (config != null && config.getValidationUrl() != null) {
            model.put("validationUrl", config.getValidationUrl());
        }
        model.put("issuedAtFormatted", DocumentFormatUtil.dateTime(LocalDateTime.now()));
        model.put("issuedBy", configValue(issuedBy, "Analista de Qualidade"));

        return model;
    }

    private Map<String, Object> iterationRow(IsepHistoryItemDTO item) {
        Map<String, Object> row = new LinkedHashMap<>();
        String name = item.iterationName() != null ? item.iterationName()
                : (item.stageName() != null ? item.stageName() : item.questionnaireName());
        row.put("name", name);
        row.put("isepPercent", DocumentFormatUtil.percent(item.isepPercent()));
        row.put("band", item.band());
        row.put("bandLabel", DocumentFormatUtil.bandLabel(item.band()));
        return row;
    }

    private String configValue(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
