package com.ethicalsoft.ethicalsoft_complience.application.usecase.document;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.NotificationTemplateDocument;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.NotificationTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.notification.NotificationEmailSender;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RepresentativeRepository;
import com.ethicalsoft.ethicalsoft_complience.domain.isep.EthicalComplianceBand;
import com.ethicalsoft.ethicalsoft_complience.domain.notification.NotificationType;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmitNonComplianceBulletinUseCase {

    private final GenerateNonComplianceBulletinUseCase generateBulletinUseCase;
    private final RepresentativeRepository representativeRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationEmailSender emailSender;
    private final RegisterDocumentEmissionUseCase registerEmissionUseCase;

    public record BulletinEmissionResult(String documentCode, int totalRecipients,
                                         int sent, int skipped) {
    }

    @Transactional(readOnly = true)
    public BulletinEmissionResult execute(Long projectId, Integer questionnaireId,
                                          String emittedBy, Long emittedByUserId) {
        log.info("[boletim-emit] Emitindo boletim projeto={} questionário={}", projectId, questionnaireId);

        GenerateNonComplianceBulletinUseCase.GeneratedBulletin bulletin =
                generateBulletinUseCase.execute(projectId, questionnaireId, emittedBy);

        List<Representative> representatives = representativeRepository.findByProjectId(projectId);
        if (representatives.isEmpty()) {
            throw new BusinessException(
                    "O projeto não possui representantes cadastrados para receber o boletim.");
        }

        NotificationTemplateDocument template = notificationTemplateRepository
                .findByKey(NotificationType.NON_COMPLIANCE_BULLETIN_EMITTED.templateKey())
                .orElse(null);

        String templateLink = template != null && isFilled(template.getTemplateLink())
                ? template.getTemplateLink()
                : "users/non-compliance-bulletin-emitted.ftl";
        String subject = resolveSubject(template, bulletin);
        String emittedAtFormatted = DocumentFormatUtil.dateTime(LocalDateTime.now());

        int sent = 0;
        int skipped = 0;
        for (Representative representative : representatives) {
            String email = representative.getUser() != null ? representative.getUser().getEmail() : null;
            if (!isFilled(email)) {
                skipped++;
                continue;
            }
            try {
                emailSender.sendWithAttachment(email, subject, templateLink,
                        buildModel(representative, bulletin, emittedBy, emittedAtFormatted),
                        bulletin.content(), bulletin.fileName());
                sent++;
            } catch (Exception e) {
                log.warn("[boletim-emit] Falha ao enviar boletim para {}: {}", email, e.getMessage());
                skipped++;
            }
        }

        registerEmissionUseCase.execute(new RegisterDocumentEmissionUseCase.EmissionRequest(
                RegisterDocumentEmissionUseCase.TYPE_BULLETIN,
                bulletin.documentCode(),
                projectId,
                bulletin.projectName(),
                questionnaireId,
                bulletin.questionnaireName(),
                null, null, null, null,
                "Questionário",
                emittedByUserId,
                emittedBy,
                bulletin.isepValue(),
                bulletin.band(),
                List.of(projectId, questionnaireId, bulletin.band(), bulletin.isepPercent())));

        log.info("[boletim-emit] Boletim {} emitido: {}/{} enviados (userId={})",
                bulletin.documentCode(), sent, representatives.size(), emittedByUserId);
        return new BulletinEmissionResult(bulletin.documentCode(), representatives.size(), sent, skipped);
    }

    private Map<String, Object> buildModel(Representative representative,
                                           GenerateNonComplianceBulletinUseCase.GeneratedBulletin bulletin,
                                           String emittedBy, String emittedAtFormatted) {
        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", representative.getUser() != null
                ? representative.getUser().getFirstName() : "Participante");
        model.put("projectName", bulletin.projectName());
        model.put("questionnaireName", bulletin.questionnaireName());
        model.put("isepPercent", bulletin.isepPercent());
        model.put("band", bulletin.band());
        model.put("minimumBand", EthicalComplianceBand.MINIMUM_ACCEPTABLE.name());
        model.put("emittedBy", emittedBy);
        model.put("emittedAtFormatted", emittedAtFormatted);
        return model;
    }

    private String resolveSubject(NotificationTemplateDocument template,
                                  GenerateNonComplianceBulletinUseCase.GeneratedBulletin bulletin) {
        String title = template != null && isFilled(template.getTitle())
                ? template.getTitle()
                : "Boletim de Não Conformidade Ética: {questionnaireName}";
        return title
                .replace("{questionnaireName}", nullSafe(bulletin.questionnaireName()))
                .replace("{projectName}", nullSafe(bulletin.projectName()))
                .replace("{isepPercent}", nullSafe(bulletin.isepPercent()))
                .replace("{band}", nullSafe(bulletin.band()));
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
