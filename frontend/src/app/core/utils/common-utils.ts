import { parseISO, format } from 'date-fns'
import { LoggerService } from '../services/logger.service'

export function dateParserSend(key: string, value: any) {
	if (value instanceof Date) {
		return format(value, "yyyy-MM-dd'T'HH:mm:ss")
	}

	if (typeof value === 'string') {
		if (value.trim() === '') {
			return null
		}

		const regexISO = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*))(?:Z|([\+|-])([\d|:]*))?$/
		const match = regexISO.exec(value)
		if (match) {
			return format(parseISO(value), "yyyy-MM-dd'T'HH:mm:ss")
		} else {
			LoggerService.warn(`dateParserSend: Invalid date format detected for value: ${value}`)
		}
	}

	return value
}

export function dateParser(key: string, value: any) {
	if (typeof value === 'string') {
		let a = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*))(?:Z|([\+|-])([\d|:]*))?$/.exec(value)
		if (a) {
			return new Date(value)
		}
		a = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})?$/.exec(value)
		if (a) {
			return new Date(value)
		}
		a = /^\/Date\((.*)\)[\/|\\]$/.exec(value)
		if (a) {
			let b = a[1].split(/[-+,.]/)
			return new Date(b[0] ? +b[0] : 0 - +b[1])
		}
		a = /^(\d{1,4})-(\d{1,2})-(\d{1,2})$/.exec(value)
		if (a) {
			const sa = value.split('-')
			return new Date(parseInt(sa[0], 10), parseInt(sa[1], 10) - 1, parseInt(sa[2], 10))
		}
	}

	LoggerService.error(`dateParser: Invalid date string or unsupported format: ${value}`)
	return value
}

export function biggest(...values: number[]): number {
	return [...values].sort((a, b) => b - a)[0]
}

export function round(value: number, scale: number = 2) {
	scale = Math.abs(scale)
	if (scale === 0) return Math.trunc(value)
	return +(Math.round(Number.parseFloat(`${value}e+${scale}`)) + `e-${scale}`)
}

export function isPropertyAssignable(obj: any, prop: string): boolean {
	return isDefined(Object.getOwnPropertyDescriptor(obj, prop))
}

export function isDefined(val: any) {
	return val !== undefined
}

export function isDefinedAndNotNull(val: any) {
	return val !== undefined && val !== null
}

export function isNumber(val: any) {
	return !isNaN(parseFloat(val)) && isFinite(val)
}

export function isNullOrEmpty(obj: any): boolean {
	return !!(!obj || Object.keys(obj).length === 0)
}

export function isPromise(value: any) {
	return value instanceof Promise
}

export function isFunction(value: any) {
	return value instanceof Function
}

export function isString(value: any): boolean {
	return typeof value === 'string' || value instanceof String
}

export function isArray(...arrays: any[]) {
	return arrays?.every((arr) => arr instanceof Array)
}

export function isObject(...objs: any[]) {
	return objs?.every((obj) => (typeof obj === 'object' || obj instanceof Object) && isDefinedAndNotNull(obj))
}

export function isArrayOrObject(val: any) {
	return isArray(val) || isObject(val)
}

export function isBlob(value: any) {
	return value instanceof Blob
}

export function deepCopy<T>(model: T): T {
	return JSON.parse(JSON.stringify(model))
}

export function hasProperties(...objs: Object[]): boolean {
	return objs?.every((obj) => !!Object.keys(obj).length)
}

export function hasSameProperties(a: Object, b: Object): boolean {
	let aKeys = Object.keys(a).sort((a, b) => a.localeCompare(b))
	let bKeys = Object.keys(b).sort((a, b) => a.localeCompare(b))
	return JSON.stringify(aKeys) === JSON.stringify(bKeys)
}

export function hasSpecificProperties(properties: string[], ...objs: Object[]): boolean {
	return objs?.every((obj) => Object.keys(obj).every((k) => properties.every((prop) => k === prop)))
}

export function countProperties(obj: Object): number {
	return Object.keys(obj).length
}

export function equalsObjects(a: any, b: any): boolean {
	return (
		a === b ||
		!(a && b) ||
		!(a?.length && b?.length) ||
		equalsByStringify(a, b) ||
		equalsArray(a, b) ||
		(isObject(a, b) &&
			hasProperties(a, b) &&
			countProperties(a) === countProperties(b) &&
			Object.keys(a).every((p) => equalsObjects(a[p], b[p])))
	)
}

export function equalsByStringify(a: any, b: any): boolean {
	return a === b || JSON.stringify(a) === JSON.stringify(b)
}

export function equalsArray<T>(a: Array<T>, b: Array<T>): boolean {
	return !(a?.length && b?.length) || (a?.length === b?.length && isArray(a, b) && a.every((el) => b.includes(el)))
}

export function copy(o: any) {
	return { ...o }
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
		return '';
	}

	return text
		.toLowerCase()
		.split(' ')
		.map(word => {
			if (word.length === 0) return '';
			return word.charAt(0).toUpperCase() + word.slice(1);
		})
		.join(' ')
		.trim();
}
