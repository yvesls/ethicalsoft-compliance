package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.TemplateSummaryProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProjectTemplateRepository extends MongoRepository<ProjectTemplate, String> {

    @Query(value = "{ $or: [ { 'visibility': 'PUBLIC' }, { 'visibility': 'PRIVATE', 'userId': ?0 } ] }",
            fields = "{ 'id': 1, 'name': 1, 'description': 1, 'type': 1 }")
    List<TemplateSummaryProjection> findTemplateSummariesForUser(Long userId);
}
