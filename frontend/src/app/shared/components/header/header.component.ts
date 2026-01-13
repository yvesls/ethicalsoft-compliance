import { InternalNotificationService } from '../../../core/services/internal-notification.service'
import { NotificationResponse, NotificationStatus } from '../../interfaces/notification/notification.interface'
import { NotificationService } from '../../../core/services/notification.service'
import { Component, OnInit, inject, DestroyRef, HostListener, OnDestroy, AfterViewInit } from '@angular/core'
import { RouterService } from '../../../core/services/router.service'
import { Router, NavigationEnd } from '@angular/router'
import { filter } from 'rxjs/operators'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { CommonModule } from '@angular/common'

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent implements OnInit, OnDestroy, AfterViewInit {
  routerPath = ''
  isPanelOpen = false
  isLoading = false
  errorMessage = ''
  notifications: NotificationResponse[] = []
  private lastLoadedAt: number | null = null
  private refreshTimerId: ReturnType<typeof setInterval> | null = null

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
    if (this.isPanelOpen) {
      void this.loadNotificationsIfStale()
    }
  }

  closePanel(): void {
    this.isPanelOpen = false
  }

  async loadNotifications(force = false): Promise<void> {
    if (!force && this.lastLoadedAt && Date.now() - this.lastLoadedAt < 5 * 60 * 1000) {
      return
    }
    this.isLoading = true
    this.errorMessage = ''
    try {
      const result = await this.internalNotificationService.list()
      this.notifications = (result ?? []).filter((n) => n.status === 'UNREAD')
      this.lastLoadedAt = Date.now()
    } catch (error: unknown) {
      this.errorMessage = 'Não foi possível carregar as notificações.'
      this.notificationService.showError(error)
    } finally {
      this.isLoading = false
    }
  }

  async markAsRead(notification: NotificationResponse): Promise<void> {
    if (notification.status === 'READ') return
    await this.updateStatus(notification, 'READ')
    await this.loadNotifications(true)
  }

  async deleteNotification(notification: NotificationResponse): Promise<void> {
    await this.updateStatus(notification, 'DELETED')
    await this.loadNotifications(true)
  }

  private async updateStatus(notification: NotificationResponse, status: NotificationStatus): Promise<void> {
    try {
      await this.internalNotificationService.updateStatus(notification.id, status)
      this.notifications = this.notifications
        .map((n) => (n.id === notification.id ? { ...n, status } : n))
        .filter((n) => n.status === 'UNREAD')
    } catch (error: unknown) {
      this.notificationService.showError(error)
    }
  }

  get unreadCount(): number {
    return this.notifications.filter((n) => n.status === 'UNREAD').length
  }

  private loadNotificationsIfStale(): Promise<void> {
    return this.loadNotifications(!this.lastLoadedAt || Date.now() - this.lastLoadedAt >= 5 * 60 * 1000)
  }

  private startAutoRefresh(): void {
    if (this.refreshTimerId) return
    this.refreshTimerId = setInterval(() => {
      void this.loadNotifications(true)
    }, 5 * 60 * 1000)
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.isPanelOpen) return
    const target = event.target as Node | null
    const panel = document.querySelector('.notification-panel')
    const button = document.querySelector('.notification-toggle')
    if (panel && panel.contains(target)) return
    if (button && button.contains(target)) return
    this.closePanel()
  }

  ngAfterViewInit(): void {
    void this.loadNotifications(true)
    this.startAutoRefresh()
  }

  ngOnDestroy(): void {
    if (this.refreshTimerId) {
      clearInterval(this.refreshTimerId)
      this.refreshTimerId = null
    }
  }
}
