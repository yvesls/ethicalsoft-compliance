package com.ethicalsoft.ethicalsoft_complience.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireResponseRepository extends MongoRepository<QuestionnaireResponse, String> {

    Optional<QuestionnaireResponse> findByQuestionnaireIdAndRepresentativeId(Integer questionnaireId, Long representativeId);

    List<QuestionnaireResponse> findByQuestionnaireId(Integer questionnaireId);

    List<QuestionnaireResponse> findByProjectIdAndQuestionnaireId(Long projectId, Integer questionnaireId);

    @Query(value = "{ 'projectId' : ?0, 'questionnaireId' : ?1, 'status' : 'PENDING' }", fields = "{ 'representativeId': 1 }")
    List<QuestionnaireResponse> findPendingResponses(Long projectId, Integer questionnaireId);

    Optional<QuestionnaireResponse> findByProjectIdAndQuestionnaireIdAndRepresentativeId(Long projectId, Integer questionnaireId, Long representativeId);

    @Query(value = "{ 'projectId': ?0, 'questionnaireId': ?1 }", fields = "{ 'answers': 0 }")
    List<QuestionnaireResponse> findSummariesByProjectAndQuestionnaire(Long projectId, Integer questionnaireId);
}
