package com.ethicalsoft.ethicalsoft_complience.service;

import com.ethicalsoft.ethicalsoft_complience.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.mongo.model.dto.*;
import com.ethicalsoft.ethicalsoft_complience.mongo.repository.ProjectTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.CreateTemplateRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.TemplateVisibilityEnum;
import com.ethicalsoft.ethicalsoft_complience.postgres.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

	private final ProjectTemplateRepository projectTemplateRepository;
	private final ProjectRepository projectRepository;
	private final AuthService authService;

	public ProjectTemplate findFullTemplateById(String templateMongoId) {
		Long currentUserId = authService.getAuthenticatedUserId();
		ProjectTemplate template = projectTemplateRepository.findById(templateMongoId)
				.orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + templateMongoId));
		checkAccess(template, currentUserId);
		return template;
	}

	public List<TemplateListDTO> findAllTemplates() {
		Long currentUserId = authService.getAuthenticatedUserId();
		return projectTemplateRepository.findTemplateSummariesForUser(currentUserId).stream()
				.map(t -> new TemplateListDTO(t.getId(), t.getName(), t.getDescription(), t.getType()))
				.toList();
	}

	public ProjectTemplate createTemplateFromProject(Long projectId, CreateTemplateRequestDTO request) {
        Long currentUserId = authService.getAuthenticatedUserId();

        Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado: " + projectId));

		ProjectTemplate template = new ProjectTemplate();

		template.setName(request.getName());
		template.setDescription(request.getDescription());
		template.setVisibility(request.getVisibility());
		if (request.getVisibility() == TemplateVisibilityEnum.PRIVATE) {
			template.setUserId(currentUserId);
		}
		template.setType(project.getType());
		template.setDefaultIterationCount(project.getIterationCount());
		template.setDefaultIterationDuration(project.getIterationDuration());

		template.setStages(mapStages(project.getStages()));
		template.setIterations(mapIterations(project.getIterations()));
		template.setRepresentatives(mapRepresentatives(project.getRepresentatives()));
		template.setQuestionnaires(mapQuestionnaires(project.getQuestionnaires()));

		return projectTemplateRepository.save(template);
	}

	private void checkAccess(ProjectTemplate template, Long currentUserId) {
		if (template.getVisibility() == TemplateVisibilityEnum.PRIVATE) {
			if (!template.getUserId().equals(currentUserId)) {
				throw new AccessDeniedException("Você não tem permissão para acessar este template.");
			}
		}
	}

	private List<TemplateStageDTO> mapStages( Set<Stage> stages) {
		if (stages == null) return Collections.emptyList();
		return stages.stream().map(stage -> {
			TemplateStageDTO dto = new TemplateStageDTO();
			dto.setName(stage.getName());
			dto.setWeight(stage.getWeight());
			dto.setSequence(stage.getSequence());
			return dto;
		}).toList();
	}

	private List<TemplateIterationDTO> mapIterations(Set<Iteration> iterations) {
		if (iterations == null) return Collections.emptyList();
		return iterations.stream().map(iter -> {
			TemplateIterationDTO dto = new TemplateIterationDTO();
			dto.setName(iter.getName());
			dto.setWeight(iter.getWeight());
			return dto;
		}).toList();
	}

	private List<TemplateRepresentativeDTO> mapRepresentatives(Set<Representative> representatives) {
		if (representatives == null) return Collections.emptyList();
		return representatives.stream().map(rep -> {
			TemplateRepresentativeDTO dto = new TemplateRepresentativeDTO();
			dto.setWeight(rep.getWeight());
			if (rep.getUser() != null) {
				dto.setEmail(rep.getUser().getEmail());
				dto.setFirstName(rep.getUser().getFirstName());
				dto.setLastName(rep.getUser().getLastName());
			}
			if (rep.getRoles() != null) {
				dto.setRoleNames(rep.getRoles().stream()
						.map( Role::getName)
						.collect( Collectors.toSet()));
			}
			return dto;
		}).toList();
	}

	private List<TemplateQuestionnaireDTO> mapQuestionnaires(Set<Questionnaire> questionnaires) {
		if (questionnaires == null) return Collections.emptyList();
		return questionnaires.stream().map(q -> {
			TemplateQuestionnaireDTO qDto = new TemplateQuestionnaireDTO();
			qDto.setName(q.getName());
			if (q.getStage() != null) {
				qDto.setStageName(q.getStage().getName());
			}
			if (q.getIterationRef() != null) {
				qDto.setIterationRefName(q.getIterationRef().getName());
			}
			qDto.setQuestions(mapQuestions(q.getQuestions()));
			return qDto;
		}).toList();
	}

	private List<TemplateQuestionDTO> mapQuestions(Set<Question> questions) {
		if (questions == null) return Collections.emptyList();
		return questions.stream().map(question -> {
			TemplateQuestionDTO pDto = new TemplateQuestionDTO();
			pDto.setValue(question.getValue());
			if (question.getStage() != null) {
				pDto.setStageName(question.getStage().getName());
			}
			if (question.getRoles() != null) {
				pDto.setRoleNames(question.getRoles().stream()
						.map(Role::getName)
						.collect(Collectors.toSet()));
			}
			return pDto;
		}).toList();
	}
}