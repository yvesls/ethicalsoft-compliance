package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.DocumentEmissionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentEmissionRecordRepository
        extends MongoRepository<DocumentEmissionRecord, String> {

    Optional<DocumentEmissionRecord> findByAuthenticityCode(String authenticityCode);

    List<DocumentEmissionRecord> findByProjectIdOrderByEmittedAtDesc(Long projectId);

    List<DocumentEmissionRecord> findByProjectIdAndDocumentTypeOrderByEmittedAtDesc(
            Long projectId, String documentType);

    List<DocumentEmissionRecord> findByProjectIdAndQuestionnaireIdOrderByEmittedAtDesc(
            Long projectId, Integer questionnaireId);
}
