package com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.response;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.dto.request.LinkDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record QuestionnaireAnswerResponseDTO(Long questionId, String questionText, Boolean response,
                                             LinkDTO justification, LinkDTO evidence, List<LinkDTO> attachments) {
}
