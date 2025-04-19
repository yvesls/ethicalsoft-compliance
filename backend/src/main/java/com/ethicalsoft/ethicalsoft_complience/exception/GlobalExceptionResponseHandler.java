package com.ethicalsoft.ethicalsoft_complience.exception;


import com.ethicalsoft.ethicalsoft_complience.model.enums.ErrorTypeEnum;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import util.ExceptionUtil;
import util.ObjectUtil;

import java.net.BindException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionResponseHandler {

	private final MessageSource messageSource;

	@ExceptionHandler( EmailSendingException.class )
	@ResponseStatus( HttpStatus.INTERNAL_SERVER_ERROR )
	public ExceptionResponseDTO handleEmailError( EmailSendingException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, exception.getMessage(), request, HttpStatus.INTERNAL_SERVER_ERROR );
	}

	@ExceptionHandler( ResourceNotFoundException.class )
	@ResponseStatus( HttpStatus.NOT_FOUND )
	public ExceptionResponseDTO handleResourceNotFound( ResourceNotFoundException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, "Resource not found.", request, HttpStatus.NOT_FOUND );
	}

	@ExceptionHandler( ResponseStatusException.class )
	public ExceptionResponseDTO handleResponseStatus( ResponseStatusException exception, HttpServletRequest request, HttpServletResponse response ) {
		var httpStatusCode = exception.getStatusCode();
		response.setStatus( httpStatusCode.value() );
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.resolve( httpStatusCode.value() ) );
	}

	@ExceptionHandler( BusinessException.class )
	@ResponseStatus( HttpStatus.BAD_REQUEST )
	public ExceptionResponseDTO handleBusiness( BusinessException exception, HttpServletRequest request ) {
		return makeDefaultResponse( exception.getTypeError(), exception, exception.getMessage(), request, HttpStatus.BAD_REQUEST );
	}

	@ExceptionHandler( AuthenticationException.class )
	@ResponseStatus( HttpStatus.UNAUTHORIZED )
	public ExceptionResponseDTO handleAccessDenied( AuthenticationException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.UNAUTHORIZED );
	}

	@ExceptionHandler( AccessDeniedException.class )
	@ResponseStatus( HttpStatus.UNAUTHORIZED )
	public ExceptionResponseDTO handleAccessDenied( AccessDeniedException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, "You don't have permission to access this resource.", request, HttpStatus.UNAUTHORIZED );
	}

	@ExceptionHandler( UserNotFoundException.class )
	@ResponseStatus( HttpStatus.BAD_REQUEST )
	public ExceptionResponseDTO handleUser( UserNotFoundException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.BAD_REQUEST );
	}

	@ExceptionHandler( AuthorizationDeniedException.class )
	@ResponseStatus( HttpStatus.FORBIDDEN )
	public ExceptionResponseDTO handleAuthorizationDenied( AuthorizationDeniedException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.FORBIDDEN );
	}

	@ExceptionHandler( { IllegalArgumentException.class, IllegalStateException.class } )
	@ResponseStatus( HttpStatus.BAD_REQUEST )
	public ExceptionResponseDTO handleIllegalArgument( RuntimeException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, exception.getMessage(), request, HttpStatus.BAD_REQUEST );
	}

	@ExceptionHandler( { HttpRequestMethodNotSupportedException.class } )
	@ResponseStatus( HttpStatus.METHOD_NOT_ALLOWED )
	public ExceptionResponseDTO handleMethodNotSupported( HttpRequestMethodNotSupportedException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.METHOD_NOT_ALLOWED );
	}

	@ExceptionHandler( BindException.class )
	@ResponseStatus( HttpStatus.BAD_REQUEST )
	public ExceptionResponseDTO handleBindValidation( BindException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.BAD_REQUEST );
	}

	@ExceptionHandler( ConstraintViolationException.class )
	@ResponseStatus( HttpStatus.BAD_REQUEST )
	public ExceptionResponseDTO handleBusiness( ConstraintViolationException exception, HttpServletRequest request ) {
		var errors = new ArrayList<>();
		for ( var constraintEx : exception.getConstraintViolations() ) {
			errors.add( constraintEx.getPropertyPath() + ": " + constraintEx.getMessage() );
		}
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, errors.toString(), request, HttpStatus.BAD_REQUEST );
	}

	@ExceptionHandler( HttpMessageNotReadableException.class )
	@ResponseStatus( HttpStatus.BAD_REQUEST )
	public ExceptionResponseDTO handleHttpMessageNotReadable( HttpMessageNotReadableException exception, HttpServletRequest request ) {
		var msg = "Error reading the request body.";
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, msg, request, HttpStatus.BAD_REQUEST );
	}

	@ExceptionHandler( { JsonMappingException.class, HttpMediaTypeNotSupportedException.class } )
	@ResponseStatus( HttpStatus.BAD_REQUEST )
	public ExceptionResponseDTO handleMappingType( Exception exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.BAD_REQUEST );
	}

	@ExceptionHandler( MethodArgumentNotValidException.class )
	@ResponseStatus( HttpStatus.UNPROCESSABLE_ENTITY )
	public ExceptionResponseDTO handleValidationError( MethodArgumentNotValidException exception, HttpServletRequest request ) {
		var errorMessage = new ArrayList<String>();

		for ( FieldError fieldError : exception.getBindingResult().getFieldErrors() ) {
			String message = Optional.of( messageSource.getMessage( fieldError, LocaleContextHolder.getLocale() ) ).map( ObjectUtil::getOrNull ).orElse( fieldError.getDefaultMessage() );
			errorMessage.add( message + fieldError.getRejectedValue() );
		}

		for ( ObjectError objError : exception.getBindingResult().getGlobalErrors() ) {
			String message = Optional.of( messageSource.getMessage( objError, LocaleContextHolder.getLocale() ) ).map( ObjectUtil::getOrNull ).orElse( objError.getDefaultMessage() );
			errorMessage.add( objError.getObjectName() + ": " + message );
		}

		String errorMsgJoined = String.join( ". ", errorMessage );
		return makeDefaultResponse( ErrorTypeEnum.INFO, exception, errorMsgJoined, request, HttpStatus.UNPROCESSABLE_ENTITY );
	}

	@ExceptionHandler( { NoSuchElementException.class, EntityNotFoundException.class, MissingPathVariableException.class } )
	@ResponseStatus( HttpStatus.NOT_FOUND )
	public ExceptionResponseDTO handleNoSuchElementFoundException( Exception exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.NOT_FOUND );
	}

	@ExceptionHandler( MissingServletRequestParameterException.class )
	@ResponseStatus( HttpStatus.BAD_REQUEST )
	public ExceptionResponseDTO missingServletRequestParameter( MissingServletRequestParameterException exception, HttpServletRequest request ) {
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, null, request, HttpStatus.BAD_REQUEST );
	}

	@ExceptionHandler( Exception.class )
	@ResponseStatus( HttpStatus.INTERNAL_SERVER_ERROR )
	public ExceptionResponseDTO handleServerError( Exception exception, HttpServletRequest request ) {
		var errorCode = UUID.randomUUID();
		var msg = "Unexpected error. Contact the system administrator with the error code. " + errorCode;
		log.error("Unexpected error: [{}] - {}", errorCode, exception.getMessage(), exception);
		return makeDefaultResponse( ErrorTypeEnum.ERROR, exception, msg, request, HttpStatus.INTERNAL_SERVER_ERROR );
	}

	private ExceptionResponseDTO makeDefaultResponse( ErrorTypeEnum typeException, Exception exception, String responseMessage, HttpServletRequest request, HttpStatus httpStatus ) {
		boolean showExceptionDetails = false;

		return new ExceptionResponseDTO( typeException, httpStatus, request, responseMessage, ExceptionUtil.getErrorStackTrace( exception, showExceptionDetails ) );
	}

}
