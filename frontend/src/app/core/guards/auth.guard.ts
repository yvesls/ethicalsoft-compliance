import { Injectable } from '@angular/core'
import {
	CanActivate,
	CanActivateChild,
	CanMatch,
	ActivatedRouteSnapshot,
	RouterStateSnapshot,
	Route,
	UrlSegment,
	UrlTree,
	Router,
} from '@angular/router'
import { Observable, of } from 'rxjs'
import { catchError, map, switchMap } from 'rxjs/operators'
import { AuthenticationService } from '../services/authentication.service'
import { NotificationService } from '../services/notification.service'
import { LoggerService } from '../services/logger.service'

@Injectable({
	providedIn: 'root',
})
export class AuthGuard implements CanActivate, CanActivateChild, CanMatch {
	constructor(
		private authService: AuthenticationService,
		private router: Router,
		private notificationService: NotificationService
	) {}

	canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
		return this.verifyAccess(route)
	}

	canActivateChild(childRoute: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
		return this.verifyAccess(childRoute)
	}

	canLoad(route: Route, segments: UrlSegment[]): Observable<boolean | UrlTree> {
		return this.verifyAccess(route)
	}

	canMatch(route: Route, segments: UrlSegment[]): Observable<boolean | UrlTree> {
		return this.verifyAccess(route)
	}

	private verifyAccess(route: ActivatedRouteSnapshot | Route): Observable<boolean | UrlTree> {
		return this.authService.isAuthenticated$().pipe(
			switchMap((isAuth) => {
				if (!isAuth) {
					LoggerService.warn('AuthGuard: User not authenticated. Redirecting to login.')
					return of(this.redirectToLogin())
				}

				const requiredRoles = route.data?.['roles'] || []
				return this.authService.userRoles$.pipe(
					map((userRoles: string | any[]) => {
						const hasRole = requiredRoles.some((role: string) => userRoles.includes(role))
						if (!hasRole) {
							LoggerService.warn('AuthGuard: User does not have required role. Redirecting to login.')

							this.notificationService.showWarning("You don't have permission to access this resource.")
							return this.router.createUrlTree(['/login'])
						}
						return true
					}),
					catchError((error) => {
						LoggerService.error('AuthGuard: Error verifying access.', error)

						return of(this.redirectToLogin())
					})
				)
			}),
			catchError((error) => {
				LoggerService.error('AuthGuard: Unexpected error during access verification.', error)

				return of(this.redirectToLogin())
			})
		)
	}

	private redirectToLogin(): UrlTree {
		return this.router.createUrlTree(['/login'])
	}
}
