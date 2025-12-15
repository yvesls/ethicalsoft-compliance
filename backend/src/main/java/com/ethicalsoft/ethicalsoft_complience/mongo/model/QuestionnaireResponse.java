package com.ethicalsoft.ethicalsoft_complience.mongo.model;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.QuestionnaireResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "questionnaire_responses")
public class QuestionnaireResponse {
	@Id
	private String id;
	private Long projectId;
	private Integer questionnaireId;
	private Long representativeId;
	private Integer stageId;
	private QuestionnaireResponseStatus status;
	private LocalDateTime submissionDate;
	private List<AnswerDocument> answers;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AnswerDocument {
		private Long questionId;
		private String questionText;
		private List<Integer> stageIds;
		private List<Long> roleIds;
		private Boolean response;
		private String justification;
		private List<String> attachments;
	}
}
