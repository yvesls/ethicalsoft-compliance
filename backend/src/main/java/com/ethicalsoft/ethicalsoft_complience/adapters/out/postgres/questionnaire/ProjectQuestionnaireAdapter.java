package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.questionnaire;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model.QuestionnaireResponse;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.QuestionnaireResponseRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.*;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.QuestionDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.QuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.QuestionnaireResponseStatus;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.QuestionnaireRepository;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository.RoleRepository;
import com.ethicalsoft.ethicalsoft_complience.application.port.questionnaire.ProjectQuestionnaireCommandPort;
import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import com.ethicalsoft.ethicalsoft_complience.common.util.mapper.ModelMapperUtils;
import com.ethicalsoft.ethicalsoft_complience.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectQuestionnaireAdapter implements ProjectQuestionnaireCommandPort {

    private final QuestionnaireRepository questionnaireRepository;
    private final RoleRepository roleRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void createQuestionnaires(Project project,
                                     Set<QuestionnaireDTO> questionnaireDTOs,
                                     Map<String, Stage> stageMap,
                                     Map<String, Iteration> iterationMap) {
        try {
            log.info("[questionnaire] Criando questionários para projeto id={} quantidade={}", project != null ? project.getId() : null, questionnaireDTOs != null ? questionnaireDTOs.size() : 0);
            if (questionnaireDTOs == null || questionnaireDTOs.isEmpty()) {
                return;
            }

            Map<Long, Role> rolesById = roleRepository.findAll().stream()
                    .collect(Collectors.toMap(Role::getId, Function.identity()));

            for (QuestionnaireDTO qnDto : questionnaireDTOs) {
                Questionnaire questionnaire = ModelMapperUtils.map(qnDto, Questionnaire.class);
                questionnaire.setProject(project);

                boolean isWaterfall = ProjectTypeEnum.CASCATA.equals(Objects.requireNonNull(project).getType());
                boolean isIterative = ProjectTypeEnum.ITERATIVO.equals(project.getType());

                if (isWaterfall) {
                    Stage stage = stageMap.get(qnDto.getStageName());
                    questionnaire.setStage(stage);
                    questionnaire.setIterationRef(null);
                } else if (isIterative) {
                    Iteration iteration = iterationMap.get(qnDto.getIterationName());
                    questionnaire.setIterationRef(iteration);
                    questionnaire.setStage(null);
                }

                Questionnaire savedQuestionnaire = questionnaireRepository.save(questionnaire);
                List<Question> persistedQuestions = createQuestions(qnDto.getQuestions(), project, savedQuestionnaire, stageMap, rolesById);
                savedQuestionnaire.setQuestions(new HashSet<>(persistedQuestions));

                List<QuestionnaireResponse.AnswerDocument> answerTemplate = buildAnswerTemplate(persistedQuestions);
                createInitialResponses(project, savedQuestionnaire, answerTemplate);
            }
            log.info("[questionnaire] Questionários criados para projeto id={}", project != null ? project.getId() : null);
        } catch (Exception ex) {
            log.error("[questionnaire] Falha ao criar questionários para projeto id={}", project != null ? project.getId() : null, ex);
            throw ex;
        }
    }

    private List<Question> createQuestions(Set<QuestionDTO> questionDTOs,
                                          Project project,
                                          Questionnaire questionnaire,
                                          Map<String, Stage> stageMap,
                                          Map<Long, Role> rolesById) {

        if (ObjectUtils.isNullOrEmpty(questionDTOs)) {
            throw new BusinessException("É necessário ao menos uma questão para configurar o questionário '" + questionnaire.getName() + "'.");
        }

        return questionDTOs.stream().map(dto -> {
            String rawValue = Optional.ofNullable(dto.getValue()).map(String::trim).orElse(null);
            if (ObjectUtils.isNullOrEmpty(rawValue)) {
                throw new BusinessException("Questão sem texto detectada ao configurar o questionário '" + questionnaire.getName() + "'. Informe um texto válido.");
            }

            Question question = new Question();
            question.setQuestionnaire(questionnaire);
            question.setValue(rawValue);

            boolean isWaterfall = ProjectTypeEnum.CASCATA.equals(project.getType());
            if (isWaterfall) {
                Stage targetStage = dto.getCategoryStageName() != null ? stageMap.get(dto.getCategoryStageName()) : questionnaire.getStage();
                question.setStages(targetStage != null ? Collections.singleton(targetStage) : Collections.emptySet());
            } else {
                Set<Stage> mappedStages = Optional.ofNullable(dto.getStageNames())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(stageMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                question.setStages(mappedStages);
            }

            if (ObjectUtils.isNotNullAndNotEmpty(dto.getRoleIds())) {
                Set<Role> mappedRoles = dto.getRoleIds().stream()
                        .map(rolesById::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                question.setRoles(mappedRoles);
            }

            return question;
        }).map(questionRepository::save).toList();
    }

    private List<QuestionnaireResponse.AnswerDocument> buildAnswerTemplate(List<Question> persistedQuestions) {
        if (persistedQuestions == null || persistedQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        return persistedQuestions.stream().map(question -> {
            QuestionnaireResponse.AnswerDocument answer = new QuestionnaireResponse.AnswerDocument();
            answer.setQuestionId(question.getId().longValue());
            answer.setQuestionText(question.getValue());
            answer.setStageIds(Optional.ofNullable(question.getStages())
                    .orElse(Collections.emptySet())
                    .stream()
                    .map(Stage::getId)
                    .filter(Objects::nonNull)
                    .toList());
            if (question.getRoles() != null) {
                answer.setRoleIds(question.getRoles().stream().map(Role::getId).collect(Collectors.toList()));
            }
            return answer;
        }).collect(Collectors.toList());
    }

    private void createInitialResponses(Project project,
                                        Questionnaire questionnaire,
                                        List<QuestionnaireResponse.AnswerDocument> answerTemplate) {

        Set<Representative> representatives = Optional.ofNullable(project.getRepresentatives()).orElse(Collections.emptySet());

        if (representatives.isEmpty()) {
            QuestionnaireResponse base = new QuestionnaireResponse();
            base.setProjectId(project.getId());
            base.setQuestionnaireId(questionnaire.getId());
            base.setRepresentativeId(null);
            base.setStageId(questionnaire.getStage() != null ? questionnaire.getStage().getId() : null);
            base.setStatus(QuestionnaireResponseStatus.PENDING);
            base.setAnswers(cloneAnswerTemplate(answerTemplate));
            questionnaireResponseRepository.save(base);
            return;
        }

        List<QuestionnaireResponse> responseDocuments = representatives.stream().map(rep -> {
            QuestionnaireResponse response = new QuestionnaireResponse();
            response.setProjectId(project.getId());
            response.setQuestionnaireId(questionnaire.getId());
            response.setRepresentativeId(rep.getId());
            response.setStageId(questionnaire.getStage() != null ? questionnaire.getStage().getId() : null);
            response.setStatus(QuestionnaireResponseStatus.PENDING);
            response.setAnswers(cloneAnswerTemplate(answerTemplate));
            return response;
        }).collect(Collectors.toList());

        questionnaireResponseRepository.saveAll(responseDocuments);
    }

    private List<QuestionnaireResponse.AnswerDocument> cloneAnswerTemplate(List<QuestionnaireResponse.AnswerDocument> template) {
        if (template == null || template.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionnaireResponse.AnswerDocument> clones = new ArrayList<>();
        for (QuestionnaireResponse.AnswerDocument original : template) {
            QuestionnaireResponse.AnswerDocument copy = new QuestionnaireResponse.AnswerDocument();
            copy.setQuestionId(original.getQuestionId());
            copy.setQuestionText(original.getQuestionText());
            copy.setStageIds(original.getStageIds() != null ? new ArrayList<>(original.getStageIds()) : null);
            copy.setRoleIds(new ArrayList<>(Optional.ofNullable(original.getRoleIds()).orElse(Collections.emptyList())));
            copy.setResponse(null);
            copy.setJustification(null);
            copy.setEvidence(null);
            copy.setAttachments(new ArrayList<>());
            clones.add(copy);
        }
        return clones;
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<Questionnaire> getQuestionnairesByProjectId(Long projectId) {
        return questionnaireRepository.findByProjectId(projectId);
    }
}
