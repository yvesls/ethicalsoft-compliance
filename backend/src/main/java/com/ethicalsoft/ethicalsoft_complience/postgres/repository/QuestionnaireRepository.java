package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Integer>, JpaSpecificationExecutor<Questionnaire> {
	@Query( "SELECT qn FROM Questionnaire qn LEFT JOIN FETCH qn.questions q WHERE qn.project.id = :projectId" )
	List<Questionnaire> findAllByProjectIdWithQuestions( Long projectId );

	Page<Questionnaire> findByProjectId( Long projectId, Pageable pageable );

	List<Questionnaire> findByProjectId(Long projectId);

	@Query("select q from Questionnaire q where q.applicationStartDate <= :today and q.applicationEndDate >= :today and q.project.id = :projectId")
	List<Questionnaire> findActiveByProjectAndDate(Long projectId, LocalDate today);

	@Query("select q from Questionnaire q where q.applicationStartDate = :today and q.status <> 'CONCLUIDO'")
	List<Questionnaire> findQuestionnairesStartingToday(LocalDate today);
}
