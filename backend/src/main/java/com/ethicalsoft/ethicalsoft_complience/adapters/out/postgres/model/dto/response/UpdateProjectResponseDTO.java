package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ProjectStatusEnum;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.TimelineStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class UpdateProjectResponseDTO {

    private Long id;
    private String name;
    private String type;
    private ProjectStatusEnum status;
    private LocalDate startDate;
    private LocalDate deadline;
    private TimelineStatusEnum timelineStatus;

    private ChangesSummaryDTO changesSummary;

    @Data
    @Builder
    public static class ChangesSummaryDTO {
        private int stagesAdded;
        private int stagesRemoved;
        private int stagesUpdated;

        private int iterationsAdded;
        private int iterationsRemoved;
        private int iterationsUpdated;

        private int questionnairesAdded;
        private int questionnairesRemoved;
        private int questionnairesUpdated;

        private int questionsAdded;
        private int questionsRemoved;
        private int questionsUpdated;

        private int representativesAdded;
        private int representativesRemoved;
        private int representativesUpdated;

        private int responsesCreated;
        private int responsesUpdated;
        private int responsesDeleted;

        private int notificationsSent;

        private List<String> warnings;
        private List<String> blockedReasons;
    }
}

