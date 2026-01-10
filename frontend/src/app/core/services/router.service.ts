import { inject, Injectable } from '@angular/core'
import { ActivatedRoute, NavigationEnd, Params, Router } from '@angular/router'
import { BehaviorSubject, filter, Observable } from 'rxjs'
import { Md5 } from 'ts-md5'
import { deepCopy } from '../utils/common-utils'
import { NotificationService } from './notification.service'
import { StorageService } from './storage.service'
import { addDays } from '../utils/date-utils'
import { LayoutStateService } from './layout-state.service'
import { NavigationSourceService } from './navigation-source.service'
import { LoggerService } from './logger.service'

export type GenericParams = Record<string, unknown>

@Injectable({ providedIn: 'root' })
export class RouterService {
	private params: GenericParams = {}
	private currentRouteSubject = new BehaviorSubject<string>('')
	currentRoute$ = this.currentRouteSubject.asObservable()

	private readonly activatedRoute = inject(ActivatedRoute)
	private readonly router = inject(Router)
	private readonly storageService = inject(StorageService)
	private readonly notificationService = inject(NotificationService)
	private readonly layoutStateService = inject(LayoutStateService)
	private readonly navigationSourceService = inject(NavigationSourceService)

	constructor() {
		this.clearOldViewPageData()
		this.currentRouteSubject.next(this.router.url)
		this.monitorRouteChanges()
	}

