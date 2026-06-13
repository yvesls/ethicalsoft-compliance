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
    public static DocumentEmissionRecordDTO from(DocumentEmissionRecord record) {
        if (record == null) return null;
        return new DocumentEmissionRecordDTO(
                record.getAuthenticityCode(),
                record.getDocumentType(),
                record.getProjectId(),
                record.getProjectName(),
                record.getQuestionnaireId(),
                record.getQuestionnaireName(),
                record.getScopeLabel(),
                record.getEmittedAt(),
                record.getEmittedByUserId(),
                record.getEmittedByName(),
                record.getIsepPercent(),
                record.getBand(),
                record.getDataHash(),
                record.getTemplateVersion()
        );
    }
}
