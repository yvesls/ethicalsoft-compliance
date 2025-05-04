import { Injectable } from '@angular/core'
import { BehaviorSubject } from 'rxjs'
import { AuthenticationService } from './authentication.service'
import { MENU_CONFIG, MenuItem } from '../config/menu.config'
import { LoggerService } from './logger.service'

@Injectable({
	providedIn: 'root',
})
export class MenuService {
	private menuItemsSubject = new BehaviorSubject<MenuItem[]>([])
	menuItems$ = this.menuItemsSubject.asObservable()

	constructor(private authService: AuthenticationService) {
		this.authService.userRoles$.subscribe({
			next: (userRoles) => {
				if (!Array.isArray(userRoles)) {
					LoggerService.warn('MenuService: userRoles is not an array. Proceeding with empty menu.')
					userRoles = []
				}
				this.updateMenu(userRoles)
			},
			error: (error) => {
				LoggerService.error('MenuService: Failed to load user roles', error)
			},
		})
	}

	private updateMenu(userRoles: string[]): void {
		if (!userRoles || userRoles.length === 0) {
			LoggerService.warn('MenuService: No roles found for user, filtering menu with no roles.')
		}
		const filteredMenu = this.filterMenu(MENU_CONFIG, userRoles)
		this.menuItemsSubject.next(filteredMenu)
	}

	private filterMenu(menu: MenuItem[], userRoles: string[]): MenuItem[] {
		return menu
			.filter((item) => !item.roles || item.roles.some((role) => userRoles.includes(role)))
			.map((item) => ({
				...item,
				children: item.children ? this.filterMenu(item.children, userRoles) : undefined,
			}))
	}
}
