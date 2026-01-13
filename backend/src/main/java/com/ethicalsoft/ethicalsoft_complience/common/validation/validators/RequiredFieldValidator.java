package com.ethicalsoft.ethicalsoft_complience.common.validation.validators;

import com.ethicalsoft.ethicalsoft_complience.common.util.ObjectUtils;
import com.ethicalsoft.ethicalsoft_complience.common.validation.validators.annotations.RequiredField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequiredFieldValidator implements ConstraintValidator<RequiredField, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        return ObjectUtils.isNotNullAndNotEmpty(value);
    }
}
