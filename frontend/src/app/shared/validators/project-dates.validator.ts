import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { BusinessDaysUtils } from '../../core/utils/business-days-utils';

export class ProjectDatesValidators {
  static stageApplicationRangeWithinDeadline(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const parent = control.parent;
      if (!parent) return null;

      const deadline = parent.get('deadline')?.value;
      const steps = parent.get('steps')?.value;

      if (!deadline || !steps || !Array.isArray(steps)) {
        return null;
      }

      for (const step of steps) {
        if (step.applicationEndDate) {
          const endDate = new Date(step.applicationEndDate);
          const deadlineDate = new Date(deadline);

          if (endDate > deadlineDate) {
            const formattedStageEnd = BusinessDaysUtils.formatDateBR(step.applicationEndDate);
            const formattedDeadline = BusinessDaysUtils.formatDateBR(deadline);

            return {
              stageExceedsDeadline: {
                stageName: step.name,
                stageEndDate: formattedStageEnd,
                deadline: formattedDeadline,
                message: `A etapa "${step.name}" tem data de término (${formattedStageEnd}) que ultrapassa o prazo limite do projeto (${formattedDeadline}). Ajuste o prazo limite do projeto ou reduza o peso desta etapa.`
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

      const endDate = new Date(applicationEndDate);
      const deadlineDate = new Date(deadline);

      if (endDate > deadlineDate) {
        const formattedEndDate = BusinessDaysUtils.formatDateBR(applicationEndDate);
        const formattedDeadline = BusinessDaysUtils.formatDateBR(deadline);

        return {
          exceedsProjectDeadline: {
            endDate: formattedEndDate,
            deadline: formattedDeadline,
            message: `A faixa de aplicação desta etapa ultrapassa o prazo limite do projeto (${formattedDeadline}). A etapa terminaria em ${formattedEndDate}.`
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
      const steps = parent.get('steps')?.value;

      if (!deadline || !steps || !Array.isArray(steps) || steps.length === 0) {
        return null;
      }

      const deadlineDate = new Date(deadline);
      let maxEndDate: Date | null = null;
      let conflictingStage: any = null;

      for (const step of steps) {
        if (step.applicationEndDate) {
          const stepEndDate = new Date(step.applicationEndDate);
          if (!maxEndDate || stepEndDate > maxEndDate) {
            maxEndDate = stepEndDate;
            conflictingStage = step;
          }
        }
      }

      if (maxEndDate && maxEndDate > deadlineDate) {
        const formattedDeadline = BusinessDaysUtils.formatDateBR(deadline);
        const formattedStageEnd = BusinessDaysUtils.formatDateBR(conflictingStage.applicationEndDate);

        return {
          deadlineTooEarly: {
            deadline: formattedDeadline,
            latestStageEnd: formattedStageEnd,
            stageName: conflictingStage.name,
            message: `O prazo limite (${formattedDeadline}) não permite acomodar a etapa "${conflictingStage.name}" que termina em ${formattedStageEnd}. Por favor, estenda o prazo limite ou ajuste os pesos das etapas.`
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
      const steps = parent.get('steps')?.value;

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

      const startDateObj = new Date(startDate);
      const deadlineDate = new Date(deadline);
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
            message: `Com a data de início atual (${formattedStartDate}), as etapas terminariam em ${formattedProjectedEnd}, ultrapassando o prazo limite (${formattedDeadline}). As etapas requerem ${totalDurationDays} dias úteis. Por favor, antecipe a data de início ou ajuste os pesos das etapas.`
          }
        };
      }

      return null;
    };
  }
}
