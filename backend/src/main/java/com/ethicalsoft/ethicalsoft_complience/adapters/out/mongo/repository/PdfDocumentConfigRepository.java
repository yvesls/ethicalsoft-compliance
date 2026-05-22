package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.PdfDocumentConfigDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PdfDocumentConfigRepository extends MongoRepository<PdfDocumentConfigDocument, String> {

    Optional<PdfDocumentConfigDocument> findByKey(String key);
}
