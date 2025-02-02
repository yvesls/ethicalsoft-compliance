package util.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import util.ObjectUtil;
import util.validators.annotations.RequiredField;

public class RequiredFieldValidator implements ConstraintValidator<RequiredField, Object>  {

    @Override
    public boolean isValid( Object value, ConstraintValidatorContext context ) {
        return ObjectUtil.isNotNullAndNotEmpty( value );
    }

}
