package com.ethicalsoft.ethicalsoft_complience.exception;


import com.ethicalsoft.ethicalsoft_complience.adapters.out.postgres.model.enums.ErrorTypeEnum;
import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class ExceptionResponseDTO {

	private LocalDateTime timestamp;
	private Integer status;
	private String error;
	private String message;
	private String path;
	private ErrorTypeEnum errorType;

	@ToString.Exclude
	private List<String> stackTrace;

	public ExceptionResponseDTO( ErrorTypeEnum errorType, @NotNull HttpStatus httpStatus, @NotNull HttpServletRequest request, @NotBlank String message, List<String> stackTrace ) {
		this.timestamp = LocalDateTime.now();
		this.status = httpStatus.value();
		this.message = message;
		this.path = request.getServletPath();
		this.error = httpStatus.name();
		this.stackTrace = stackTrace;
		this.errorType = ObjectUtils.isNullOrEmpty( errorType ) ? ErrorTypeEnum.ERROR : errorType;
	}

	@Override
	public String toString() {
		return """
				{
				  "ExceptionResponseDTO": {
				    "timestamp": "%s",
				    "status": "%s",
				    "errorType": "%s",
				    "error": "%s",
				    "message": "%s",
				    "path": "%s"
				  }
				}
				""".formatted( timestamp, status, errorType, error, message, path );
	}

}