package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.repository;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findByQuestionnaireIdOrderByIdAsc(Integer questionnaireId, Pageable pageable);

    @Query("select distinct q from Question q " +
            "left join q.roles r " +
            "where q.questionnaire.id = :questionnaireId " +
            "and (:questionText is null or lower(cast(q.value as string)) like lower(concat('%', cast(:questionText as string), '%'))) " +
            "and (:roleName is null or lower(cast(r.name as string)) like lower(concat('%', cast(:roleName as string), '%')))")
    Page<Question> searchByQuestionnaireId(Integer questionnaireId, String questionText, String roleName, Pageable pageable);

    @Query("select distinct q from Question q " +
            "join q.roles r " +
            "where q.questionnaire.id = :questionnaireId " +
            "and (:questionText is null or lower(cast(q.value as string)) like lower(concat('%', cast(:questionText as string), '%'))) " +
            "and (:roleName is null or lower(cast(r.name as string)) like lower(concat('%', cast(:roleName as string), '%'))) " +
            "and r.id in :roleIds")
    Page<Question> searchByQuestionnaireIdAndRoleIds(Integer questionnaireId, String questionText, String roleName, List<Long> roleIds, Pageable pageable);

    @Query("select distinct q from Question q join q.roles r where q.questionnaire.id = :questionnaireId and r.id in :roleIds")
    Page<Question> findByQuestionnaireIdAndRoleIds(Integer questionnaireId, List<Long> roleIds, Pageable pageable);

    Optional<Question> findById(Long id);
}
