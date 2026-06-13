import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { BusinessDaysUtils } from '../../core/utils/business-days-utils';

interface StageFormValue {
  name?: string;
  applicationEndDate?: string;
  durationDays?: number | string;
}

export class ProjectDatesValidators {
  static stageApplicationRangeWithinDeadline(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const deadline = control.get('deadline')?.value;
      const steps = control.get('steps')?.value as StageFormValue[] | undefined;

      if (!deadline || !steps || !Array.isArray(steps)) {
        return null;
      }

      const deadlineDate = BusinessDaysUtils.parseISODate(deadline);

      for (const step of steps) {
        if (step.applicationEndDate) {
          const endDate = BusinessDaysUtils.parseISODate(step.applicationEndDate);

          if (endDate > deadlineDate) {
            const formattedStageEnd = BusinessDaysUtils.formatDateBR(step.applicationEndDate);
            const formattedDeadline = BusinessDaysUtils.formatDateBR(deadline);

            return {
              stageExceedsDeadline: {
                stageName: step.name,
                stageEndDate: formattedStageEnd,
                deadline: formattedDeadline,
              }
            };
          }
        }
      }

      return null;
    };
  }

  static stageWithinProjectDeadline(getProjectDeadline: () => string | null): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const applicationEndDate = control.get('applicationEndDate')?.value;
      const deadline = getProjectDeadline();

      if (!applicationEndDate || !deadline) {
        return null;
      }

      const endDate = BusinessDaysUtils.parseISODate(applicationEndDate);
      const deadlineDate = BusinessDaysUtils.parseISODate(deadline);

      if (endDate > deadlineDate) {
        const formattedEndDate = BusinessDaysUtils.formatDateBR(applicationEndDate);
        const formattedDeadline = BusinessDaysUtils.formatDateBR(deadline);

        return {
          exceedsProjectDeadline: {
            endDate: formattedEndDate,
            deadline: formattedDeadline,
          }
        };
      }

      return null;
    };
  }

  static deadlineAllowsExistingStages(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const parent = control.parent;
      if (!parent) return null;

      const deadline = control.value;
      const steps = parent.get('steps')?.value as StageFormValue[] | undefined;

      if (!deadline || !steps || !Array.isArray(steps) || steps.length === 0) {
        return null;
      }

      const deadlineDate = BusinessDaysUtils.parseISODate(deadline);
      let maxEndDate: Date | null = null;
      let conflictingStage: StageFormValue | null = null;

      for (const step of steps) {
        if (step.applicationEndDate) {
          const stepEndDate = BusinessDaysUtils.parseISODate(step.applicationEndDate);
          if (!maxEndDate || stepEndDate > maxEndDate) {
            maxEndDate = stepEndDate;
            conflictingStage = step;
          }
        }
      }

      if (
        maxEndDate &&
        maxEndDate > deadlineDate &&
        conflictingStage?.applicationEndDate
      ) {
        const formattedDeadline = BusinessDaysUtils.formatDateBR(deadline);
        const formattedStageEnd = BusinessDaysUtils.formatDateBR(conflictingStage.applicationEndDate);

        const stageName = conflictingStage.name ?? null;

        return {
          deadlineTooEarly: {
            deadline: formattedDeadline,
            latestStageEnd: formattedStageEnd,
            stageName,
          }
        };
      }

      return null;
    };
  }

  static startDateAllowsExistingStages(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const parent = control.parent;
      if (!parent) return null;

      const startDate = control.value;
      const deadline = parent.get('deadline')?.value;
      const steps = parent.get('steps')?.value as StageFormValue[] | undefined;

      if (!startDate || !deadline || !steps || !Array.isArray(steps) || steps.length === 0) {
        return null;
      }

      let totalDurationDays = 0;
      for (const step of steps) {
        totalDurationDays += Number(step.durationDays) || 0;
      }

      if (totalDurationDays === 0) {
        return null;
      }

      const startDateObj = BusinessDaysUtils.parseISODate(startDate);
      const deadlineDate = BusinessDaysUtils.parseISODate(deadline);
      const projectedEndDate = BusinessDaysUtils.addBusinessDays(startDateObj, totalDurationDays);

      if (projectedEndDate > deadlineDate) {
        const formattedStartDate = BusinessDaysUtils.formatDateBR(startDate);
        const formattedDeadline = BusinessDaysUtils.formatDateBR(deadline);
        const formattedProjectedEnd = BusinessDaysUtils.formatDateBR(projectedEndDate);

        return {
          startDateTooLate: {
            startDate: formattedStartDate,
            deadline: formattedDeadline,
            projectedEndDate: formattedProjectedEnd,
            requiredDays: totalDurationDays,
          }
        };
      }

      return null;
    };
  }
}
