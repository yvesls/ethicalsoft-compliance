package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireResponseRepository extends MongoRepository<QuestionnaireResponse, String> {

    Optional<QuestionnaireResponse> findByQuestionnaireIdAndRepresentativeId(Integer questionnaireId, Long representativeId);

    List<QuestionnaireResponse> findByQuestionnaireId(Integer questionnaireId);

    List<QuestionnaireResponse> findByProjectIdAndQuestionnaireId(Long projectId, Integer questionnaireId);

    @Query("{ 'projectId': ?0, 'questionnaireId': ?1, 'status': 'PENDING' }")
    List<QuestionnaireResponse> findPendingResponses(Long projectId, Integer questionnaireId);

    Optional<QuestionnaireResponse> findByProjectIdAndQuestionnaireIdAndRepresentativeId(Long projectId, Integer questionnaireId, Long representativeId);

    @Query(value = "{ 'projectId': ?0, 'questionnaireId': ?1 }", fields = "{ 'representativeId': 1, 'status': 1, 'submissionDate': 1 }")
    List<QuestionnaireResponse> findSummariesByProjectAndQuestionnaire(Long projectId, Integer questionnaireId);
}
