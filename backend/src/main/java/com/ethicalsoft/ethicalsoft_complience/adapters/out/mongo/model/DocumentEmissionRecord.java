package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "document_emission_records")
public class DocumentEmissionRecord {

    @Id
    private String id;

    @Indexed(unique = true)
    private String authenticityCode;

    private String documentType;

    private Long projectId;

    private String projectName;

    private Integer questionnaireId;

    private String questionnaireName;

    private Integer stageId;

    private String stageName;

    private Integer iterationId;

    private String iterationName;

    private String scopeLabel;

    private LocalDateTime emittedAt;

    private Long emittedByUserId;

    private String emittedByName;

    private String dataHash;

    private BigDecimal isepPercent;

    private String band;

    private String templateVersion;
}
