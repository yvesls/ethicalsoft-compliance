package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface QuestionMetadataRepository extends MongoRepository<QuestionMetadataDocument, String> {

    List<QuestionMetadataDocument> findByQuestionIdIn(Collection<Long> questionIds);

    boolean existsByQuestionId(Long questionId);
}

