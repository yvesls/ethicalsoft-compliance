package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.TranslationCacheDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TranslationCacheRepository
        extends MongoRepository<TranslationCacheDocument, String> {

    Optional<TranslationCacheDocument> findByHash(String hash);
}
