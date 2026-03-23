package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionMetadataRepository extends MongoRepository<QuestionMetadataDocument, String> {

    List<QuestionMetadataDocument> findByQuestionIdIn(Collection<Long> questionIds);

    Optional<QuestionMetadataDocument> findByQuestionId(Long questionId);

    boolean existsByQuestionId(Long questionId);
}

