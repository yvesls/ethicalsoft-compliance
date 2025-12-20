package com.ethicalsoft.ethicalsoft_complience.domain.repository;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireResponseRepositoryPort {

    Optional<QuestionnaireResponse> findByQuestionnaireIdAndRepresentativeId(Integer questionnaireId, Long representativeId);

    List<QuestionnaireResponse> findByQuestionnaireId(Integer questionnaireId);

    List<QuestionnaireResponse> findByProjectIdAndQuestionnaireId(Long projectId, Integer questionnaireId);

    List<QuestionnaireResponse> findPendingResponses(Long projectId, Integer questionnaireId);

    Optional<QuestionnaireResponse> findByProjectIdAndQuestionnaireIdAndRepresentativeId(Long projectId, Integer questionnaireId, Long representativeId);

    List<QuestionnaireResponse> findSummariesByProjectAndQuestionnaire(Long projectId, Integer questionnaireId);

    QuestionnaireResponse save(QuestionnaireResponse response);
}

