import { inject, Injectable } from '@angular/core'
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router'
import { NavigationSourceService } from '../services/navigation-source.service'
import { NotificationService } from '../services/notification.service'
import { RouterService } from '../services/router.service'
import { LoggerService } from '../services/logger.service'

@Injectable({
	providedIn: 'root',
})
export class NavigationSourceGuard implements CanActivate {
	private readonly navigationSourceService = inject(NavigationSourceService)
	private readonly routerService = inject(RouterService)
	private readonly notificationService = inject(NotificationService)

	canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
		LoggerService.info('NavigationSourceGuard: Checking route access source', {
			url: state.url,
			path: route.routeConfig?.path,
		})
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
