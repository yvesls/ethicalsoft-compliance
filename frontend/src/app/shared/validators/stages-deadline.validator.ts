import { AbstractControl, ValidationErrors, ValidatorFn, FormArray } from '@angular/forms';
import { BusinessDaysUtils } from '../../core/utils/business-days-utils';

export class StagesDeadlineValidator {
  static stagesFitWithinDeadline(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const formGroup = control;

      const deadline = formGroup.get('deadline')?.value;
      const stepsArray = formGroup.get('steps') as FormArray;

      if (!deadline || !stepsArray || stepsArray.length === 0) {
        return null;
      }

      const deadlineDate = BusinessDaysUtils.parseISODate(deadline);

      const stepsWithSequence = stepsArray.controls
        .map((control, index) => ({
          index,
          sequence: control.get('sequence')?.value || 0,
          name: control.get('name')?.value || 'Sem nome',
          applicationEndDate: control.get('applicationEndDate')?.value,
          durationDays: control.get('durationDays')?.value || 0
        }))
        .sort((a, b) => b.sequence - a.sequence);

      const violatingStages = stepsWithSequence.filter(step => {
        if (!step.applicationEndDate) return false;
        const endDate = BusinessDaysUtils.parseISODate(step.applicationEndDate);
        return endDate > deadlineDate;
      });

      if (violatingStages.length > 0) {
        const firstViolation = violatingStages[0];
        const endDate = BusinessDaysUtils.parseISODate(firstViolation.applicationEndDate);
        const daysOver = this.calculateBusinessDaysDifference(deadlineDate, endDate);

        return {
          stagesExceedDeadline: {
            violatingStages: violatingStages.map(s => s.name),
            count: violatingStages.length,
            worstCase: {
              stageName: firstViolation.name,
              sequence: firstViolation.sequence,
              endDate: firstViolation.applicationEndDate,
              daysOver: daysOver
            },
            message: `${violatingStages.length} etapa${violatingStages.length > 1 ? 's' : ''} ultrapassa${violatingStages.length === 1 ? '' : 'm'} o prazo limite do projeto. A etapa "${firstViolation.name}" (Sequência ${firstViolation.sequence}) excede em aproximadamente ${daysOver} dia${daysOver > 1 ? 's' : ''} úteis. Ajuste a data de início, estenda o prazo limite ou reduza a duração das etapas.`
          }
        };
      }

      return null;
    };
  }

  private static calculateBusinessDaysDifference(date1: Date, date2: Date): number {
    if (date2 <= date1) {
      return 0;
    }

    const nextDay = BusinessDaysUtils.addBusinessDays(date1, 1, {
      excludeWeekends: true,
      holidays: [],
    });

    return BusinessDaysUtils.calculateBusinessDays(nextDay, date2, {
      excludeWeekends: true,
      holidays: [],
    });
  }
}
