import { Injectable, ApplicationRef, createComponent, Type, ComponentRef, OnDestroy } from '@angular/core'
import { FormGroup } from '@angular/forms'
import { Router, NavigationStart } from '@angular/router'
import { Subscription } from 'rxjs'
import { filter } from 'rxjs/operators'
import { NotificationService } from './notification.service'
import { LoggerService } from './logger.service' // Assume LoggerService exists

export type ModalSize = 'very-small-card' | 'small-card' | 'medium-card' | 'large-card'

@Injectable({ providedIn: 'root' })
export class ModalService implements OnDestroy {
	private modalRef?: ComponentRef<any>
	private modalElement?: HTMLElement
	private isConfirming = false
	private routerSubscription: Subscription
	private pendingNavigation?: any

	constructor(
		private appRef: ApplicationRef,
		private notificationService: NotificationService,
		private router: Router
	) {
		this.routerSubscription = this.router.events
			.pipe(filter((event): event is NavigationStart => event instanceof NavigationStart))
			.subscribe((event) => {
				if (this.modalRef) {
					this.handleNavigation(event)
				}
			})
	}

	ngOnDestroy() {
		this.routerSubscription?.unsubscribe()
	}

	private handleNavigation(event: NavigationStart) {
		const instance = this.modalRef?.instance as any
		const form = instance?.form

		if (form instanceof FormGroup && form.touched && !this.isConfirming) {
			this.pendingNavigation = {
				url: event.url,
				extras:
					event.navigationTrigger === 'imperative'
						? {
								skipLocationChange: false,
								replaceUrl: false,
							}
						: undefined,
			}

			this.isConfirming = true
			this.notificationService.showConfirm(
				'Você tem alterações não salvas. Deseja sair mesmo assim?',
				() => {
					this.isConfirming = false
					this.close()
					if (this.pendingNavigation) {
						const nav = this.pendingNavigation
						this.pendingNavigation = undefined
						this.router.navigateByUrl(nav.url, nav.extras)
					}
				},
				() => {
					this.isConfirming = false
					this.pendingNavigation = undefined
				}
			)

			this.router.navigate([], {
				skipLocationChange: true,
				replaceUrl: true,
			})
		} else {
			this.close()
		}
	}

	open<T>(component: Type<T>, size: ModalSize = 'small-card', data?: Partial<T>): void {
		LoggerService.info('Opening modal with component:', component)
		if (this.modalRef) {
			this.attemptClose(() => this.open(component, size, data))
			return
		}

		this.modalRef = createComponent(component, { environmentInjector: this.appRef.injector })

		if (data) {
			Object.assign(this.modalRef.instance, data)
		}

		this.modalElement = document.createElement('div')
		this.modalElement.classList.add('modal')
		this.modalElement.innerHTML = `
      <div class="${size} modal-content" @modalAnimation>
        <button class="close">×</button>
      </div>
    `

		const contentDiv = this.modalElement.querySelector('.modal-content')!
		contentDiv.appendChild(this.modalRef.location.nativeElement)

		document.body.appendChild(this.modalElement)
		this.appRef.attachView(this.modalRef.hostView)

		this.modalElement.querySelector('.close')!.addEventListener('click', () => this.attemptClose())
		this.modalElement.addEventListener('click', (e) => {
			if (e.target === this.modalElement) this.attemptClose()
		})
	}

	private attemptClose(onConfirm?: () => void): void {
		const instance = this.modalRef?.instance as any
		const form = instance?.form

		if (form instanceof FormGroup && form.touched && !this.isConfirming) {
			this.isConfirming = true

			this.notificationService.showConfirm(
				'Você tem alterações não salvas. Deseja fechar mesmo assim?',
				() => {
					this.isConfirming = false
					this.close()
					if (onConfirm) onConfirm()
				},
				() => {
					this.isConfirming = false
				}
			)
			return
		}

		this.close()
		if (onConfirm) onConfirm()
	}

	close(): void {
		LoggerService.info('Closing modal')
		if (this.modalRef) {
			this.appRef.detachView(this.modalRef.hostView)
			this.modalRef.destroy()
			this.modalRef = undefined
		}

		if (this.modalElement) {
			this.modalElement.remove()
			this.modalElement = undefined
		}
	}
}
