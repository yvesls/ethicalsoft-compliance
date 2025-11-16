import { Injectable, OnDestroy } from '@angular/core'
import { Router, NavigationStart } from '@angular/router'
import { Subscription } from 'rxjs'
import { filter } from 'rxjs/operators'
import { LoggerService } from './logger.service'
import { getErrorMessage } from '../../shared/enums/error-messages.enum'

@Injectable({
	providedIn: 'root',
})
export class NotificationService implements OnDestroy {
	private routerSubscription: Subscription

	constructor(private router: Router) {
		this.routerSubscription = this.router.events
			.pipe(filter((event): event is NavigationStart => event instanceof NavigationStart))
			.subscribe(() => {
				this.closeModal()
				LoggerService.info('NotificationService: Router event NavigationStart detected. Closing modal.')
			})
	}

	ngOnDestroy() {
		if (this.routerSubscription) {
			this.routerSubscription.unsubscribe()
			LoggerService.info('NotificationService: Unsubscribed from router events.')
		}
	}

	showWarning(error: any) {
		LoggerService.warn('NotificationService: Showing warning notification.')
		if (typeof error === 'string') {
			this.showModal('warning', 'Atenção', error)
		} else {
			this.showModal('warning', 'Atenção', this.formatErrorMessage(error))
		}
	}

	showError(error: any) {
		LoggerService.error('NotificationService: Showing error notification.')
		this.showModal('error', 'Erro', this.formatErrorMessage(error))
	}

	showSuccess(message: string) {
		LoggerService.info('NotificationService: Showing success notification.')
		this.showModal('success', 'Sucesso', message)
	}

	showConfirm(message: string, callbackConfirm: () => void, callbackCancel?: () => void) {
		LoggerService.warn('NotificationService: Showing confirmation modal.')
		this.showModal('confirm', 'Atenção', message, callbackConfirm, callbackCancel)
	}

	private formatErrorMessage(error: any): string {
		const errorMessage = error.message?.trim()
		LoggerService.error('NotificationService: Formatting error message', error)
		return `**Erro ${error.status}** - ${error.errorType}: ${errorMessage || getErrorMessage(error.status)}`
	}

	private showModal(
		type: 'success' | 'warning' | 'error' | 'confirm',
		title: string,
		message: string,
		callbackConfirm?: () => void,
		callbackCancel?: () => void
	) {
		LoggerService.info('NotificationService: Displaying modal of type', type)
		this.closeModal()

		const modal = document.createElement('div')
		modal.classList.add('modal', 'notification-modal', type)

		modal.innerHTML = `
      <div class="small-card modal-content" @modalAnimation>
        <div class="close text-end">x</div>
        <img class="modal-icon" src="assets/icons/${type}.svg" alt="Ícone ${title}">
        <h2 class="modal-title">${title}</h2>
        <p class="modal-message">${message}</p>
        ${
			type === 'confirm'
				? `
          <div class="modal-buttons">
            <button class="btn-cancel">Cancelar</button>
            <button class="btn-confirm">Confirmar</button>
          </div>`
				: ''
		}
      </div>
    `

		document.body.appendChild(modal)

		modal.querySelector('.close')?.addEventListener('click', () => {
			this.closeModal()
			LoggerService.info('NotificationService: Modal closed by user.')
		})

		if (type === 'confirm') {
			modal.querySelector('.btn-cancel')?.addEventListener('click', () => {
				this.closeModal()
				callbackCancel?.()
				LoggerService.info('NotificationService: Modal canceled by user.')
			})

			modal.querySelector('.btn-confirm')?.addEventListener('click', () => {
				this.closeModal()
				callbackConfirm?.()
				LoggerService.info('NotificationService: Modal confirmed by user.')
			})
		}

		modal.addEventListener('click', (evt: MouseEvent) => {
			const target = evt.target as HTMLElement
			if (target === modal) {
				this.closeModal()
				LoggerService.info('NotificationService: Modal closed by clicking outside.')
			}
		})
	}

	closeModal() {
		document.querySelectorAll('.notification-modal').forEach((modal) => {
			modal.classList.add('closing')
      modal.remove()
      LoggerService.info('NotificationService: Modal removed from DOM.')
		})
	}
}
