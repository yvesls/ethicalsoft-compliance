import { inject, Injectable } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { RequestService } from './request.service'
import { RequestInputOptions } from '../interfaces/request-input-options.interface'
import { NotificationResponse, NotificationStatus } from '../../shared/interfaces/notification/notification.interface'

const BASE_URL = 'api/notifications'

@Injectable({ providedIn: 'root' })
export class InternalNotificationService {
	private readonly request = inject(RequestService)

	list(): Promise<NotificationResponse[]> {
		const options: RequestInputOptions = { useAuth: true }
		return firstValueFrom(this.request.makeGet<NotificationResponse[]>(`${BASE_URL}`, options))
	}

	updateStatus(id: string, status: NotificationStatus): Promise<void> {
		const options: RequestInputOptions = { useAuth: true, data: { status } }
		return firstValueFrom(this.request.makePost<void>(`${BASE_URL}/${id}/status`, options))
	}
}
