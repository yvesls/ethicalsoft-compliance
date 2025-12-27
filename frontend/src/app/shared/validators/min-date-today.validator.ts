import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

export function minDateTodayValidator(message?: string): ValidatorFn {
  const defaultMessage = 'A data não pode ser anterior a hoje.';

  return (control: AbstractControl): ValidationErrors | null => {
    const dateStr = control.value;

    if (!dateStr) {
      return null;
    }

    const parts = dateStr.split('-');
    if (parts.length !== 3) {
      return { invalidDate: 'Formato de data inválido.' };
    }

    const controlDate = new Date(
      Number(parts[0]),
      Number(parts[1]) - 1,
      Number(parts[2])
    );

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (controlDate < today) {
      return {
        minDateToday: message || defaultMessage
      };
    }

    return null;
  };
}
