package com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.request;

import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.IterationDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.QuestionnaireDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.RepresentativeDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.dto.StageDTO;
import com.ethicalsoft.ethicalsoft_complience.postgres.model.enums.ProjectStatusEnum;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class ProjectCreationRequestDTO {
	private String name;
    @NotEmpty
	private String type;
	private LocalDate startDate;
	private LocalDate deadline;
	private Integer templateId;
	private ProjectStatusEnum status;

	private Integer iterationDuration;
	private Integer iterationCount;

	private List<StageDTO> stages;
	private Set<QuestionnaireDTO> questionnaires;
	private Set<RepresentativeDTO> representatives;

	private Set<IterationDTO> iterations;
}