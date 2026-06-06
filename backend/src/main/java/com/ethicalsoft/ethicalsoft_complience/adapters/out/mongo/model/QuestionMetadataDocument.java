package com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.model;

import com.ethicalsoft.ethicalsoft_complience.domain.isep.QuestionDomainEnum;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "question_metadata")
public class QuestionMetadataDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long questionId;

    private QuestionDomainEnum domain;

    private String theme;

    @Builder.Default
    private BigDecimal weight = BigDecimal.ONE;

    @Builder.Default
    private boolean critical = false;
}

