import { inject, Injectable } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { RequestService } from './request.service'
import { RequestInputOptions } from '../interfaces/request-input-options.interface'
import { Page } from '../../shared/interfaces/pageable.interface'
import { NotificationResponse, NotificationStatus } from '../../shared/interfaces/notification/notification.interface'

const BASE_URL = 'api/notifications'

@Injectable({ providedIn: 'root' })
export class InternalNotificationService {
  private readonly request = inject(RequestService)

  list(page = 0, size = 10): Promise<Page<NotificationResponse>> {
    const options: RequestInputOptions = { useAuth: true }
    return firstValueFrom(
      this.request.makeGet<Page<NotificationResponse>>(`${BASE_URL}?page=${page}&size=${size}`, options)
    )
  }

  updateStatus(id: string, status: NotificationStatus): Promise<void> {
    const options: RequestInputOptions = { useAuth: true, data: { status } }
    return firstValueFrom(this.request.makePatch<void>(`${BASE_URL}/${id}/status`, options))
  }
}
