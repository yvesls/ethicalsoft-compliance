/**
 * Utilitários para cálculo de dias úteis
 * Implementa a BR08 - Previsão de data limite de resposta dos questionários
 */

export interface BusinessDaysConfig {
  excludeWeekends?: boolean;
  holidays?: Date[];
}

export class BusinessDaysUtils {
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
    config: BusinessDaysConfig = { excludeWeekends: true, holidays: [] }
  ): Date {
    const result = new Date(startDate);
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
    config: BusinessDaysConfig = { excludeWeekends: true, holidays: [] }
  ): number {
    let count = 0;
    const current = new Date(startDate);
    const holidays = config.holidays?.map(h => h.toDateString()) || [];

    while (current <= endDate) {
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
    config: BusinessDaysConfig = { excludeWeekends: true, holidays: [] }
  ): { openingDate: Date; closingDate: Date } {
    const openingOffset = Math.round(stageDurationDays * 0.1);
    const closingOffset = Math.round(stageDurationDays * 0.9);

    const openingDate = this.addBusinessDays(stageStartDate, openingOffset, config);
    const closingDate = this.addBusinessDays(stageStartDate, closingOffset, config);

    return { openingDate, closingDate };
  }

  static formatDateISO(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  static formatDateBR(date: Date | string): string {
    if (!date) return '';

    const dateObj = typeof date === 'string' ? new Date(date) : date;
    const day = String(dateObj.getDate()).padStart(2, '0');
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const year = dateObj.getFullYear();

    return `${day}/${month}/${year}`;
  }

  static parseISODate(isoDate: string): Date {
    return new Date(isoDate);
  }
}
