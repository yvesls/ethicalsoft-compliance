package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findByQuestionnaireIdOrderByIdAsc(Integer questionnaireId, Pageable pageable);

    @Query("select q from Question q " +
            "left join q.roles r " +
            "where q.questionnaire.id = :questionnaireId " +
            "and (:questionText is null or lower(q.value) like lower(concat('%', :questionText, '%'))) " +
            "and (:roleName is null or lower(r.name) like lower(concat('%', :roleName, '%'))) ")
    Page<Question> searchByQuestionnaireId(Integer questionnaireId, String questionText, String roleName, Pageable pageable);
}

