package util.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import util.ObjectUtil;
import util.validators.annotations.Email;

import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<Email, String> {

	private static final Pattern EMAIL_PATTERN = Pattern.compile( "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$" );

	@Override
	public boolean isValid( String value, ConstraintValidatorContext context ) {
		if ( ObjectUtil.isNullOrEmpty( value ) ) {
			return false;
		}
		return EMAIL_PATTERN.matcher( value ).matches();
	}

}