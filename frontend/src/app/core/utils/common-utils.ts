
import { parseISO, format } from 'date-fns'
import { LoggerService } from '../services/logger.service'

type PlainObject = Record<string, unknown>

const DATE_TIME_BASE = String.raw`\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}`
const FRACTIONAL_SEGMENT = String.raw`(?:\.\d+)?`
const OFFSET_SEGMENT = String.raw`(?:Z|[+-]\d{2}:?\d{2})`
const ISO_DATE_TIME_REGEX = new RegExp(`^${DATE_TIME_BASE}${FRACTIONAL_SEGMENT}(?:${OFFSET_SEGMENT})?$`)
const ISO_DATE_REGEX = new RegExp(`^${DATE_TIME_BASE}$`)
const SLASH_DATE_REGEX = /\/Date\((.*)\)[/\\]$/
const SIMPLE_DATE_REGEX = /^(\d{1,4})-(\d{1,2})-(\d{1,2})$/

export function dateParserSend(_key: string, value: unknown): unknown {
	if (value instanceof Date) {
		return format(value, "yyyy-MM-dd'T'HH:mm:ss")
	}

	if (typeof value === 'string') {
		if (value.trim() === '') {
			return null
		}

		if (ISO_DATE_TIME_REGEX.test(value)) {
			return format(parseISO(value), "yyyy-MM-dd'T'HH:mm:ss")
		} else {
			LoggerService.warn(`dateParserSend: Invalid date format detected for value: ${value}`)
		}
	}

	return value
}

export function dateParser(_key: string, value: unknown): unknown {
	if (typeof value === 'string') {
		if (ISO_DATE_TIME_REGEX.test(value) || ISO_DATE_REGEX.test(value)) {
			return new Date(value)
		}
		const slashMatch = SLASH_DATE_REGEX.exec(value)
		if (slashMatch) {
			const parts = slashMatch[1].split(/[-+,.]/)
			const base = parts[0] ? Number(parts[0]) : 0
			const offset = parts[1] ? Number(parts[1]) : 0
			return new Date(base - offset)
		}
		const simpleMatch = SIMPLE_DATE_REGEX.exec(value)
		if (simpleMatch) {
			const [year, month, day] = simpleMatch.slice(1).map((segment) => Number.parseInt(segment, 10))
			return new Date(year, month - 1, day)
		}
	}

	LoggerService.error(`dateParser: Invalid date string or unsupported format: ${value}`)
	return value
}

export function biggest(...values: number[]): number {
	return Math.max(...values)
}

export function round(value: number, scale = 2): number {
	const normalizedScale = Math.abs(scale)
	if (normalizedScale === 0) {
		return Math.trunc(value)
	}
	return +(Math.round(Number.parseFloat(`${value}e+${normalizedScale}`)) + `e-${normalizedScale}`)
}

export function isPropertyAssignable(obj: PlainObject, prop: string): boolean {
	return isDefined(Object.getOwnPropertyDescriptor(obj, prop))
}

export function isDefined<T>(val: T | undefined): val is T {
	return val !== undefined
}

export function isDefinedAndNotNull<T>(val: T | null | undefined): val is T {
	return val !== undefined && val !== null
}

export function isNumber(val: unknown): val is number {
	return typeof val === 'number' && Number.isFinite(val)
}

export function isNullOrEmpty(obj: PlainObject | null | undefined): boolean {
	return !obj || Object.keys(obj).length === 0
}

export function isPromise<T = unknown>(value: unknown): value is Promise<T> {
	return value instanceof Promise
}

export function isFunction(value: unknown): value is (...args: unknown[]) => unknown {
	return typeof value === 'function'
}

export function isString(value: unknown): value is string {
	return typeof value === 'string'
}

export function isArray(...arrays: unknown[]): boolean {
	return arrays.every((arr) => Array.isArray(arr))
}

export function isObject(...objs: unknown[]): boolean {
	return objs.every((obj) => typeof obj === 'object' && obj !== null && !Array.isArray(obj))
}

export function isArrayOrObject(val: unknown): boolean {
	return isArray(val) || isObject(val)
}

export function isBlob(value: unknown): value is Blob {
	return value instanceof Blob
}

export function deepCopy<T>(model: T): T {
	return structuredClone(model)
}

export function hasProperties(...objs: PlainObject[]): boolean {
	return objs.every((obj) => Object.keys(obj).length > 0)
}

export function hasSameProperties(a: PlainObject, b: PlainObject): boolean {
	const aKeys = Object.keys(a).sort((first, second) => first.localeCompare(second))
	const bKeys = Object.keys(b).sort((first, second) => first.localeCompare(second))
	return JSON.stringify(aKeys) === JSON.stringify(bKeys)
}

export function hasSpecificProperties(properties: readonly string[], ...objs: PlainObject[]): boolean {
	return objs.every((obj) => Object.keys(obj).every((key) => properties.includes(key)))
}

export function countProperties(obj: PlainObject): number {
	return Object.keys(obj).length
}

export function equalsObjects(a: unknown, b: unknown): boolean {
	if (a === b) {
		return true
	}

	if (!isDefinedAndNotNull(a) || !isDefinedAndNotNull(b)) {
		return false
	}

	if (Array.isArray(a) && Array.isArray(b)) {
		return equalsArray(a, b)
	}

	if (isPlainObject(a) && isPlainObject(b)) {
		if (countProperties(a) !== countProperties(b)) {
			return false
		}
		return Object.keys(a).every((key) => equalsObjects(a[key], b[key]))
	}

	return equalsByStringify(a, b)
}

export function equalsByStringify<T>(a: T, b: T): boolean {
	return a === b || JSON.stringify(a) === JSON.stringify(b)
}

export function equalsArray<T>(a: readonly T[] | null | undefined, b: readonly T[] | null | undefined): boolean {
	if (!a || !b) {
		return false
	}
	return a.length === b.length && a.every((el) => b.includes(el))
}

export function copy<T extends PlainObject>(obj: T): T {
	return { ...obj }
}

/**
 * Capitaliza a primeira letra de cada palavra em uma string
 * @param text - Texto a ser capitalizado
 * @returns Texto com a primeira letra de cada palavra em maiúscula
 * @example
 * capitalizeWords('joão da silva') // returns 'João Da Silva'
 * capitalizeWords('MARIA SANTOS') // returns 'Maria Santos'
 */
export function capitalizeWords(text: string | null | undefined): string {
	if (!text || typeof text !== 'string') {
		return ''
	}

	return text
		.toLowerCase()
		.split(' ')
		.map((word) => {
			if (word.length === 0) return ''
			return word.charAt(0).toUpperCase() + word.slice(1)
		})
		.join(' ')
		.trim()
}

function isPlainObject(value: unknown): value is PlainObject {
	return typeof value === 'object' && value !== null && !Array.isArray(value)
}
