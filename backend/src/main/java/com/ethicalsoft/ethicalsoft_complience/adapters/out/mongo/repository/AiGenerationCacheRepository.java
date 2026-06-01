package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.AiGenerationCacheDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AiGenerationCacheRepository
        extends MongoRepository<AiGenerationCacheDocument, String> {

    Optional<AiGenerationCacheDocument> findByHash(String hash);
}
