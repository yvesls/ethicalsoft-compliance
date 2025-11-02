import { HttpClient, HttpContext, HttpContextToken, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http'
import { Injectable } from '@angular/core'
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

	constructor(
		private http: HttpClient,
		private notificationService: NotificationService
	) {}

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
			throw Error('Não há arquivo para ser enviado.')
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

	private _buildBodyRequest(contentType: string, data: Object): string | Object {
		return contentType !== 'application/json' ? (data ?? '') : data ? JSON.stringify(data, dateParserSend) : ''
	}

	private _buildRequest<T>(
		type: string,
		url: string,
		headers: HttpHeaders,
		body: string | Object,
		options: RequestInputOptions
	): Observable<HttpResponse<string>> {
		options.context = (options.context ?? new HttpContext())
			.set(USE_AUTH_CONTEXT, options.useAuth || false)
			.set(USE_CACHE_CONTEXT, options.useCache || false)
			.set(USE_LOG_CONTEXT, options.useLog || false)
			.set(USE_FAKE_BACKEND_CONTEXT, options.useFakeBackend || false)
		const requestOptions: RequestCommonHttpOptions = {
			headers: headers,
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
				throw new Error('Tipo de request inválido: ' + type)
		}
	}

	private _getUrl(action: string, isBase: boolean = false, params?: UrlParameter[]): string {
		const baseUrl = isBase ? '' : (this._apiUrl ?? '')
		const query = params?.length ? '?' + params.map((p) => `${p.key}=${p.value}`).join('&') : ''
		return `${baseUrl}/${action}${query}`
	}

	private _makeFileUploadRequest<T>(url: string, options: RequestInputOptions): Observable<T> {
		if (!options.data) {
			LoggerService.error('RequestService: Não há arquivo para ser enviado')
			throw Error('Não há arquivo para ser enviado.')
		}
		const headers = options.headers ?? new HttpHeaders()
		if (options.contentType) {
			headers.set('Content-Type', options.contentType)
		}
		const body = this._buildBodyRequest(options.contentType ?? '', options.data)
		const request = this._buildRequest<T>(POST, url, headers, body, options)
		return this._mappingBody<T>(request)
	}

	private _makeRequest<T>(type: string, url: string, options: RequestInputOptions): Observable<T> {
		const contentType = options.contentType || 'application/json'
		const headers = (options.headers ?? new HttpHeaders()).set('Content-Type', contentType)
		const body = this._buildBodyRequest(contentType, options.data!)
		const request = this._buildRequest<T>(type, url, headers, body, options)
		return this._mappingBody<T>(request)
	}

	private _mappingBody<T>(request: Observable<HttpResponse<string>>): Observable<T> {
		return request.pipe(
			map((response) => {
				if (response?.body) {
					try {
						return JSON.parse(response.body)
					} catch (errorParse) {
						LoggerService.error('RequestService: Error parsing response body', errorParse)
						return response.body
					}
				}
				return null
			}),
			catchError((error) => {
				LoggerService.error('RequestService: HTTP request failed', error)
				let formattedError: any = {
					status: 0,
					errorType: 'ERROR',
					message: getErrorMessage(error.status ?? 0),
				}

				if (error?.error) {
					try {
						const parsedError = JSON.parse(error.error)
						formattedError = {
							status: parsedError.status ?? error.status ?? 0,
							errorType: parsedError.errorType ?? 'ERROR',
							message: parsedError.message ?? getErrorMessage(parsedError.status ?? 0),
						}
					} catch (e) {
						formattedError.message = error.message || getErrorMessage(error.status ?? 0)
					}
				}

				return throwError(() => formattedError)
			})
		)
	}
}

interface RequestCommonHttpOptions {
	headers?: HttpHeaders | { [header: string]: string | string[] }
	observe: 'response'
	context?: HttpContext
	params?: HttpParams | { [param: string]: string | number | boolean | ReadonlyArray<string | number | boolean> }
	reportProgress?: boolean
	responseType: 'text'
	withCredentials?: boolean
}
