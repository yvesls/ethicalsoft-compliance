package com.ethicalsoft.ethicalsoft_complience.application.usecase.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Representative;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.RepresentativeAnswerDetailDTO;
import com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard.RepresentativeResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.QuestionnaireResponseRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.repository.RepresentativeRepositoryPort;
import com.ethicalsoft.ethicalsoft_complience.domain.service.LinkMapper;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetRepresentativeResponsesUseCase {

    private final QuestionnaireResponseRepositoryPort questionnaireResponseRepository;
    private final RepresentativeRepositoryPort representativeRepository;
    private final LinkMapper linkMapper;

    public RepresentativeResponseDTO execute(Long projectId, Integer questionnaireId, Long representativeId) {
        log.info("[admin-responses] Buscando respostas representante={} questionário={} projeto={}",
                representativeId, questionnaireId, projectId);

        Representative representative = representativeRepository.findById(representativeId)
                .orElseThrow(() -> new BusinessException("Representante não encontrado"));

        if (!representative.getProject().getId().equals(projectId)) {
            throw new BusinessException("Representante não pertence ao projeto informado");
        }

        QuestionnaireResponse response = questionnaireResponseRepository
                .findByProjectIdAndQuestionnaireIdAndRepresentativeId(projectId, questionnaireId, representativeId)
                .orElseThrow(() -> new BusinessException("Registro de respostas não encontrado para o representante"));

        List<QuestionnaireResponse.AnswerDocument> answers = Optional.ofNullable(response.getAnswers())
                .orElse(Collections.emptyList());

        int totalQuestions = answers.size();
        int answeredQuestions = (int) answers.stream().filter(a -> a.getResponse() != null).count();
        int yesCount = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getResponse())).count();
        int noCount = (int) answers.stream().filter(a -> Boolean.FALSE.equals(a.getResponse())).count();

        List<RepresentativeAnswerDetailDTO> answerDtos = answers.stream()
                .map(this::toDetailDTO)
                .toList();

        String name = representative.getUser() != null
                ? representative.getUser().getFirstName() + " " + representative.getUser().getLastName()
                : "Representante #" + representativeId;

        return new RepresentativeResponseDTO(
                representativeId,
                name,
                questionnaireId,
                response.getStatus(),
                response.getSubmissionDate(),
                totalQuestions,
                answeredQuestions,
                yesCount,
                noCount,
                answerDtos
        );
    }

    private RepresentativeAnswerDetailDTO toDetailDTO(QuestionnaireResponse.AnswerDocument ans) {
        return new RepresentativeAnswerDetailDTO(
                ans.getQuestionId(),
                ans.getQuestionText(),
                ans.getStageIds(),
                ans.getRoleIds(),
                ans.getResponse(),
                linkMapper.toDto(ans.getJustification()),
                linkMapper.toDto(ans.getEvidence()),
                Optional.ofNullable(ans.getAttachments())
                        .map(list -> list.stream().map(linkMapper::toDto).toList())
                        .orElse(Collections.emptyList())
        );
    }
}

