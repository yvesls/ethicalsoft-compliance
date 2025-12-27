import { ApplicationRef, ComponentRef, Injectable, OnDestroy, Type, createComponent, inject } from '@angular/core'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { FormGroup } from '@angular/forms'
import { NavigationExtras, NavigationStart, Router } from '@angular/router'
import { Observable, Subject } from 'rxjs'
import { filter } from 'rxjs/operators'
import { NotificationService } from './notification.service'
import { LoggerService } from './logger.service'

export type ModalSize = 'very-small-card' | 'small-card' | 'medium-card' | 'large-card'

@Injectable({ providedIn: 'root' })
export class ModalService implements OnDestroy {
	private readonly appRef = inject(ApplicationRef)
	private readonly notificationService = inject(NotificationService)
	private readonly router = inject(Router)

	private modalRef: ComponentRef<unknown> | null = null
	private modalElement: HTMLElement | null = null
	private isConfirming = false
	private pendingNavigation?: PendingNavigation
	private modalClosedSubject = new Subject<void>()

	constructor() {
		this.router.events
			.pipe(filter((event): event is NavigationStart => event instanceof NavigationStart), takeUntilDestroyed())
			.subscribe((event) => {
				if (this.modalRef) {
					this.handleNavigation(event)
				}
			})
	}

	ngOnDestroy(): void {
		this.modalClosedSubject.complete()
	}

	get modalClosed$(): Observable<void> {
		return this.modalClosedSubject.asObservable()
	}

	getActiveInstance<TInstance>(): TInstance | null {
		return (this.modalRef?.instance as TInstance) ?? null
	}

	private handleNavigation(event: NavigationStart) {
		const form = this.getModalForm()

		if (form?.touched && !this.isConfirming) {
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

	open<T extends object>(component: Type<T>, size: ModalSize = 'small-card', data?: Partial<T>): void {
		LoggerService.info('Opening modal with component:', component)
		if (this.modalRef) {
			this.attemptClose(() => this.open(component, size, data))
			return
		}

		this.modalRef = createComponent(component, { environmentInjector: this.appRef.injector })

		if (data && this.modalRef.instance) {
			Object.assign(this.modalRef.instance as object, data)
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
		const form = this.getModalForm()

		if (form?.touched && !this.isConfirming) {
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
			this.modalRef = null
		}

		if (this.modalElement) {
			this.modalElement.remove()
			this.modalElement = null
		}

		// Emite evento de modal fechado
		this.modalClosedSubject.next()
	}

	private getModalForm(): FormGroup | null {
		const instance = this.modalRef?.instance as ModalComponentInstance | undefined
		const form = instance?.form
		return form instanceof FormGroup ? form : null
	}
}

interface PendingNavigation {
	url: string
	extras?: NavigationExtras
}

interface ModalComponentInstance {
	form?: FormGroup
}
