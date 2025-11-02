import { PasswordRule } from "./password-rule";

export const passwordRules: PasswordRule[] = [
  {
    message: 'Ter entre 8 e 20 caracteres.',
    test: (value) => value.length >= 8 && value.length <= 20,
  },
  {
    message: 'Não conter caracteres repetidos.',
    test: (value) => !/(.)\1+/.test(value),
  },
  {
    message: 'Possuir letras maiúsculas.',
    test: (value) => /[A-Z]/.test(value),
  },
  {
    message: 'Possuir uma letra minúscula.',
    test: (value) => /[a-z]/.test(value),
  },
  {
    message: 'Possuir um número.',
    test: (value) => /[0-9]/.test(value),
  },
  {
    message: 'Possuir um carácter especial.',
    test: (value) => /[!@#$%^&*()+=,.?":{}|<>`~\[\]\\/-]/.test(value),
  },
];
