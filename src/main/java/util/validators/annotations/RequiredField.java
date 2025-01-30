package util.validators.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import util.validators.RequiredFieldValidator;

import java.lang.annotation.*;

@Documented
@Constraint( validatedBy = RequiredFieldValidator.class )
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER } )
@Retention( RetentionPolicy.RUNTIME )
public @interface RequiredField {
    String message() default "'{label}' ${NullOrEmptyMessage}";

    @NotBlank
    String label();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
