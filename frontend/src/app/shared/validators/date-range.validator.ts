import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

/**
 * Validador de grupo para garantir que a data de início
 * não seja posterior à data limite (deadline).
 *
 * @param startDateKey
 * @param deadlineKey
 */
export function dateRangeValidator(
  startDateKey: string,
  deadlineKey: string,
  message?: string
): ValidatorFn {
  const defaultMessage = 'A data de início não pode ser posterior ao prazo limite.';

  return (control: AbstractControl): ValidationErrors | null => {
    const startDateControl = control.get(startDateKey);
    const deadlineControl = control.get(deadlineKey);

    if (!startDateControl || !deadlineControl) {
      return null;
    }

    if (startDateControl.hasError('dateOrder')) {
  const errors = startDateControl.errors ? { ...startDateControl.errors } : {};
  delete errors['dateOrder'];
  startDateControl.setErrors(Object.keys(errors).length > 0 ? errors : null);
    }

    const startDateStr = startDateControl.value;
    const deadlineStr = deadlineControl.value;

    if (!startDateStr || !deadlineStr) {
      return null;
    }

    const startParts = startDateStr.split('-');
    const endParts = deadlineStr.split('-');
    if (startParts.length !== 3 || endParts.length !== 3) {
      return null;
    }

    const startDate = new Date(Number(startParts[0]), Number(startParts[1]) - 1, Number(startParts[2]));
    const deadlineDate = new Date(Number(endParts[0]), Number(endParts[1]) - 1, Number(endParts[2]));

    if (startDate > deadlineDate) {
      const errorMessage = message || defaultMessage;
      startDateControl.setErrors({
        ...startDateControl.errors,
        dateOrder: errorMessage
      });
      return {
        dateOrder: errorMessage
      } satisfies ValidationErrors;
    }

    return null;
  };
}
