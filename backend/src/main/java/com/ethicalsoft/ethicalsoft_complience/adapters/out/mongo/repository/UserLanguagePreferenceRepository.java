package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.UserLanguagePreferenceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserLanguagePreferenceRepository
        extends MongoRepository<UserLanguagePreferenceDocument, String> {

    Optional<UserLanguagePreferenceDocument> findByUserId(Long userId);
}
