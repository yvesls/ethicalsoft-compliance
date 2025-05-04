import { Injectable } from '@angular/core'
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http'
import { Observable } from 'rxjs'
import { AuthenticationService } from '../services/authentication.service'
import { LoggerService } from '../services/logger.service'

@Injectable({
	providedIn: 'root',
})
export class TokenInterceptor implements HttpInterceptor {
	constructor(private authService: AuthenticationService) {}

	intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
		const token = this.authService.getToken()
		if (token) {
			LoggerService.info('TokenInterceptor: Token found, adding to the request headers.')
			const cloned = req.clone({
				setHeaders: { Authorization: `Bearer ${token}` },
			})
			return next.handle(cloned)
		}
		LoggerService.warn('TokenInterceptor: No token found, sending request without authorization header.')
		return next.handle(req)
	}
}
