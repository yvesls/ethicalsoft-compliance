package util.validators.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import util.validators.RequiredFieldValidator;

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

