import { inject, Injectable } from '@angular/core'
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
	private readonly authService = inject(AuthenticationService)
	private readonly router = inject(Router)
	private readonly notificationService = inject(NotificationService)

	canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
		LoggerService.info('AuthGuard: Processing canActivate', { url: state.url })
		return this.verifyAccess(route, state.url)
	}

	canActivateChild(childRoute: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
		LoggerService.info('AuthGuard: Processing canActivateChild', { url: state.url })
		return this.verifyAccess(childRoute, state.url)
	}

	canMatch(route: Route, segments: UrlSegment[]): Observable<boolean | UrlTree> {
		LoggerService.info('AuthGuard: Processing canMatch', {
			segments: segments.map((segment) => segment.path),
		})
		return this.verifyAccess(route, undefined, segments)
	}

	private verifyAccess(
		route: ActivatedRouteSnapshot | Route,
		stateUrl?: string,
		segments?: UrlSegment[]
	): Observable<boolean | UrlTree> {
		return this.authService.isAuthenticated$().pipe(
			switchMap((isAuth) => {
				if (!isAuth) {
					LoggerService.warn('AuthGuard: User not authenticated. Redirecting to login.')
					return of(this.redirectToLogin())
				}

				const requiredRoles: string[] = (route.data?.['roles'] as string[]) ?? []
				return this.authService.userRoles$.pipe(
					map((userRoles: string[]) => {
						const hasRole = !requiredRoles.length || requiredRoles.some((role: string) => userRoles.includes(role))
						if (!hasRole) {
							LoggerService.warn('AuthGuard: User does not have required role. Redirecting to login.')

							this.notificationService.showWarning("You don't have permission to access this resource.")
							return this.router.createUrlTree(['/login'])
						}

						const isFirstAccessPending = this.authService.isFirstAccessPending()
						if (isFirstAccessPending && !this.isRouteAllowedDuringFirstAccess(stateUrl, route, segments)) {
							LoggerService.info('AuthGuard: Redirecting user with first access pending to reset password route.')
							return this.router.createUrlTree(['/settings/reset-password'])
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

	private isRouteAllowedDuringFirstAccess(
		stateUrl: string | undefined,
		route: ActivatedRouteSnapshot | Route,
		segments?: UrlSegment[]
	): boolean {
		const currentPath = this.resolveCurrentPath(stateUrl, route, segments)
		return currentPath.startsWith('/settings/reset-password')
	}

	private resolveCurrentPath(
		stateUrl: string | undefined,
		route: ActivatedRouteSnapshot | Route,
		segments?: UrlSegment[]
	): string {
		if (stateUrl) {
			return stateUrl
		}
		if (segments?.length) {
			return `/${segments.map((segment) => segment.path).join('/')}`
		}
		const routePath = this.extractRoutePath(route)
		return routePath ? `/${routePath}` : '/'
	}

	private extractRoutePath(route: ActivatedRouteSnapshot | Route): string {
		if ('routeConfig' in route) {
			return route.routeConfig?.path ?? ''
		}
		return route.path ?? ''
	}
}
