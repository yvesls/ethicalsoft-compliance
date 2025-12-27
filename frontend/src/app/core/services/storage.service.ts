import { inject, Injectable, PLATFORM_ID } from '@angular/core'
import { isPlatformBrowser } from '@angular/common'
import { NavigateInfo, RouteStorageParams } from './router.service'
import { LoggerService } from '../services/logger.service'

@Injectable({
	providedIn: 'root',
})
export class StorageService {
	private readonly AUTH_TOKEN_KEY = 'auth_token'
	private readonly NAVIGATION_HISTORY_KEY = 'navigation_history'
	private readonly CURRENT_PAGE_KEY = 'current_page'
	private readonly SIDEBAR_STATE_KEY = 'sidebar_collapsed'
	private readonly SHOW_LAYOUT_KEY = 'show_layout'
	private readonly platformId = inject(PLATFORM_ID)

	setAuthToken(token: string | null): void {
		if (isPlatformBrowser(this.platformId)) {
			if (token) {
				try {
					localStorage.setItem(this.AUTH_TOKEN_KEY, token)
					LoggerService.warn('StorageService: Auth token saved successfully')
				} catch (error) {
					LoggerService.error('StorageService: Failed to save auth token', error)
				}
			} else {
				localStorage.removeItem(this.AUTH_TOKEN_KEY)
				LoggerService.warn('StorageService: Auth token removed successfully')
			}
		}
	}

	getAuthToken(): string | null {
		if (isPlatformBrowser(this.platformId)) {
			return localStorage.getItem(this.AUTH_TOKEN_KEY)
		}
		return null
	}

	clearAuthToken(): void {
		if (isPlatformBrowser(this.platformId)) {
			localStorage.removeItem(this.AUTH_TOKEN_KEY)
			LoggerService.warn('StorageService: Auth token cleared')
		}
	}

	getHistVID(): string[] {
		if (isPlatformBrowser(this.platformId)) {
			const history = localStorage.getItem(this.NAVIGATION_HISTORY_KEY)
			if (!history) {
				LoggerService.warn('StorageService: No navigation history found')
			}
			try {
				return history ? JSON.parse(history) : []
			} catch (error) {
				LoggerService.error('StorageService: Failed to parse navigation history', error)
				return []
			}
		}
		return []
	}

	remHistVID(...vids: string[]): void {
		if (isPlatformBrowser(this.platformId)) {
			const history = this.getHistVID().filter((vid) => !vids.includes(vid))
			localStorage.setItem(this.NAVIGATION_HISTORY_KEY, JSON.stringify(history))
			LoggerService.warn(`StorageService: Removed VIDs: ${vids.join(', ')}`)
		}
	}

	setCurrentPage(currentData: NavigateInfo): void {
		if (isPlatformBrowser(this.platformId)) {
			localStorage.setItem(this.CURRENT_PAGE_KEY, JSON.stringify(currentData))
			LoggerService.warn('StorageService: Current page set successfully')
		}
	}

	getCurrentPage(): NavigateInfo {
		if (isPlatformBrowser(this.platformId)) {
			const data = localStorage.getItem(this.CURRENT_PAGE_KEY)
			if (!data) {
				LoggerService.warn('StorageService: No current page found')
			}
			try {
				return data ? JSON.parse(data) : { vid: '', route: '' }
			} catch (_error) {
				LoggerService.error('StorageService: Failed to parse current page', _error)
				return { vid: '', route: '' }
			}
		}
		return { vid: '', route: '' }
	}

	setViewPageData(vid: string, storageData: RouteStorageParams): void {
		if (isPlatformBrowser(this.platformId) && vid) {
			const key = `view_page_${vid}`
			localStorage.setItem(key, JSON.stringify(storageData))

			const history = this.getHistVID()
			if (!history.includes(vid)) {
				history.push(vid)
				localStorage.setItem(this.NAVIGATION_HISTORY_KEY, JSON.stringify(history))
			}
		}
	}

	getViewPageData(vid: string): RouteStorageParams | null {
		if (isPlatformBrowser(this.platformId) && vid) {
			const key = `view_page_${vid}`
			const data = localStorage.getItem(key)

			if (!data) {
				return null
			}

			try {
				return JSON.parse(data)
			} catch (error) {
				LoggerService.error('StorageService: Failed to parse view page data', {
					error,
					vid,
				})
				return null
			}
		}
		return null
	}

	setSidebarState(collapsed: boolean): void {
		if (isPlatformBrowser(this.platformId)) {
			localStorage.setItem(this.SIDEBAR_STATE_KEY, JSON.stringify(collapsed))
		}
	}

	getSidebarState(): boolean {
		if (isPlatformBrowser(this.platformId)) {
			return JSON.parse(localStorage.getItem(this.SIDEBAR_STATE_KEY) || 'false')
		}
		return false
	}

	setShowLayout(show: boolean): void {
		if (isPlatformBrowser(this.platformId)) {
			localStorage.setItem(this.SHOW_LAYOUT_KEY, JSON.stringify(show))
		}
	}

	getShowLayout(): boolean {
		if (isPlatformBrowser(this.platformId)) {
			const storedValue = localStorage.getItem(this.SHOW_LAYOUT_KEY)
			try {
				const parsedValue = JSON.parse(storedValue || 'true')
				return typeof parsedValue === 'boolean' ? parsedValue : true
			} catch (error) {
				LoggerService.error('StorageService: Failed to parse show layout flag', error)
				return true
			}
		}
		return true
	}

	remove(vid: string): void {
		if (isPlatformBrowser(this.platformId) && vid) {
			const key = `view_page_${vid}`
			localStorage.removeItem(key)
		}
	}

	clear(): void {
		if (isPlatformBrowser(this.platformId)) {
			localStorage.removeItem(this.NAVIGATION_HISTORY_KEY)
			localStorage.removeItem(this.CURRENT_PAGE_KEY)
			for (const key of Object.keys(localStorage)) {
				if (key.startsWith('view_page_')) {
					localStorage.removeItem(key)
				}
			}
			LoggerService.warn('StorageService: All storage cleared')
		}
	}
}
