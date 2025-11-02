package com.ethicalsoft.ethicalsoft_complience.util.validators;

import com.ethicalsoft.ethicalsoft_complience.util.ObjectUtil;
import com.ethicalsoft.ethicalsoft_complience.util.validators.annotations.Email;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<Email, String> {

	private static final Pattern EMAIL_PATTERN = Pattern.compile( "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$" );

	@Override
	public boolean isValid( String value, ConstraintValidatorContext context ) {
		if ( ObjectUtil.isNullOrEmpty( value ) ) {
			return false;
		}
		return isValidEmailFormat( value );
	}

	private boolean isValidEmailFormat( String email ) {
		return EMAIL_PATTERN.matcher( email ).matches();
	}

}