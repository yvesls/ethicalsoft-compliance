import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

export function minDateTodayValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const dateStr = control.value;

    if (!dateStr) {
      return null;
    }

    const parts = dateStr.split('-');
    if (parts.length !== 3) {
      return { invalidDate: true };
    }

    const controlDate = new Date(
      Number(parts[0]),
      Number(parts[1]) - 1,
      Number(parts[2])
    );

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (controlDate < today) {
      return { minDateToday: true };
    }

    return null;
  };
}
