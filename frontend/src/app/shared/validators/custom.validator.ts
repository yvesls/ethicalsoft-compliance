import { passwordMatchValidator } from "./password-match.validator";
import { passwordValidator } from "./password.validator";

export abstract class CustomValidators {
  static readonly passwordValidator = passwordValidator
  static readonly passwordMatchValidator = passwordMatchValidator
}
