package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class UpdateProjectRequestDTO {

    private String name;
    private LocalDate startDate;
    private LocalDate deadline;
    private Integer iterationDuration;
    private Integer iterationCount;

    private List<UpdateStageDTO> stages;
    private Set<UpdateIterationDTO> iterations;
    private Set<UpdateQuestionnaireDTO> questionnaires;
    private Set<UpdateRepresentativeDTO> representatives;

    private boolean dryRun;

    @Data
    public static class UpdateStageDTO {
        private Integer id;
        private String name;
        private BigDecimal weight;
        private Integer durationDays;
        private LocalDate applicationStartDate;
        private LocalDate applicationEndDate;
    }

    @Data
    public static class UpdateIterationDTO {
        private Integer id;
        private String name;
        private BigDecimal weight;
        private LocalDate applicationStartDate;
        private LocalDate applicationEndDate;
    }

    @Data
    public static class UpdateQuestionnaireDTO {
        private Integer id;
        private String name;
        private Integer weight;
        private String stageName;
        private String iterationName;
        private LocalDate applicationStartDate;
        private LocalDate applicationEndDate;
        private String domain;
        private String description;
        private Set<UpdateQuestionDTO> questions;
    }

    @Data
    public static class UpdateQuestionDTO {
        private Integer id;
        private String value;
        private Set<Long> roleIds;
        private List<String> stageNames;
    }

    @Data
    public static class UpdateRepresentativeDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private Set<Long> roleIds;
        private BigDecimal weight;
    }
}

