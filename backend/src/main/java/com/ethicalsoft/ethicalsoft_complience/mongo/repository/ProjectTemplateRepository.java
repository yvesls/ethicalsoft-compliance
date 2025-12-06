package com.ethicalsoft.ethicalsoft_complience.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectTemplateRepository extends MongoRepository<ProjectTemplate, String> {
	@Query(value = "{ $or: [ { 'visibility': 'PUBLIC' }, { 'userId': ?0 } ] }",
			fields = "{ 'name': 1, 'description': 1, 'type': 1, 'visibility': 1 }")
	List<ProjectTemplate> findTemplateSummariesForUser( Long userId);
}