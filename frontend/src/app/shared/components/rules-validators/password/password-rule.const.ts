import { PasswordRule } from './password-rule'

export const passwordRules: PasswordRule[] = [
  {
    message: 'auth.password_rules.length',
    test: (value) => value.length >= 8 && value.length <= 20,
  },
  {
    message: 'auth.password_rules.no_repeated',
    test: (value) => !/(.)\1+/.test(value),
  },
  {
    message: 'auth.password_rules.uppercase',
    test: (value) => /[A-Z]/.test(value),
  },
  {
    message: 'auth.password_rules.lowercase',
    test: (value) => /[a-z]/.test(value),
  },
  {
    message: 'auth.password_rules.number',
    test: (value) => /\d/.test(value),
  },
  {
    message: 'auth.password_rules.special_char',
    test: (value) => /[^A-Za-z0-9]/.test(value),
  },
]
