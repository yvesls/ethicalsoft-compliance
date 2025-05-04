import { Injectable } from '@angular/core'
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router'
import { Observable, of } from 'rxjs'
import { NavigationSourceService } from '../services/navigation-source.service'
import { NotificationService } from '../services/notification.service'
import { RouterService } from '../services/router.service'
import { LoggerService } from '../services/logger.service'

@Injectable({
	providedIn: 'root',
})
export class NavigationSourceGuard implements CanActivate {
	constructor(
		private navigationSourceService: NavigationSourceService,
		private routerService: RouterService,
		private notificationService: NotificationService
	) {}

	canActivate(
		route: ActivatedRouteSnapshot,
		state: RouterStateSnapshot
	): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
		if (this.navigationSourceService.isInternalNavigation()) {
			LoggerService.info('NavigationSourceGuard: Internal navigation detected. Access granted.')
			return true
		}

		LoggerService.warn('NavigationSourceGuard: Direct access to this route is not allowed. Redirecting to login.')
		this.notificationService.showWarning('Acesso direto a esta rota não é permitido.')
		this.routerService.navigateTo('login')

		return false
	}
}
