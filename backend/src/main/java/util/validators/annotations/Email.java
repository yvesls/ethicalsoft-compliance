package util.validators.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import util.validators.EmailValidator;

import java.lang.annotation.*;

@Documented
@Constraint( validatedBy = EmailValidator.class )
@Target( { ElementType.METHOD, ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface Email {

	String message() default "{EmailInvalidMessage}. Value: ";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
