import { Directive, HostListener, inject, OnDestroy, OnInit } from '@angular/core'
import { Params } from '@angular/router'
import { Observable, take } from 'rxjs'
import { MenuService } from '../services/menu.service'
import {
	GenericParams,
	NavigateParams,
	RouteHistoryParams,
	RouteParams,
	RouterService,
} from '../services/router.service'
import { hasProperties } from '../utils/common-utils'
import { LoggerService } from '../services/logger.service'

@Directive()
export abstract class BasePageComponent<TParams extends GenericParams = GenericParams> implements OnInit, OnDestroy {
	private objCopy = ''
	menuService = inject(MenuService)
	routerService = inject(RouterService)
	protected routeInfo: RouteHistoryParams<TParams> = {
		vid: '',
		route: '',
		params: {} as RouteParams<TParams>,
	}

	@HostListener('window:beforeunload')
	beforeunloadHandler(): void {
		this._onSave()
	}

	ngOnDestroy(): void {
		this._onSave()
	}

	ngOnInit(): void {
		this._validateVID()
	}

	protected abstract onInit(): void
	protected abstract save(): RouteParams<TParams> | undefined
	protected abstract restore(restoreParameter: RestoreParams<TParams>): void
	protected abstract loadParams(params: RouteParams<TParams>, queryParams?: Params | undefined): void

	protected deepCopy(obj: GenericParams): void {
		this.objCopy = JSON.stringify(obj)
	}

	protected hasObjChanged(obj: GenericParams): boolean {
		return this.objCopy !== JSON.stringify(obj)
	}

	private _onComponentInit(): Observable<void> {
		return new Observable<void>((observer) => {
			observer.next(this.onInit())
			observer.complete()
		})
	}

	private _validateVID(): void {
		this.routerService
			.getRouteInfoParams<TParams>()
			.pipe(take(1))
			.subscribe({
				next: (activatedRouteInfo: RouteHistoryParams<TParams>) => {
					this.routeInfo = activatedRouteInfo

					if (this.routeInfo.vid) {
						this._startWithVID()
						return
					}

					this._getVID()
				},
				error: (error) => {
					LoggerService.error('BasePageComponent: Error validating route info', error)
				},
			})
	}

	private _getVID(): void {
		const navigateParams: NavigateParams<TParams> = {
			params: this.routeInfo.params,
			queryParams: this.routeInfo.queryParams,
		}
		const [uri] = this.routerService.currentUrl.split('?')
		this.routerService
			.navigateTo(uri, navigateParams)
			.then(() => this._validateVID())
			.catch((error) => {
				LoggerService.error('BasePageComponent: Error while navigating to URI for VID', error)
			})
	}

	private _startWithVID(): void {
		this._onComponentInit().subscribe({
			next: () => {
				const storedParams = this.routerService.getStoredPageViewParams(this.routeInfo.vid)?.obj
					?.params as RouteParams<TParams> | undefined
				const startPageParams: RouteParams<TParams> = storedParams || ({
				} as RouteParams<TParams>)

				this.restore({
					...startPageParams,
					hasParams: hasProperties(startPageParams),
				})

				if (!hasProperties(this.routeInfo.params)) {
					LoggerService.warn('BasePageComponent: No parameters found in route info, using default parameters')
					this.routeInfo.params = startPageParams
				}
				this.objCopy = startPageParams.objCopy || ''
				this.loadParams(this.routeInfo.params, this.routeInfo.queryParams)

				this._onSave()
				this.routerService.setStoredCurrentPage(this.routeInfo)
			},
			error: (error) => {
				LoggerService.error('BasePageComponent: Error during component initialization with VID', error)
			},
		})
	}

	private _onSave(): void {
		if (this.routeInfo.vid) {
				const routeHistoryParams: RouteHistoryParams<TParams> = this.routeInfo
				routeHistoryParams.params = this.save() || ({} as RouteParams<TParams>)
			routeHistoryParams.params.objCopy = this.objCopy
			routeHistoryParams.route = routeHistoryParams.route?.split('?')[0] || ''
			this.routerService.setStoredPageViewParams(this.routeInfo.vid, {
				d: new Date(),
				obj: routeHistoryParams,
			})
		} else {
			LoggerService.warn('BasePageComponent: No VID found, unable to save route information')
		}
	}
}

export type RestoreParams<T extends GenericParams> = RouteParams<T> & { hasParams: boolean }
