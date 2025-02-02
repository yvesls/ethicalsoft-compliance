package util.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import util.ObjectUtil;
import util.validators.annotations.Email;

public class EmailValidator implements ConstraintValidator<Email, String> {

    @Override
    public boolean isValid( String value, ConstraintValidatorContext context ) {
        if( ObjectUtil.isNullOrEmpty( value ) ) {
            return false;
        }
        return value.matches( "^[^\\s]+@[^\\s]+\\.[^\\s]+$" );
    }

}