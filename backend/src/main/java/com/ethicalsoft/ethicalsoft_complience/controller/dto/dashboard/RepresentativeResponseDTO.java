package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RepresentativeResponseDTO(
        Long representativeId,
        String representativeName,
        Integer questionnaireId,
        QuestionnaireResponseStatus status,
        LocalDateTime submissionDate,
        int totalQuestions,
        int answeredQuestions,
        int yesCount,
        int noCount,
        List<RepresentativeAnswerDetailDTO> answers
) {}

