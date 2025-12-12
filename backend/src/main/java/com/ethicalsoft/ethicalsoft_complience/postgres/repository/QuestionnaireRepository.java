package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Questionnaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long>, JpaSpecificationExecutor<Questionnaire> {
	@Query( "SELECT qn FROM Questionnaire qn LEFT JOIN FETCH qn.questions q WHERE qn.project.id = :projectId" )
	List<Questionnaire> findAllByProjectIdWithQuestions( Long projectId );

	Page<Questionnaire> findByProjectId( Long projectId, Pageable pageable );
}