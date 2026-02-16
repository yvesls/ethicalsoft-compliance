/**
 * Utilitários para cálculo de dias úteis
 * Implementa a BR08 - Previsão de data limite de resposta dos questionários
 */

export interface BusinessDaysConfig {
  excludeWeekends?: boolean;
  holidays?: Date[];
}

export class BusinessDaysUtils {
  private static readonly DEFAULT_CONFIG: BusinessDaysConfig = {
    excludeWeekends: true,
    holidays: [],
  };

  private static normalizeToLocalDate(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  private static parseDateInput(date: Date | string): Date {
    if (typeof date === 'string') {
      return this.parseISODate(date);
    }

    return this.normalizeToLocalDate(date);
  }

  /**
   * Adiciona dias úteis a uma data, excluindo fins de semana e feriados
   * @param startDate Data inicial
   * @param days Número de dias úteis a adicionar
   * @param config Configuração para exclusão de fins de semana e feriados
   * @returns Nova data com os dias úteis adicionados
   */
  static addBusinessDays(
    startDate: Date,
    days: number,
    config: BusinessDaysConfig = BusinessDaysUtils.DEFAULT_CONFIG
  ): Date {
    const result = this.normalizeToLocalDate(startDate);
    let addedDays = 0;
    const holidays = config.holidays?.map(h => h.toDateString()) || [];

    while (addedDays < days) {
      result.setDate(result.getDate() + 1);

      const isWeekend = result.getDay() === 0 || result.getDay() === 6;
      const isHoliday = holidays.includes(result.toDateString());

      if ((!config.excludeWeekends || !isWeekend) && !isHoliday) {
        addedDays++;
      }
    }

    return result;
  }

  /**
   * Calcula o número de dias úteis entre duas datas
   * @param startDate Data inicial
   * @param endDate Data final
   * @param config Configuração para exclusão de fins de semana e feriados
   * @returns Número de dias úteis
   */
  static calculateBusinessDays(
    startDate: Date,
    endDate: Date,
    config: BusinessDaysConfig = BusinessDaysUtils.DEFAULT_CONFIG
  ): number {
    let count = 0;
    const current = this.normalizeToLocalDate(startDate);
    const end = this.normalizeToLocalDate(endDate);
    const holidays = config.holidays?.map(h => h.toDateString()) || [];

    while (current <= end) {
      const isWeekend = current.getDay() === 0 || current.getDay() === 6;
      const isHoliday = holidays.includes(current.toDateString());

      if ((!config.excludeWeekends || !isWeekend) && !isHoliday) {
        count++;
      }

      current.setDate(current.getDate() + 1);
    }

    return count;
  }

  /**
   * Calcula a faixa de aplicação de questionário baseado na BR08
   * - Data de abertura: 10% do esforço da fase
   * - Data limite: 90% do esforço da fase
   *
   * @param stageStartDate Data de início da etapa
   * @param stageDurationDays Duração da etapa em dias úteis
   * @param config Configuração para exclusão de fins de semana e feriados
   * @returns Objeto com as datas de abertura e fechamento
   */
  static calculateApplicationRange(
    stageStartDate: Date,
    stageDurationDays: number,
    config: BusinessDaysConfig = BusinessDaysUtils.DEFAULT_CONFIG
  ): { openingDate: Date; closingDate: Date } {
    const openingOffset = Math.max(Math.round(stageDurationDays * 0.1), 0);
    const closingOffset = Math.max(Math.round(stageDurationDays * 0.9), openingOffset);

    const openingDate = this.addBusinessDays(stageStartDate, openingOffset, config);
    const closingDate = this.addBusinessDays(stageStartDate, closingOffset, config);

    return { openingDate, closingDate };
  }

  static formatDateISO(date: Date): string {
    const dateObj = this.normalizeToLocalDate(date);
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  static formatDateBR(date: Date | string): string {
    if (!date) return '';

    const dateObj = this.parseDateInput(date);
    const day = String(dateObj.getDate()).padStart(2, '0');
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const year = dateObj.getFullYear();

    return `${day}/${month}/${year}`;
  }

  static parseISODate(isoDate: string | Date | null | undefined): Date {
    if (isoDate instanceof Date) {
      return this.normalizeToLocalDate(isoDate);
    }

    if (!isoDate || typeof isoDate !== 'string') {
      return new Date(Number.NaN);
    }

    const normalized = isoDate.trim();

    if (!normalized) {
      return new Date(Number.NaN);
    }

    const strictIsoDateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (strictIsoDateRegex.test(normalized)) {
      const [year, month, day] = normalized.split('-').map(Number);

      if (!year || !month || !day) {
        return new Date(Number.NaN);
      }

      return new Date(year, month - 1, day);
    }

    const fallbackDate = new Date(normalized);
    if (Number.isNaN(fallbackDate.getTime())) {
      return new Date(Number.NaN);
    }

    return this.normalizeToLocalDate(fallbackDate);
  }
}
