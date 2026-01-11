import { NotificationService } from '../../../core/services/notification.service'
import { Component, OnInit, inject, DestroyRef } from '@angular/core'
import { RouterService } from '../../../core/services/router.service'
import { Router, NavigationEnd } from '@angular/router'
import { filter } from 'rxjs/operators'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { CommonModule } from '@angular/common'
import { InternalNotificationService } from '../../../core/services/internal-notification.service'
import { NotificationResponse, NotificationStatus } from '../../../shared/interfaces/notification/notification.interface'
import { Page } from '../../../shared/interfaces/pageable.interface'

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent implements OnInit {
  routerPath = ''
  isPanelOpen = false
  isLoading = false
  errorMessage = ''
  notifications: NotificationResponse[] = []
  pageInfo: Page<NotificationResponse> | null = null
  pageSize = 10

  private router = inject(Router)
  private destroyRef = inject(DestroyRef)
  private routerService = inject(RouterService)
  private notificationService = inject(NotificationService)
  private internalNotificationService = inject(InternalNotificationService)

	ngOnInit(): void {
    this.routerPath = this.routerService.getFormattedRoute()

    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.routerPath = this.routerService.getFormattedRoute()
        this.closePanel()
      })
  }

  openNotifications(): void {
    this.isPanelOpen = !this.isPanelOpen
    if (this.isPanelOpen && !this.notifications.length) {
      void this.loadNotifications()
    }
  }

  closePanel(): void {
    this.isPanelOpen = false
  }

  async loadNotifications(page = 0): Promise<void> {
    this.isLoading = true
    this.errorMessage = ''
    try {
      const result = await this.internalNotificationService.list(page, this.pageSize)
      this.notifications = result?.content ?? []
      this.pageInfo = result ?? null
    } catch (error) {
      this.errorMessage = 'Não foi possível carregar as notificações.'
      this.notificationService.showError(error)
    } finally {
      this.isLoading = false
    }
  }

  async markAsRead(notification: NotificationResponse): Promise<void> {
    if (notification.status === 'READ') return
    await this.updateStatus(notification, 'READ')
  }

  async deleteNotification(notification: NotificationResponse): Promise<void> {
    await this.updateStatus(notification, 'DELETED')
  }

  private async updateStatus(notification: NotificationResponse, status: NotificationStatus): Promise<void> {
    try {
      await this.internalNotificationService.updateStatus(notification.id, status)
      this.notifications = this.notifications.map((n) =>
        n.id === notification.id ? { ...n, status } : n
      )
    } catch {
      this.notificationService.showError('Falha ao atualizar status da notificação.')
    }
  }
}
