import { Injectable, inject } from '@angular/core'
import { Router, NavigationStart } from '@angular/router'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { filter } from 'rxjs/operators'
import { LoggerService } from './logger.service'
import { getErrorMessage } from '../../shared/enums/error-messages.enum'

@Injectable({
	providedIn: 'root',
})
export class NotificationService {
	private readonly router = inject(Router)

	constructor() {
		this.router.events
			.pipe(filter((event): event is NavigationStart => event instanceof NavigationStart), takeUntilDestroyed())
			.subscribe(() => {
				this.closeModal()
				LoggerService.info('NotificationService: Router event NavigationStart detected. Closing modal.')
			})
	}

	showWarning(error: unknown) {
		LoggerService.warn('NotificationService: Showing warning notification.')
		const message = typeof error === 'string' ? error : this.formatErrorMessage(error)
		this.showModal('warning', 'Atenção', message)
	}

	showError(error: unknown) {
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

	private formatErrorMessage(error: unknown): string {
		if (typeof error === 'string') {
			return error
		}

		if (error instanceof Error) {
			LoggerService.error('NotificationService: Formatting error message from Error instance', error)
			return error.message
		}

		if (this.isApiError(error)) {
			LoggerService.error('NotificationService: Formatting API error message', error)
			const message = error.message?.trim()
			const status = error.status ?? 0
			return `**Erro ${status}** - ${error.errorType ?? 'ERROR'}: ${message || getErrorMessage(status)}`
		}

		LoggerService.error('NotificationService: Unknown error type received', error)
		return getErrorMessage(0)
	}

	private showModal(
		type: 'success' | 'warning' | 'error' | 'confirm',
		title: string,
		message: string,
		callbackConfirm?: () => void,
		callbackCancel?: () => void
	) {
		LoggerService.info('NotificationService: Displaying modal of type', type)
		if (!this.hasDOM()) {
			LoggerService.warn('NotificationService: DOM is not available, skipping modal rendering.')
			return
		}
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
		if (!this.hasDOM()) {
			return
		}
		const modals = document.querySelectorAll<HTMLElement>('.notification-modal')
		for (const modal of modals) {
			modal.classList.add('closing')
			modal.remove()
			LoggerService.info('NotificationService: Modal removed from DOM.')
		}
	}

	private isApiError(error: unknown): error is ApiError {
		return typeof error === 'object' && error !== null && ('status' in error || 'errorType' in error || 'message' in error)
	}

	private hasDOM(): boolean {
		return typeof document !== 'undefined'
	}
}

interface ApiError {
	status?: number
	errorType?: string
	message?: string
}
