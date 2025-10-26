package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateListDTO;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.TemplateStageDTO;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.ProjectTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.Project;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import util.ModelMapperUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

	private final ProjectTemplateRepository projectTemplateRepository;
	private final ProjectRepository projectRepository;

	public ProjectTemplate findById( String templateMongoId ) {
		return projectTemplateRepository.findById( templateMongoId ).orElseThrow( () -> new EntityNotFoundException( "Template não encontrado: " + templateMongoId ) );
	}

	public List<TemplateListDTO> findAllTemplates() {
		return projectTemplateRepository.findAll().stream().map( template -> new TemplateListDTO( template.getId(), template.getName(), template.getDescription(), template.getType() ) ).toList();
	}

	public List<TemplateStageDTO> findTemplateStages( String templateMongoId ) {
		ProjectTemplate template = findById( templateMongoId );
		return template.getStages();
	}

	@Transactional( readOnly = true )
	public ProjectTemplate createTemplateFromProject( Long projectId, String templateName ) {
		Project project = projectRepository.findById( projectId ).orElseThrow( () -> new EntityNotFoundException( "Projeto não encontrado: " + projectId ) );
		return projectTemplateRepository.save( ModelMapperUtils.map( project, ProjectTemplate.class ) );
	}
}