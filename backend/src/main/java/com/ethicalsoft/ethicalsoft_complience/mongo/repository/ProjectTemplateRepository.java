package com.ethicalsoft.ethicalsoft_complience.mongo.repository;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTemplateRepository extends MongoRepository<ProjectTemplate, String> {}