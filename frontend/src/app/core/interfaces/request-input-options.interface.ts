import { HttpContext, HttpHeaders } from '@angular/common/http'

export interface RequestInputOptions<TPayload = Record<string, unknown>> {
	data?: TPayload
	contentType?: string
	isBase?: boolean
	headers?: HttpHeaders
	useAuth?: boolean
	useCache?: boolean
	useLog?: boolean
	useFakeBackend?: boolean
	context?: HttpContext
}
