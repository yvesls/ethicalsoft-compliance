package com.ethicalsoft.ethicalsoft_complience.mongo.model;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "questionnaire_responses")
public class QuestionnaireResponseDocument {

    @Id
    private String id;

    @Field("project_id")
    private Long projectId;

    @Field("questionnaire_id")
    private Integer questionnaireId;

    @Field("representative_id")
    private Long representativeId;

    @Field("stage_id")
    private Integer stageId;

    @Field("submission_date")
    private LocalDateTime submissionDate;

    private QuestionnaireResponseStatus status;

    private List<AnswerDocument> answers = new ArrayList<>();

    private List<AttachmentDocument> attachments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class AnswerDocument {
        @Field("question_id")
        private Long questionId;
        private String questionText;
        @Field("stage_id")
        private Integer stageId;
        private List<Long> roleIds = new ArrayList<>();
        private Object response;
        private String justification;
    }

    @Data
    @NoArgsConstructor
    public static class AttachmentDocument {
        private String fileName;
        private String url;
    }
}
