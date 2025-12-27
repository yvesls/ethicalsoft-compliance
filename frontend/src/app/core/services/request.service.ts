import { HttpClient, HttpContext, HttpContextToken, HttpHeaders, HttpParams, HttpResponse, HttpErrorResponse } from '@angular/common/http'
import { Injectable, inject } from '@angular/core'
import { Observable, catchError, map, throwError } from 'rxjs'
import { UrlParameter } from '../interfaces/url-parameter.interface'
import { dateParserSend } from '../utils/common-utils'
import { RequestInputOptions } from '../interfaces/request-input-options.interface'
import { getErrorMessage } from '../../shared/enums/error-messages.enum'
import { LoggerService } from './logger.service'
import { NotificationService } from './notification.service'

export const USE_AUTH_CONTEXT = new HttpContextToken<boolean>(() => false)
export const USE_CACHE_CONTEXT = new HttpContextToken<boolean>(() => false)
export const USE_LOG_CONTEXT = new HttpContextToken<boolean>(() => false)
export const USE_FAKE_BACKEND_CONTEXT = new HttpContextToken<boolean>(() => false)

const GET = 'get'
const POST = 'post'
const PUT = 'put'
const PATCH = 'patch'
const DELETE = 'delete'

@Injectable({
	providedIn: 'root',
})
export class RequestService {
	private _apiUrl?: string
	private readonly http = inject(HttpClient)
	private readonly notificationService = inject(NotificationService)

	get apiUrl(): string | undefined {
		return this._apiUrl
	}

	set apiUrl(value: string | undefined) {
		this._apiUrl = value
	}

	makeDelete<T>(url: string, options: RequestInputOptions, ...params: UrlParameter[]): Observable<T> {
		return this._makeRequest(DELETE, this._getUrl(url, options.isBase, params), options)
	}

	makeFilePost<T>(url: string, options: RequestInputOptions, ...params: UrlParameter[]): Observable<T> {
		if (!options.data) {
			LoggerService.error('RequestService: No file data provided for POST request.', { url, options })
			this.notificationService.showError('No file data to send.')
			throw new Error('Não há arquivo para ser enviado.')
		}
		return this._makeFileUploadRequest<T>(this._getUrl(url, options.isBase, params), options)
	}

	makeGet<T>(url: string, options: RequestInputOptions, ...params: UrlParameter[]): Observable<T> {
		return this._makeRequest<T>(GET, this._getUrl(url, options.isBase, params), options)
	}

	makePatch<T>(url: string, options: RequestInputOptions, ...params: UrlParameter[]): Observable<T> {
		return this._makeRequest<T>(PATCH, this._getUrl(url, options.isBase, params), options)
	}

	makePost<T>(url: string, options: RequestInputOptions, ...params: UrlParameter[]): Observable<T> {
		return this._makeRequest<T>(POST, this._getUrl(url, options.isBase, params), options)
	}

	makePut<T>(url: string, options: RequestInputOptions, ...params: UrlParameter[]): Observable<T> {
		return this._makeRequest<T>(PUT, this._getUrl(url, options.isBase, params), options)
	}

	private _buildBodyRequest(contentType: string, data: unknown): unknown {
		if (contentType === 'application/json') {
			if (data === undefined || data === null) {
				return ''
			}
			if (typeof data === 'string') {
				return data
			}
			return JSON.stringify(data, dateParserSend)
		}

		return data ?? ''
	}

	private _buildRequest(
		type: string,
		url: string,
		headers: HttpHeaders,
		body: unknown,
		options: RequestInputOptions
	): Observable<HttpResponse<string>> {
		options.context = (options.context ?? new HttpContext())
			.set(USE_AUTH_CONTEXT, options.useAuth || false)
			.set(USE_CACHE_CONTEXT, options.useCache || false)
			.set(USE_LOG_CONTEXT, options.useLog || false)
			.set(USE_FAKE_BACKEND_CONTEXT, options.useFakeBackend || false)
		const requestOptions: RequestCommonHttpOptions = {
			headers,
			observe: 'response',
			responseType: 'text',
			context: options.context,
		}
		switch (type) {
			case GET:
				return this.http.get(url, requestOptions)
			case POST:
				return this.http.post(url, body, requestOptions)
			case PUT:
				return this.http.put(url, body, requestOptions)
			case PATCH:
				return this.http.patch(url, body, requestOptions)
			case DELETE:
				return this.http.delete(url, requestOptions)
			default:
				throw new Error(`Tipo de request inválido: ${type}`)
		}
	}

	private _getUrl(action: string, isBase = false, params?: UrlParameter[]): string {
		const baseUrl = isBase ? '' : this._apiUrl ?? ''
		const prefix = baseUrl ? `${baseUrl}/` : ''
		const query = params?.length
			? '?' + params.map((p) => `${encodeURIComponent(p.key)}=${encodeURIComponent(String(p.value ?? ''))}`).join('&')
			: ''
		return `${prefix}${action}${query}`
	}

