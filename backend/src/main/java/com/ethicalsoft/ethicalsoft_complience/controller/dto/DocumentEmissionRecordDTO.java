package com.ethicalsoft.ethicalsoft_complience.controller.dto;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.DocumentEmissionRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DocumentEmissionRecordDTO(
        String authenticityCode,
        String documentType,
        Long projectId,
        String projectName,
        Integer questionnaireId,
        String questionnaireName,
        String scopeLabel,
        LocalDateTime emittedAt,
        Long emittedByUserId,
        String emittedByName,
        BigDecimal isepPercent,
        String band,
        String dataHash,
        String templateVersion
) {
    public static DocumentEmissionRecordDTO from(DocumentEmissionRecord source) {
        if (source == null) return null;
        return new DocumentEmissionRecordDTO(
                source.getAuthenticityCode(),
                source.getDocumentType(),
                source.getProjectId(),
                source.getProjectName(),
                source.getQuestionnaireId(),
                source.getQuestionnaireName(),
                source.getScopeLabel(),
                source.getEmittedAt(),
                source.getEmittedByUserId(),
                source.getEmittedByName(),
                source.getIsepPercent(),
                source.getBand(),
                source.getDataHash(),
                source.getTemplateVersion()
        );
    }
}
