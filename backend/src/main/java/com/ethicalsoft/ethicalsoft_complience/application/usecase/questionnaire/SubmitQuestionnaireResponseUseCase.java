package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitQuestionnaireResponseUseCase {

    private final ProjectRepository projectRepository;
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;

    @Transactional
    public void execute(Long projectId, Long questionnaireId, List<QuestionnaireResponse> responses) {
        try {
            log.info("[usecase-submit-response] Atualizando respostas do questionário id={} para o projeto id={}", questionnaireId, projectId);

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

            questionnaireRepository.findById(questionnaireId.intValue())
                    .orElseThrow(() -> new EntityNotFoundException("Questionário não encontrado: " + questionnaireId));

            processResponses(project, questionnaireId, responses);

            log.info("[usecase-submit-response] Respostas atualizadas com sucesso");
        } catch (Exception ex) {
            log.error("[usecase-submit-response] Falha ao atualizar respostas", ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireResponse> getResponses(Long projectId, Long questionnaireId) {
        if (!projectRepository.existsById(projectId)) {
            throw new EntityNotFoundException("Projeto não encontrado: " + projectId);
        }
        return questionnaireResponseRepository.findByProjectIdAndQuestionnaireId(projectId, questionnaireId.intValue());
    }

    private void processResponses(Project project, Long questionnaireId, List<QuestionnaireResponse> responses) {
        Set<Representative> projectRepresentatives = Optional.ofNullable(project.getRepresentatives()).orElse(Set.of());
        Map<Long, Representative> representativesById = projectRepresentatives.stream()
                .collect(Collectors.toMap(Representative::getId, rep -> rep));

        Map<Long, List<QuestionnaireResponse>> responsesByRepresentative = responses.stream()
                .filter(response -> response.getRepresentativeId() != null)
                .collect(Collectors.groupingBy(QuestionnaireResponse::getRepresentativeId));

        for (Map.Entry<Long, List<QuestionnaireResponse>> entry : responsesByRepresentative.entrySet()) {
            Long representativeId = entry.getKey();
            List<QuestionnaireResponse> representativeResponses = entry.getValue();

            if (representativesById.containsKey(representativeId)) {
                for (QuestionnaireResponse response : representativeResponses) {
                    response.setProjectId(project.getId());
                    response.setQuestionnaireId(questionnaireId.intValue());
                    response.setRepresentativeId(representativeId);
                    response.setSubmissionDate(LocalDateTime.now());
                    questionnaireResponseRepository.save(response);
                }
            }
        }
    }
}