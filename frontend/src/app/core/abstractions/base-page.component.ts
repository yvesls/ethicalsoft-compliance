import { Directive, HostListener, inject, OnDestroy, OnInit } from '@angular/core'
import { Observable, take } from 'rxjs'
import { MenuService } from '../services/menu.service'
import { NavigateParams, RouteHistoryParams, RouteParams, RouterService } from '../services/router.service'
import { hasProperties } from '../utils/common-utils'

@Directive()
export abstract class BasePageComponent implements OnInit, OnDestroy {
	private objCopy: string = ''
	menuService = inject(MenuService)
	routerService = inject(RouterService)
	protected routeInfo: RouteHistoryParams<any> = { vid: '', route: '', params: {} }

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
	protected abstract save(): any
	protected abstract restore(restoreParameter: RestoreParams<any>): void
	protected abstract loadParams(params: any, queryParams?: any): void

	protected deepCopy(obj: any): void {
		this.objCopy = JSON.stringify(obj)
	}

	protected hasObjChanged(obj: any): boolean {
		return this.objCopy !== JSON.stringify(obj)
	}

	private _onComponentInit(): Observable<any> {
		return new Observable<any>((observer) => {
			observer.next(this.onInit())
			observer.complete()
		})
	}

	private _validateVID(): void {
		this.routerService
			.getRouteInfoParams()
			.pipe(take(1))
			.subscribe((activatedRouteInfo: RouteHistoryParams<any>) => {
				this.routeInfo = activatedRouteInfo
				if (!this.routeInfo.vid) {
					this._getVID()
				} else {
					this._startWithVID()
				}
			})
	}

	private _getVID(): void {
		const navigateParams: NavigateParams<any> = {
			params: this.routeInfo.params,
			queryParams: this.routeInfo.queryParams,
		}
		const [uri] = this.routerService.currentUrl.split('?')
		this.routerService.navigateTo(uri, navigateParams).then(() => this._validateVID())
	}

	private _startWithVID(): void {
		this._onComponentInit().subscribe(() => {
			let startPageParams: RouteParams<any> =
				this.routerService.getStoredPageViewParams(this.routeInfo.vid)?.obj?.params || {}

			this.restore({
				...startPageParams,
				hasParams: hasProperties(startPageParams),
			})

			if (!hasProperties(this.routeInfo.params)) {
				this.routeInfo.params = startPageParams
			}
			this.objCopy = startPageParams.objCopy || ''
			this.loadParams(this.routeInfo.params, this.routeInfo.queryParams)

			this._onSave()
			this.routerService.setStoredCurrentPage(this.routeInfo)
		})
	}

	private _onSave(): void {
		if (this.routeInfo.vid) {
			const routeHistoryParams: RouteHistoryParams<any> = this.routeInfo
			routeHistoryParams.params = this.save() || {}
			routeHistoryParams.params.objCopy = this.objCopy
			routeHistoryParams.route = routeHistoryParams.route?.split('?')[0] || ''
			this.routerService.setStoredPageViewParams(this.routeInfo.vid, {
				d: new Date(),
				obj: routeHistoryParams,
			})
		}
	}
}

export type RestoreParams<T> = { [s in keyof T]: any } & { hasParams: boolean }
