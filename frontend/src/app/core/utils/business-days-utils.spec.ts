import { BusinessDaysUtils } from './business-days-utils';

describe('BusinessDaysUtils', () => {
  describe('addBusinessDays', () => {
    it('deve adicionar dias úteis corretamente, excluindo fins de semana', () => {
      const startDate = new Date(2023, 5, 15);

      const result = BusinessDaysUtils.addBusinessDays(startDate, 3);

      expect(result.getDate()).toBe(21);
      expect(result.getMonth()).toBe(5);
      expect(result.getDay()).toBe(3);
    });

    it('deve excluir feriados configurados', () => {
      const startDate = new Date(2023, 5, 15);
      const holidays = [new Date(2023, 5, 19)];

      const result = BusinessDaysUtils.addBusinessDays(startDate, 3, {
        excludeWeekends: true,
        holidays
      });

      expect(result.getDate()).toBe(22);
    });
  });

  describe('calculateBusinessDays', () => {
    it('deve calcular corretamente dias úteis entre duas datas', () => {
      const startDate = new Date(2023, 5, 15);
      const endDate = new Date(2023, 5, 23);

      const result = BusinessDaysUtils.calculateBusinessDays(startDate, endDate);

      expect(result).toBe(7);
    });

    it('deve excluir fins de semana do cálculo', () => {
      const startDate = new Date(2023, 5, 19);
      const endDate = new Date(2023, 5, 25);

      const result = BusinessDaysUtils.calculateBusinessDays(startDate, endDate);

      expect(result).toBe(5);
    });
  });

  describe('calculateApplicationRange', () => {
    it('deve calcular faixa de aplicação baseado na BR08', () => {
      const stageStartDate = new Date(2023, 5, 15);
      const stageDurationDays = 5;

      const result = BusinessDaysUtils.calculateApplicationRange(
        stageStartDate,
        stageDurationDays
      );

      expect(result.openingDate).toBeDefined();
      expect(result.closingDate).toBeDefined();

      expect(result.openingDate.getTime()).toBeGreaterThan(stageStartDate.getTime());

      expect(result.closingDate.getTime()).toBeGreaterThan(result.openingDate.getTime());
    });
  });

  describe('formatDateBR', () => {
    it('deve formatar data no padrão brasileiro DD/MM/YYYY', () => {
      const date = new Date(2023, 5, 15);
      const result = BusinessDaysUtils.formatDateBR(date);

      expect(result).toBe('15/06/2023');
    });

    it('deve formatar string ISO no padrão brasileiro', () => {
      const isoDate = '2023-06-15';
      const result = BusinessDaysUtils.formatDateBR(isoDate);

      expect(result).toBe('15/06/2023');
    });
  });

  describe('formatDateISO', () => {
    it('deve formatar data no padrão ISO YYYY-MM-DD', () => {
      const date = new Date(2023, 5, 15);
      const result = BusinessDaysUtils.formatDateISO(date);

      expect(result).toBe('2023-06-15');
    });
  });

  describe('BR08 - Exemplo Completo', () => {
    it('deve calcular corretamente o exemplo da BR08', () => {
      const projectStart = new Date(2023, 5, 15);

      const iniciacao = {
        durationDays: 5,
        weight: 4
      };

      const rangeIniciacao = BusinessDaysUtils.calculateApplicationRange(
        projectStart,
        iniciacao.durationDays
      );

      expect(rangeIniciacao.openingDate).toBeDefined();
      expect(rangeIniciacao.closingDate).toBeDefined();

      const openingOffset = BusinessDaysUtils.calculateBusinessDays(
        projectStart,
        rangeIniciacao.openingDate
      );
      expect(openingOffset).toBeGreaterThanOrEqual(0);
      expect(openingOffset).toBeLessThanOrEqual(2);

      const closingOffset = BusinessDaysUtils.calculateBusinessDays(
        projectStart,
        rangeIniciacao.closingDate
      );
      expect(closingOffset).toBeGreaterThanOrEqual(4);
      expect(closingOffset).toBeLessThanOrEqual(6);
    });
  });
});
