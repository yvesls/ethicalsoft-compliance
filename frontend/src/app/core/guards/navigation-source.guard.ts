import { inject, Injectable } from '@angular/core'
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router'
import { NavigationSourceService } from '../services/navigation-source.service'
import { NotificationService } from '../services/notification.service'
import { TranslateService } from '@ngx-translate/core'
import { RouterService } from '../services/router.service'
import { LoggerService } from '../services/logger.service'

@Injectable({
	providedIn: 'root',
})
export class NavigationSourceGuard implements CanActivate {
	private readonly navigationSourceService = inject(NavigationSourceService)
	private readonly routerService = inject(RouterService)
	private readonly notificationService = inject(NotificationService)
	private readonly translate = inject(TranslateService)

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
		this.notificationService.showWarning(this.translate.instant('notifications.guard.direct_access_denied'))
		this.routerService.navigateTo('login')

		return false
	}
}