	private _makeFileUploadRequest<T>(url: string, options: RequestInputOptions): Observable<T> {
		if (!options.data) {
			LoggerService.error('RequestService: Não há arquivo para ser enviado')
			throw new Error('Não há arquivo para ser enviado.')
		}
		let headers = options.headers ?? new HttpHeaders()
		if (options.contentType) {
			headers = headers.set('Content-Type', options.contentType)
		}
		const body = this._buildBodyRequest(options.contentType ?? '', options.data)
		const request = this._buildRequest(POST, url, headers, body, options)
		return this._mappingBody<T>(request)
	}

	private _makeRequest<T>(type: string, url: string, options: RequestInputOptions): Observable<T> {
		const contentType = options.contentType || 'application/json'
		const headers = (options.headers ?? new HttpHeaders()).set('Content-Type', contentType)
		const body = this._buildBodyRequest(contentType, options.data ?? null)
		const request = this._buildRequest(type, url, headers, body, options)
		return this._mappingBody<T>(request)
	}

	private _mappingBody<T>(request: Observable<HttpResponse<string>>): Observable<T> {
		return request.pipe(
			map((response) => this.parseResponseBody<T>(response.body)),
			catchError((error: unknown) => {
				LoggerService.error('RequestService: HTTP request failed', error)
				return throwError(() => this.formatHttpError(error))
			})
		)
	}

	private parseResponseBody<T>(body: string | null): T {
		if (!body) {
			return null as T
		}

		try {
			return JSON.parse(body) as T
		} catch (error) {
			LoggerService.debug('RequestService: Returning raw response body', error)
			return body as unknown as T
		}
	}

	private formatHttpError(error: unknown): ApiError {
		if (error instanceof HttpErrorResponse) {
			const fallbackMessage = getErrorMessage(error.status)
			const baseError: ApiError = {
				status: error.status,
				errorType: 'ERROR',
				message: fallbackMessage,
			}

			const serverError = this.extractServerErrorPayload(error.error)
			if (serverError) {
				return {
					status: serverError.status ?? baseError.status,
					errorType: serverError.errorType ?? baseError.errorType,
					message: this.resolveBestMessage(serverError.message, fallbackMessage, error.message),
				}
			}

			return {
				...baseError,
				message: this.resolveBestMessage(undefined, fallbackMessage, error.message),
			}
		}

		if (typeof error === 'string') {
			return {
				status: 0,
				errorType: 'ERROR',
				message: error,
			}
		}

		if (error instanceof Error) {
			return {
				status: 0,
				errorType: 'ERROR',
				message: error.message,
			}
		}

		return {
			status: 0,
			errorType: 'ERROR',
			message: getErrorMessage(0),
		}
	}

	private extractServerErrorPayload(payload: unknown): Partial<ApiError> | null {
		if (!payload) {
			return null
		}

		if (typeof payload === 'string') {
			return this.parseErrorString(payload)
		}

		if (typeof payload === 'object') {
			return this.parseErrorObject(payload as Record<string, unknown>)
		}

		return null
	}

	private parseErrorString(payload: string): Partial<ApiError> | null {
		const trimmed = payload.trim()
		if (!trimmed) {
			return null
		}

		try {
			return JSON.parse(trimmed) as Partial<ApiError>
		} catch (parseError) {
			LoggerService.warn('RequestService: Unable to parse error response JSON string', parseError)
			return { message: trimmed }
		}
	}

	private parseErrorObject(payload: Record<string, unknown>): Partial<ApiError> | null {
		const rawStatus = payload['status']
		const status = typeof rawStatus === 'number' ? rawStatus : undefined
		const errorType = this.resolveErrorType(payload)
		const rawMessage = payload['message']
		const message = typeof rawMessage === 'string' && rawMessage.trim() ? rawMessage : undefined

		if (status === undefined && !errorType && !message) {
			return null
		}

		return { status, errorType, message }
	}

	private resolveErrorType(payload: Record<string, unknown>): string | undefined {
		const rawErrorType = payload['errorType']
		if (typeof rawErrorType === 'string' && rawErrorType.trim()) {
			return rawErrorType
		}

		const rawError = payload['error']
		if (typeof rawError === 'string' && rawError.trim()) {
			return rawError
		}

		return undefined
	}

	private resolveBestMessage(serverMessage: string | undefined, fallbackMessage: string, httpMessage?: string): string {
		if (serverMessage && serverMessage.trim()) {
			return serverMessage.trim()
		}

		if (fallbackMessage) {
			return fallbackMessage
		}

		return httpMessage || getErrorMessage(0)
	}
}

interface RequestCommonHttpOptions {
	headers?: HttpHeaders | Record<string, string | string[]>
	observe: 'response'
	context?: HttpContext
	params?: HttpParams | Record<string, string | number | boolean | readonly (string | number | boolean)[]>
	reportProgress?: boolean
	responseType: 'text'
	withCredentials?: boolean
}

interface ApiError {
	status: number
	errorType: string
	message: string
}
