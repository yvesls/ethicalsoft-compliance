import { inject, Injectable } from '@angular/core'
import { environment } from '../../../enviroments/environments'
import { RequestService } from '../../../core/services/request.service'

@Injectable({
	providedIn: 'root',
})
export abstract class BaseStore {
	protected baseController: string = ''
	protected requestService: RequestService

	constructor(baseControllerName: string) {
		this.requestService = inject(RequestService)
		this.requestService.apiUrl = environment.apiBaseUrl

		this.baseController = baseControllerName
	}

	getUrl(action: string) {
		return `/${this.baseController}/${action}`
	}
}
