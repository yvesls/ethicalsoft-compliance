import { inject, Injectable } from '@angular/core'
import { environment } from '../../../enviroments/environments'
import { RequestService } from '../../../core/services/request.service'

@Injectable({
	providedIn: 'root',
})
export abstract class BaseStore {
	protected baseController = ''
	protected readonly requestService: RequestService = inject(RequestService)

	constructor(baseControllerName: string) {
		this.requestService.apiUrl = environment.apiBaseUrl
		this.baseController = baseControllerName
	}

	getUrl(action: string): string {
		return action ? `${this.baseController}/${action}` : this.baseController
	}
}
