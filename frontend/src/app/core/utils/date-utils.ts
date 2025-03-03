export function addDays(days: number, date: Date = new Date()): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

export function addMonths(months: number, adjustLastMonthDay: boolean = true, date: Date = new Date()): Date {
  const result = new Date(date);
  result.setMonth(result.getMonth() + months);

  if (adjustLastMonthDay && date.getDate() !== result.getDate()) {
    result.setDate(0);
  }

  return result;
}

export function addYears(years: number, date: Date = new Date()): Date {
  const result = new Date(date);
  result.setFullYear(result.getFullYear() + years);
  return result;
}
