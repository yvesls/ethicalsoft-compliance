import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { BusinessDaysUtils } from '../../core/utils/business-days-utils';

export class RescheduleDateValidators {

  static endDateAfterStartDate(
    startDateKey: string,
    endDateKey: string,
    message = ''
  ): ValidatorFn {
    const defaultMessage = message;

    return (control: AbstractControl): ValidationErrors | null => {
      const startControl = control.get(startDateKey);
      const endControl = control.get(endDateKey);

      if (!startControl || !endControl) {
        return null;
      }

      this.clearControlError(endControl, 'dateOrder');

      const start = startControl.value;
      const end = endControl.value;

      if (!start || !end) {
        return null;
      }

      if (end < start) {
        const errorMessage = message || defaultMessage;
        endControl.setErrors({
          ...endControl.errors,
          dateOrder: errorMessage,
        });
        return { dateOrder: errorMessage };
      }

      return null;
    };
  }

  static startDateNotBefore(
    startDateKey: string,
    getReferenceDate: () => string | null,
    getErrorMessage: (formattedDate: string) => string
  ): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const startControl = control.get(startDateKey);
      if (!startControl) {
        return null;
      }

      this.clearControlError(startControl, 'minDate');

      const start = startControl.value;
      const referenceDate = getReferenceDate();

      if (!start || !referenceDate) {
        return null;
      }

      if (start < referenceDate) {
        const formatted = BusinessDaysUtils.formatDateBR(referenceDate);
        const errorMessage = getErrorMessage(formatted);

        startControl.setErrors({
          ...startControl.errors,
          minDate: errorMessage,
        });
        return { minDate: errorMessage };
      }

      return null;
    };
  }

  private static clearControlError(control: AbstractControl, errorKey: string): void {
    if (!control.hasError(errorKey)) {
      return;
    }

    const errors = { ...control.errors };
    delete errors[errorKey];
    control.setErrors(Object.keys(errors).length > 0 ? errors : null);
  }
}
