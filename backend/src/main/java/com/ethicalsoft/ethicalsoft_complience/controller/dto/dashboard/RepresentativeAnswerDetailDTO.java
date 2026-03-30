package com.ethicalsoft.ethicalsoft_complience.controller.dto.dashboard;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.LinkDTO;

import java.util.List;

public record RepresentativeAnswerDetailDTO(
        Long questionId,
        String questionText,
        List<Integer> stageIds,
        List<Long> roleIds,
        Boolean response,
        LinkDTO justification,
        LinkDTO evidence,
        List<LinkDTO> attachments
) {}