	private monitorRouteChanges(): void {
		this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
			let currentRoute = this.activatedRoute
			while (currentRoute.firstChild) {
				currentRoute = currentRoute.firstChild
			}
			const showLayout = currentRoute.snapshot.data?.['showLayout'] !== false
			this.layoutStateService.setShowLayout(showLayout)
		})
	}

	async navigateTo<T extends GenericParams>(
		url: string,
		navigateParams?: NavigateParams<T>,
		isFormDirty = false
	): Promise<boolean> {
		const { params = {}, queryParams = {} } = navigateParams || {}

		if (isFormDirty) {
			return new Promise((resolve) => {
				this.notificationService.showConfirm(
					'Os dados não salvos serão perdidos. Deseja continuar?',
					async () => {
						const result = await this._redirectTo(url, queryParams)
						resolve(result)
					},
					() => resolve(false)
				)
			})
		}

		this._createPageData<T>(url, params, queryParams)
		this.navigationSourceService.setInternalNavigation(true)

		return this._redirectTo(url, queryParams)
	}

	navigateToNewTab<T extends GenericParams>(url: string, navigateParams?: NavigateParams<T>): void {
		const { queryParams = {} } = navigateParams || {}
		this._generateVID(queryParams)
		this.navigationSourceService.setInternalNavigation(true)
		globalThis.open(`${url}?vid=${queryParams['vid']}`, '_blank')
		LoggerService.warn('Opening URL in new tab', { url, queryParams })
	}

	rawNavigate(uri: string, queryParams?: Params | null): Promise<boolean> {
		return this.router.navigate([uri], { queryParams })
	}

	rawNavigateToNewTab(url: string): void {
		globalThis.open(url, '_blank')
	}

	getRouteInfoParams<T extends GenericParams>(): Observable<RouteHistoryParams<T>> {
		return new Observable((observer) => {
			this.activatedRoute.queryParams.subscribe({
				next: (routeQueryParams: Params) => {
					const routerHistoryParams: RouteHistoryParams<T> = {
						vid: routeQueryParams['vid'] || '',
						route: this.router.url.split('?')[0] || '',
						params: { ...this.params },
						queryParams: deepCopy(routeQueryParams),
					}
					observer.next(routerHistoryParams)
					observer.complete()
				},
				error: (error) => {
					LoggerService.error('Error getting route info parameters', error)
					observer.complete()
				},
			})
		})
	}

	get currentUrl(): string {
		return this.router.url
	}

	getFormattedRoute(): string {
		const url = this.router.url.split('?')[0]
		const segments = url.split('/').filter(Boolean)
		if (!segments.length) return 'Home'
		return segments.map((segment) => this.capitalizeWords(segment)).join(' > ')
	}

	private capitalizeWords(str: string): string {
		return str
			.split('-')
			.map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
			.join('-')
	}

	backToPrevious(
		inverseIndex = 1,
		removeVID = true,
		updatedParams?: GenericParams,
		fallbackRoute = ''
	): void {
		const currentViewPage = this.getStoredCurrentPage()
		if (!this._isStoredViewPage(currentViewPage.vid)) inverseIndex = 0
		else if (removeVID) {
			this.storageService.remHistVID(currentViewPage.vid)
			this.storageService.remove(currentViewPage.vid)
			this.removeTrailingEntriesByRoute(currentViewPage.route)
		}
		const previousViewPage = this.getStoredViewPageByHistory(inverseIndex)
		if (previousViewPage?.obj?.route) {
			if (updatedParams) {
				if (previousViewPage.obj.params && previousViewPage.obj.params.p) {
					previousViewPage.obj.params.p = { ...previousViewPage.obj.params.p, ...updatedParams }
				} else {
					previousViewPage.obj.params = { ...previousViewPage.obj.params, ...updatedParams }
				}
			}
			this.navigateTo(previousViewPage?.obj?.route, previousViewPage.obj)
		} else {
			this.navigateTo(fallbackRoute)
		}
	}

	browserBack(): void {
		const history = globalThis.history
		if (history.length > 1) {
			history.back()
		} else {
			this.navigateTo('')
		}
	}

	private _generateVID(queryParams: Params): void {
		if (!queryParams['vid']) {
			queryParams['vid'] = Md5.hashStr(Math.random().toString())
		}
	}

	private async _redirectTo(uri: string, queryParams?: Params | null): Promise<boolean> {
		return this.router.navigate([uri], { queryParams })
	}

	private _createPageData<T extends GenericParams>(url: string, params: RouteParams<T>, queryParams: Params): void {
		this.params = params
		this._generateVID(queryParams)
		const vid = queryParams['vid']
		this.setStoredCurrentPage({ vid: vid, route: url })
		this.setStoredPageViewParams(vid, {
			d: new Date(),
			obj: { vid: vid, route: url, params: { ...this.params }, queryParams: queryParams },
		})
	}

	private _isStoredViewPage(vid: string): boolean {
		return this.storageService.getHistVID()?.includes(vid) ?? false
	}

	private getStoredViewPageByHistory(inverseIndex: number): RouteStorageParams | null {
		const histVID = this.storageService.getHistVID()
		const vidIndex = histVID.length - 1 - inverseIndex
		const vid = histVID[vidIndex]
		if (vid) {
			return this.getStoredPageViewParams(vid)
		}
		return null
	}

	private getStoredCurrentPage(): NavigateInfo {
		return this.storageService.getCurrentPage()
	}

	setStoredCurrentPage(currentData: NavigateInfo): void {
		this.storageService.setCurrentPage(currentData)
	}

	getStoredPageViewParams(vid: string): RouteStorageParams | null {
		return this.storageService.getViewPageData(vid)
	}

	setStoredPageViewParams(vid: string, storageData: RouteStorageParams): void {
		this.storageService.setViewPageData(vid, storageData)
	}

	private clearOldViewPageData(): void {
		const expiredStorageDate = addDays(-7)
		const lastViewPag = this.getStoredViewPageByHistory(0)
		if (!lastViewPag || new Date(lastViewPag.d) < expiredStorageDate) {
			this.storageService.clear()
		} else {
			const histVID = this.storageService.getHistVID()
			for (const vidKey of histVID) {
				const viewPageData = this.storageService.getViewPageData(vidKey)
				if (!viewPageData || new Date(viewPageData.d) < expiredStorageDate) {
					this.storageService.remove(vidKey)
				}
			}
		}
	}

	private removeTrailingEntriesByRoute(route: string): void {
		if (!route) {
			return
		}

		const histVID = this.storageService.getHistVID()
		const vidsToRemove: string[] = []

		for (let i = histVID.length - 1; i >= 0; i--) {
			const vid = histVID[i]
			const viewData = this.getStoredPageViewParams(vid)
			if (viewData?.obj?.route === route) {
				vidsToRemove.push(vid)
			} else {
				break
			}
		}

		if (vidsToRemove.length) {
			this.storageService.remHistVID(...vidsToRemove)
			for (const vid of vidsToRemove) {
				this.storageService.remove(vid)
			}
		}
	}
}

export interface NavigateInfo {
	vid: string
	route: string
}

export interface NavigateParams<T extends GenericParams> {
	params: RouteParams<T>
	queryParams?: Params
}

export interface RouteHistoryParams<T extends GenericParams> {
	vid: string
	route: string
	params: RouteParams<T>
	queryParams?: Params
}

export type RouteParams<T extends GenericParams> = GenericParams & {
	objCopy?: string
	p?: T
}

export interface RouteStorageParams {
	d: Date
	obj: RouteHistoryParams<GenericParams>
}
