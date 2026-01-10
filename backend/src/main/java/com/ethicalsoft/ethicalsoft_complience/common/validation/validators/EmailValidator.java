package com.ethicalsoft.ethicalsoft_complience.common.validation.validators;

import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import com.ethicalsoft.ethicalsoft_complience.common.validation.validators.annotations.ValidEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

	@Override
	public boolean isValid( String value, ConstraintValidatorContext context ) {
		if ( ObjectUtils.isNullOrEmpty( value ) ) {
			return true;
		}
		return value.matches( "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$" );
	}

}