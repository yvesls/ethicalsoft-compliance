package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.LinkDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ConsolidatedAnswerDTO(
        Long representativeId,
        String representativeName,
        List<String> roles,
        Integer questionnaireId,
        QuestionnaireResponseStatus responseStatus,
        LocalDateTime submissionDate,
        Long questionId,
        String questionText,
        List<Integer> stageIds,
        Boolean response,
        LinkDTO justification,
        LinkDTO evidence,
        List<LinkDTO> attachments
) {}

