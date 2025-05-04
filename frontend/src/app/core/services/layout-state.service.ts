import { Injectable } from '@angular/core'
import { BehaviorSubject, Observable } from 'rxjs'
import { StorageService } from './storage.service'

@Injectable({
	providedIn: 'root',
})
export class LayoutStateService {
	private showLayoutSubject = new BehaviorSubject<boolean>(true)
	showLayout$: Observable<boolean> = this.showLayoutSubject.asObservable()

	private isSidebarCollapsedSubject = new BehaviorSubject<boolean>(false)
	isSidebarCollapsed$: Observable<boolean> = this.isSidebarCollapsedSubject.asObservable()

	private sidebarMobileOpenedSubject = new BehaviorSubject<boolean>(false)
	sidebarMobileOpened$: Observable<boolean> = this.sidebarMobileOpenedSubject.asObservable()

	constructor(private storageService: StorageService) {
		this.showLayoutSubject.next(this.storageService.getShowLayout())
		this.isSidebarCollapsedSubject.next(this.storageService.getSidebarState())
		this.setSidebarMobileState(true)
	}

	toggleSidebar(): void {
		const newState = !this.isSidebarCollapsedSubject.value
		this.isSidebarCollapsedSubject.next(newState)
		this.storageService.setSidebarState(newState)
	}

	toggleSidebarMobile(): void {
		this.sidebarMobileOpenedSubject.next(!this.sidebarMobileOpenedSubject.value)
	}

	setSidebarMobileState(state: boolean): void {
		this.sidebarMobileOpenedSubject.next(state)
	}

	setShowLayout(show: boolean): void {
		this.showLayoutSubject.next(show)
		this.storageService.setShowLayout(show)
	}
}
