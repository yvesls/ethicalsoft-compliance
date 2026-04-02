package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class ProjectEditSnapshotDTO {
    Long id;
    String name;
    String type;
    LocalDate startDate;
    LocalDate deadline;
    LocalDate closingDate;
    ProjectStatusEnum status;
    TimelineStatusEnum timelineStatus;
    Integer iterationDuration;
    Integer iterationCount;
    String currentSituation;
    boolean editable;
    String editableReason;

    List<StageSnapshot> stages;
    List<IterationSnapshot> iterations;
    List<QuestionnaireSnapshot> questionnaires;
    List<RepresentativeSnapshot> representatives;

    @Value
    @Builder
    public static class StageSnapshot {
        Integer id;
        String name;
        BigDecimal weight;
        int sequence;
        LocalDate applicationStartDate;
        LocalDate applicationEndDate;
        TimelineStatusEnum status;
        boolean locked;
        String lockReason;
    }

    @Value
    @Builder
    public static class IterationSnapshot {
        Integer id;
        String name;
        BigDecimal weight;
        LocalDate applicationStartDate;
        LocalDate applicationEndDate;
        TimelineStatusEnum status;
        boolean locked;
        String lockReason;
    }

    @Value
    @Builder
    public static class QuestionnaireSnapshot {
        Integer id;
        String name;
        Integer weight;
        String stageName;
        Integer stageId;
        String iterationName;
        Integer iterationId;
        LocalDate applicationStartDate;
        LocalDate applicationEndDate;
        TimelineStatusEnum status;
        String domain;
        String description;
        boolean hasIsepResult;
        boolean locked;
        String lockReason;
        List<QuestionSnapshot> questions;
    }

    @Value
    @Builder
    public static class QuestionSnapshot {
        Integer id;
        String text;
        Set<Long> roleIds;
        List<String> roleNames;
        List<String> stageNames;
        List<Integer> stageIds;
    }

    @Value
    @Builder
    public static class RepresentativeSnapshot {
        Long id;
        Long userId;
        String firstName;
        String lastName;
        String email;
        Set<Long> roleIds;
        List<String> roleNames;
        BigDecimal weight;
        boolean hasResponses;
        boolean locked;
        String lockReason;
    }
}

