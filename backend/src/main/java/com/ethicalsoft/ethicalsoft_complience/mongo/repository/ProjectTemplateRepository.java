package com.ethicalsoft.ethicalsoft_complience.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectTemplateRepository extends MongoRepository<ProjectTemplate, String> {
	@Query(value = "{ $or: [ { 'visibility': 'PUBLIC' }, { 'userId': ?0 } ] }",
			fields = "{ 'name': 1, 'description': 1, 'type': 1, 'visibility': 1 }")
	List<ProjectTemplate> findTemplateSummariesForUser( Long userId);

	@Query(value = "{ '_id': ?0 }", fields = "{ 'name': 1, 'description': 1, 'type': 1, 'visibility': 1, 'userId': 1 }")
	Optional<ProjectTemplate> findHeaderById(String id);

	@Query(value = "{ '_id': ?0 }", fields = "{ 'stages': 1, 'visibility': 1, 'userId': 1 }")
	Optional<ProjectTemplate> findStagesById(String id);

	@Query(value = "{ '_id': ?0 }", fields = "{ 'iterations': 1, 'visibility': 1, 'userId': 1 }")
	Optional<ProjectTemplate> findIterationsById(String id);

	@Query(value = "{ '_id': ?0 }", fields = "{ 'questionnaires': 1, 'visibility': 1, 'userId': 1 }")
	Optional<ProjectTemplate> findQuestionnairesById(String id);

	@Query(value = "{ '_id': ?0 }", fields = "{ 'representatives': 1, 'visibility': 1, 'userId': 1 }")
	Optional<ProjectTemplate> findRepresentativesById( String id);
}