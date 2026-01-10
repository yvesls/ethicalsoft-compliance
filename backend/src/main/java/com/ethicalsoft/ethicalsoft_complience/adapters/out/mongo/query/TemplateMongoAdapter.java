package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.query;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.ProjectTemplate;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.dto.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.CreateTemplateRequestDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response.RoleSummaryResponseDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TemplateVisibilityEnum;
import com.ethicalsoft.ethicalsoft_complience.application.port.CurrentUserPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.TemplateCommandPort;
import com.ethicalsoft.ethicalsoft_complience.application.port.TemplateQueryPort;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.ProjectTemplateRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateMongoAdapter implements TemplateQueryPort, TemplateCommandPort {

    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public ProjectTemplate findFullTemplateById(String templateMongoId) {
        try {
            Long currentUserId = currentUserPort.getCurrentUser().getId();
            log.info("[template] Buscando template completo id={} para usuário {}", templateMongoId, currentUserId);
            ProjectTemplate template = projectTemplateRepository.findById(templateMongoId)
                    .orElseThrow(() -> new EntityNotFoundException("Template não encontrado: " + templateMongoId));
            checkAccess(template, currentUserId);
            log.info("[template] Template {} carregado", templateMongoId);
            return template;
        } catch (Exception ex) {
            log.error("[template] Falha ao buscar template id={}", templateMongoId, ex);
            throw ex;
        }
    }

    @Override
    public List<TemplateListDTO> findAllTemplates() {
        try {
            Long currentUserId = currentUserPort.getCurrentUser().getId();
            log.info("[template] Listando templates para usuário {}", currentUserId);
            List<TemplateListDTO> templates = projectTemplateRepository.findTemplateSummariesForUser(currentUserId).stream()
                    .map(t -> new TemplateListDTO(t.getId(), t.getName(), t.getDescription(), t.getType()))
                    .toList();
            log.info("[template] {} templates retornados", templates.size());
            return templates;
        } catch (Exception ex) {
            log.error("[template] Falha ao listar templates", ex);
            throw ex;
        }
    }

    @Override
    public ProjectTemplate createTemplateFromProject(Long projectId, CreateTemplateRequestDTO request) {
        try {
            Long currentUserId = currentUserPort.getCurrentUser().getId();
            log.info("[template] Criando template a partir do projeto {} para usuário {}", projectId, currentUserId);

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

            ProjectTemplate saved = projectTemplateRepository.save(template);
            log.info("[template] Template {} criado para projeto {}", saved.getId(), projectId);
            return saved;
        } catch (Exception ex) {
            log.error("[template] Falha ao criar template a partir do projeto {}", projectId, ex);
            throw ex;
        }
    }

    private void checkAccess(ProjectTemplate template, Long currentUserId) {
        if (template.getVisibility() == TemplateVisibilityEnum.PRIVATE && !template.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Você não tem permissão para acessar este template.");
        }
    }

    private List<TemplateStageDTO> mapStages(Set<Stage> stages) {
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
                dto.setRoles(rep.getRoles().stream()
                        .map(role -> new RoleSummaryResponseDTO(role.getId(), role.getName()))
                        .collect(Collectors.toSet()));
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
            if (question.getRoles() != null) {
                pDto.setRoles(question.getRoles().stream()
                        .map(role -> new RoleSummaryResponseDTO(role.getId(), role.getName()))
                        .collect(Collectors.toSet()));
            }
            return pDto;
        }).toList();
    }
}
