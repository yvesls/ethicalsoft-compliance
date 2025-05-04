import { LoggerService } from '../services/logger.service'

export function addDays(days: number, date: Date = new Date()): Date {
	try {
		const result = new Date(date)
		result.setDate(result.getDate() + days)

		if (isNaN(result.getTime())) {
			LoggerService.warn('addDays: Invalid date after adding days.', { days, date })
		}

		return result
	} catch (error) {
		LoggerService.error('addDays: Error while adding days.', error)
		throw error
	}
}

export function addMonths(months: number, adjustLastMonthDay: boolean = true, date: Date = new Date()): Date {
	try {
		const result = new Date(date)
		result.setMonth(result.getMonth() + months)

		if (adjustLastMonthDay && date.getDate() !== result.getDate()) {
			result.setDate(0)
		}

		if (isNaN(result.getTime())) {
			LoggerService.warn('addMonths: Invalid date after adding months.', { months, date })
		}

		return result
	} catch (error) {
		LoggerService.error('addMonths: Error while adding months.', error)
		throw error
	}
}

export function addYears(years: number, date: Date = new Date()): Date {
	try {
		const result = new Date(date)
		result.setFullYear(result.getFullYear() + years)

		if (isNaN(result.getTime())) {
			LoggerService.warn('addYears: Invalid date after adding years.', { years, date })
		}

		return result
	} catch (error) {
		LoggerService.error('addYears: Error while adding years.', error)
		throw error
	}
}
