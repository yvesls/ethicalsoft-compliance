package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import java.time.LocalDateTime;

public record CloseProjectResponseDTO(
        Long projectId,
        String isepPercent,
        String band,
        Integer questionnaireCount,
        LocalDateTime calculatedAt,
        String closedBy
) {}
