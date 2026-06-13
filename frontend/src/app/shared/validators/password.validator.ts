import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms"
import { passwordRules } from "../components/rules-validators/password/password-rule.const";

export function passwordValidator(t: (key: string) => string = (k) => k): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const senha = control.value;

    if (!senha) {
      return null;
    }
    const errorMessages = passwordRules
      .filter(rule => !rule.test(senha))
      .map(rule => t(rule.message));

    if (errorMessages.length > 0) {
      return {
        politicaSenha: `${t('auth.password_rules.invalid_prefix')}\n- ${errorMessages.join('\n- ')}`,
      };
    }

    return null;
  };
}
