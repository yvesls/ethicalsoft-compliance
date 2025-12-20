package com.ethicalsoft.ethicalsoft_complience.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    Page<Question> findByQuestionnaireIdOrderByIdAsc(Integer questionnaireId, Pageable pageable);

    @Query("""
            select distinct q
            from Question q
            left join q.roles r
            where q.questionnaire.id = :questionnaireId
              and (COALESCE(:questionText, '') = '' or lower(q.value) like lower(concat('%', :questionText, '%')))
              and (COALESCE(:roleName, '') = '' or lower(r.name) like lower(concat('%', :roleName, '%')))
            """)
    Page<Question> searchByQuestionnaireId(@Param("questionnaireId") Integer questionnaireId,
                                          @Param("questionText") String questionText,
                                          @Param("roleName") String roleName,
                                          Pageable pageable);
}
