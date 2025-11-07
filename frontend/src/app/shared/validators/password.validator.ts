import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms"
import { passwordRules } from "../components/rules-validators/password/password-rule.const";

export function passwordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const senha = control.value;

    if (!senha) {
      return null;
    }
    const errorMessages = passwordRules
      .filter(rule => !rule.test(senha))
      .map(rule => rule.message);


    if (errorMessages.length > 0) {
      return {
        politicaSenha: `Senha inválida. Ela deve atender aos seguintes critérios: \n- ${errorMessages.join(
          '\n- '
        )}`,
      };
    }

    return null;
  };
}
