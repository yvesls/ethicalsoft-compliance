import { Injectable, OnDestroy } from '@angular/core'
import { Router, NavigationStart, Event as RouterEvent } from '@angular/router'
import { Subscription } from 'rxjs'
import { filter } from 'rxjs/operators'
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
			})
	}

	ngOnDestroy() {
		if (this.routerSubscription) {
			this.routerSubscription.unsubscribe()
		}
	}

	showWarning(error: any) {
		if (typeof error === 'string') {
			this.showModal('warning', 'Atenção', error)
		} else {
			this.showModal('warning', 'Atenção', this.formatErrorMessage(error))
		}
	}

	showError(error: any) {
		this.showModal('error', 'Erro', this.formatErrorMessage(error))
	}

	showSuccess(message: string) {
		this.showModal('success', 'Sucesso', message)
	}

	showConfirm(message: string, callbackConfirm: () => void, callbackCancel?: () => void) {
		this.showModal('confirm', 'Atenção', message, callbackConfirm, callbackCancel)
	}

	private formatErrorMessage(error: any): string {
		const errorMessage = error.message?.trim()
		return `**Erro ${error.status}** - ${error.errorType}: ${errorMessage || getErrorMessage(error.status)}`
	}

	private showModal(
		type: 'success' | 'warning' | 'error' | 'confirm',
		title: string,
		message: string,
		callbackConfirm?: () => void,
		callbackCancel?: () => void
	) {
		this.closeModal()

		const modal = document.createElement('div')
		modal.classList.add('modal', 'notification-modal', type)

		modal.innerHTML = `
      <div class="very-small-card modal-content" @modalAnimation>
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

		modal.querySelector('.close')?.addEventListener('click', () => this.closeModal())

		if (type === 'confirm') {
			modal.querySelector('.btn-cancel')?.addEventListener('click', () => {
				this.closeModal()
				callbackCancel?.()
			})

			modal.querySelector('.btn-confirm')?.addEventListener('click', () => {
				this.closeModal()
				callbackConfirm?.()
			})
		}

		modal.addEventListener('click', (evt: MouseEvent) => {
			const target = evt.target as HTMLElement
			if (target === modal) {
				this.closeModal()
			}
		})
	}

	closeModal() {
		document.querySelectorAll('.notification-modal').forEach((modal) => {
			modal.classList.add('closing')
			setTimeout(() => modal.remove(), 50)
		})
	}
}
