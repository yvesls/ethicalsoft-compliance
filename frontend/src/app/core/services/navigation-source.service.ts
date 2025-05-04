import { Injectable } from '@angular/core'
import { LoggerService } from './logger.service'

@Injectable({
	providedIn: 'root',
})
export class NavigationSourceService {
	private _isInternalNavigation = false

	setInternalNavigation(isInternal: boolean): void {
		LoggerService.info('NavigationSourceService: Setting internal navigation state to:', isInternal)
		this._isInternalNavigation = isInternal
	}

	isInternalNavigation(): boolean {
		const result = this._isInternalNavigation
		LoggerService.info('NavigationSourceService: Checking internal navigation state. Result:', result)
		this._isInternalNavigation = false
		return result
	}
}
