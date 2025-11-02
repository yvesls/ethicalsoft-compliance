package com.ethicalsoft.ethicalsoft_complience.util.validators.annotations;

import com.ethicalsoft.ethicalsoft_complience.util.validators.RequiredFieldValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint( validatedBy = RequiredFieldValidator.class )
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER } )
@Retention( RetentionPolicy.RUNTIME )
public @interface RequiredField {
	String message() default "'{label}' {NullOrEmptyMessage} Please provide a valid value. Value: ";

	String label();

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}

