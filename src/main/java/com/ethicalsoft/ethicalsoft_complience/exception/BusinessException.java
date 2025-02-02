package com.ethicalsoft.ethicalsoft_complience.exception;

import com.ethicalsoft.ethicalsoft_complience.model.enums.ErrorTypeEnum;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Getter
    private final ErrorTypeEnum typeError;

    
    @Getter
    private final List<String> errors = new ArrayList<>();
    
    public BusinessException( List<String> errors ) {
        super();
        this.typeError = ErrorTypeEnum.ERROR;
        this.errors.addAll( errors );
    }
    
    public BusinessException( ErrorTypeEnum typeError, List<String> errors ) {
        super();
        this.typeError = typeError;
        this.errors.addAll( errors );
    }

    public BusinessException( List<String> errors, Throwable cause ) {
        super( cause );
        this.typeError = ErrorTypeEnum.ERROR;
        this.errors.addAll( errors );
    }

    public BusinessException( ErrorTypeEnum typeError, List<String> errors, Throwable cause ) {
        super( cause );
        this.typeError = typeError;
        this.errors.addAll( errors );
    }

    public BusinessException( String message ) {
        super( message );
        this.typeError = ErrorTypeEnum.ERROR;
        this.errors.add( message );
    }

    public BusinessException( ErrorTypeEnum typeError, String message ) {
        super( message );
        this.typeError = typeError;
        this.errors.add( message );
    }

    public BusinessException( Throwable cause ) {
        super( cause );
        this.typeError = ErrorTypeEnum.ERROR;
    }

    public BusinessException( ErrorTypeEnum typeError, Throwable cause ) {
        super( cause );
        this.typeError = typeError;
    }

    public BusinessException( String message, Throwable cause ) {
        super( message, cause );
        this.typeError = ErrorTypeEnum.ERROR;
    }

    public BusinessException( ErrorTypeEnum typeError, String message, Throwable cause ) {
        super( message, cause );
        this.typeError = typeError;
    }

    @Override
    public String getMessage() {
        if( errors.isEmpty() ) {
            errors.add( super.getMessage() );
        }
        return errors.stream()
                .distinct()
                .filter( Objects::nonNull )
                .collect( Collectors.joining( ". " ) );
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

}