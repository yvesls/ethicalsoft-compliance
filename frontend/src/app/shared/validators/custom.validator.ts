import { passwordMatchValidator } from "./password-match.validator";
import { passwordValidator } from "./password.validator";
import { minDateTodayValidator } from "./min-date-today.validator";
import { dateRangeValidator } from "./date-range.validator";

export abstract class CustomValidators {
  static readonly passwordValidator = passwordValidator
  static readonly passwordMatchValidator = passwordMatchValidator
  static readonly minDateToday = minDateTodayValidator;
  static readonly dateRange = dateRangeValidator;
}
