package com.ethicalsoft.ethicalsoft_complience.util.validators;

import com.ethicalsoft.ethicalsoft_complience.util.ObjectUtil;
import com.ethicalsoft.ethicalsoft_complience.util.validators.annotations.RequiredField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequiredFieldValidator implements ConstraintValidator<RequiredField, Object> {

	@Override
	public boolean isValid( Object value, ConstraintValidatorContext context ) {
		return ObjectUtil.isNotNullAndNotEmpty( value );
	}

}
