package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Builder
public record ProjectEditSnapshotDTO(Long id, String name, String type, LocalDate startDate, LocalDate deadline,
                                     LocalDate closingDate, ProjectStatusEnum status, TimelineStatusEnum timelineStatus,
                                     Integer iterationDuration, Integer iterationCount, String currentSituation,
                                     boolean editable, String editableReason, List<StageSnapshot> stages,
                                     List<IterationSnapshot> iterations, List<QuestionnaireSnapshot> questionnaires,
                                     List<RepresentativeSnapshot> representatives) {
    @Builder
        public record StageSnapshot(Integer id, String name, BigDecimal weight, int sequence, Integer durationDays,
                                    LocalDate applicationStartDate, LocalDate applicationEndDate, TimelineStatusEnum status,
                                    boolean locked, String lockReason) {
    }

    @Builder
        public record IterationSnapshot(Integer id, String name, BigDecimal weight, LocalDate applicationStartDate,
                                        LocalDate applicationEndDate, TimelineStatusEnum status, boolean locked,
                                        String lockReason) {
    }

    @Builder
        public record QuestionnaireSnapshot(Integer id, String name, BigDecimal weight, String stageName, Integer stageId,
                                            String iterationName, Integer iterationId, LocalDate applicationStartDate,
                                            LocalDate applicationEndDate, TimelineStatusEnum status, String domain,
                                            String description, boolean hasIsepResult, boolean locked, String lockReason,
                                            List<QuestionSnapshot> questions) {
    }

    @Builder
        public record QuestionSnapshot(Integer id, String text, Set<Long> roleIds, List<String> roleNames,
                                       List<String> stageNames, List<Integer> stageIds) {
    }

    @Builder
        public record RepresentativeSnapshot(Long id, Long userId, String firstName, String lastName, String email,
                                             Set<Long> roleIds, List<String> roleNames, BigDecimal weight,
                                             boolean hasResponses, boolean locked, String lockReason) {
    }
}

