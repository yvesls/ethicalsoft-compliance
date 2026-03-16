package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.User;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.auth.AuthenticatedUserPort;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import com.ethicalsoft.ethicalsoft_complience.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForceCloseQuestionnaireUseCase {

    private final QuestionnaireRepository questionnaireRepository;
    private final ProcessQuestionnaireIsepUseCase processQuestionnaireIsepUseCase;
    private final ProcessExpiredQuestionnairesIsepUseCase processExpiredQuestionnairesIsepUseCase;
    private final AuthenticatedUserPort authenticatedUserPort;

    @Transactional
    public void execute(Long projectId, Integer questionnaireId) {
        log.info("[force-close] Solicitado encerramento forçado: questionário={} projeto={}", questionnaireId, projectId);

        Questionnaire questionnaire = questionnaireRepository.findByIdAndProjectId(questionnaireId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questionário id=" + questionnaireId + " não encontrado no projeto id=" + projectId));

        if (questionnaire.getStatus() == TimelineStatusEnum.CONCLUIDO) {
            throw new BusinessException("Questionário id=" + questionnaireId + " já está encerrado.");
        }

        boolean calculated = processQuestionnaireIsepUseCase.forceCalculate(projectId, questionnaireId);

        if (!calculated) {
            throw new BusinessException(
                    "Não foi possível calcular o ISEP para o questionário id=" + questionnaireId +
                    ". Verifique se existem respostas registradas.");
        }

        questionnaire.setStatus(TimelineStatusEnum.CONCLUIDO);
        questionnaireRepository.save(questionnaire);

        log.info("[force-close] Questionário id={} encerrado com sucesso pelo administrador.", questionnaireId);

        String closedBy = resolveClosedBy();
        processExpiredQuestionnairesIsepUseCase.notifyIsepCalculated(questionnaire, projectId, closedBy);
    }

    private String resolveClosedBy() {
        try {
            User user = authenticatedUserPort.getAuthenticatedUser();
            if (user == null) return "Administrador";
            String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                    (user.getLastName() != null ? user.getLastName() : "")).trim();
            return name.isBlank() ? user.getEmail() : name;
        } catch (Exception ex) {
            return "Administrador";
        }
    }
}
