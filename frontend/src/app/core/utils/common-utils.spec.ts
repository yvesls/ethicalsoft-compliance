import { capitalizeWords } from './common-utils';

describe('capitalizeWords', () => {
  it('should capitalize first letter of each word', () => {
    expect(capitalizeWords('joão da silva')).toBe('João Da Silva');
    expect(capitalizeWords('maria santos')).toBe('Maria Santos');
    expect(capitalizeWords('ana paula costa')).toBe('Ana Paula Costa');
  });

  it('should handle uppercase input', () => {
    expect(capitalizeWords('MARIA SANTOS')).toBe('Maria Santos');
    expect(capitalizeWords('JOÃO PEDRO')).toBe('João Pedro');
  });

  it('should handle mixed case input', () => {
    expect(capitalizeWords('MaRiA sAnToS')).toBe('Maria Santos');
    expect(capitalizeWords('jOãO dA sIlVa')).toBe('João Da Silva');
  });

  it('should handle single word', () => {
    expect(capitalizeWords('maria')).toBe('Maria');
    expect(capitalizeWords('JOÃO')).toBe('João');
    expect(capitalizeWords('aNa')).toBe('Ana');
  });

  it('should handle multiple spaces', () => {
    expect(capitalizeWords('maria  santos')).toBe('Maria  Santos');
    expect(capitalizeWords('joão   da   silva')).toBe('João   Da   Silva');
  });

  it('should handle leading and trailing spaces', () => {
    expect(capitalizeWords('  maria santos  ')).toBe('Maria Santos');
    expect(capitalizeWords(' joão ')).toBe('João');
  });

  it('should handle empty string', () => {
    expect(capitalizeWords('')).toBe('');
    expect(capitalizeWords('   ')).toBe('');
  });

  it('should handle null and undefined', () => {
    expect(capitalizeWords(null)).toBe('');
    expect(capitalizeWords(undefined)).toBe('');
  });

  it('should handle special characters', () => {
    expect(capitalizeWords('maría josé')).toBe('María José');
    expect(capitalizeWords("o'brien")).toBe("O'brien");
    expect(capitalizeWords('joão-pedro')).toBe('João-pedro');
  });

  it('should handle single letter words', () => {
    expect(capitalizeWords('a b c')).toBe('A B C');
    expect(capitalizeWords('maria e silva')).toBe('Maria E Silva');
  });

  it('should preserve accented characters', () => {
    expect(capitalizeWords('josé maria')).toBe('José Maria');
    expect(capitalizeWords('françois')).toBe('François');
    expect(capitalizeWords('münchen')).toBe('München');
  });
});
