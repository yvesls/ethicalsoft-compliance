import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms"
import { passwordRules } from "../components/rules-validators/password/password-rule.const";

export function passwordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const senha = control.value;

    if (!senha) {
      // Deixa o 'Validators.required' cuidar de valores vazios
      return null;
    }

    // 3. Itera sobre as regras, filtra as que falharam e pega suas mensagens
    const errorMessages = passwordRules
      .filter(rule => !rule.test(senha)) // Encontra todas as regras que FALHARAM
      .map(rule => rule.message);          // Pega a mensagem de erro de cada uma


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
