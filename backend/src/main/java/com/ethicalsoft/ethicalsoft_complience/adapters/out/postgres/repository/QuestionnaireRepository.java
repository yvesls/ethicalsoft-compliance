package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Questionnaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Integer>, JpaSpecificationExecutor<Questionnaire> {

    Page<Questionnaire> findByProjectId(Long projectId, Pageable pageable);

    List<Questionnaire> findByProjectId(Long projectId);

    @Query("select q from Questionnaire q left join fetch q.questions where q.project.id = :projectId")
    List<Questionnaire> findAllByProjectIdWithQuestions(Long projectId);

    @Query("select q from Questionnaire q where q.project.id = :projectId and :today between q.applicationStartDate and q.applicationEndDate")
    List<Questionnaire> findActiveByProjectAndDate(Long projectId, LocalDate today);

    @Query("select q from Questionnaire q where q.applicationStartDate = :today")
    List<Questionnaire> findQuestionnairesStartingToday(LocalDate today);

    Optional<Questionnaire> findByIdAndProjectId(Integer questionnaireId, Long projectId);

    @Override
    Page<Questionnaire> findAll(Specification<Questionnaire> spec, Pageable pageable);
}

