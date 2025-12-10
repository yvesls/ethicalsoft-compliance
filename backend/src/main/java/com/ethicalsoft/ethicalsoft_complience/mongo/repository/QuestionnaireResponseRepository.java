package com.ethicalsoft.ethicalsoft_complience.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.QuestionnaireResponseDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireResponseRepository extends MongoRepository<QuestionnaireResponseDocument, String> {

    Optional<QuestionnaireResponseDocument> findByQuestionnaireIdAndRepresentativeId(Integer questionnaireId, Long representativeId);

    List<QuestionnaireResponseDocument> findByQuestionnaireId(Integer questionnaireId);

    List<QuestionnaireResponseDocument> findByProjectIdAndQuestionnaireId(Long projectId, Integer questionnaireId);
}

