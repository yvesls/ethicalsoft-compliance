import { BusinessDaysUtils } from '../../core/utils/business-days-utils';

export class FormUtils {
  static formatDateBR(isoDate: string): string {
    return BusinessDaysUtils.formatDateBR(isoDate);
  }

  static formatDateISO(date: Date): string {
    return BusinessDaysUtils.formatDateISO(date);
  }

  static calculateBusinessDays(startDate: Date, endDate: Date): number {
    return BusinessDaysUtils.calculateBusinessDays(startDate, endDate, {
      excludeWeekends: true,
      holidays: [],
    });
  }

  static addBusinessDays(startDate: Date, days: number): Date {
    return BusinessDaysUtils.addBusinessDays(startDate, days, {
      excludeWeekends: true,
      holidays: [],
    });
  }
}
