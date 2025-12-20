package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request.LinkDTO;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QuestionnaireAnswerResponseDTO {
    Long questionId;
    Boolean response;
    LinkDTO justification;
    LinkDTO evidence;
    List<LinkDTO> attachments;
}
