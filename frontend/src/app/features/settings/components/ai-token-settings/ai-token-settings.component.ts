import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core'

import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms'
import { TranslateModule, TranslateService } from '@ngx-translate/core'
import { AiTokenService } from '../../../../core/ai/ai-token.service'
import { AiTokenStatus } from '../../../../core/ai/ai-token.types'
import { NotificationService } from '../../../../core/services/notification.service'

@Component({
	selector: 'app-ai-token-settings',
	standalone: true,
	imports: [ReactiveFormsModule, TranslateModule],
	templateUrl: './ai-token-settings.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './ai-token-settings.component.scss',
})
export class AiTokenSettingsComponent implements OnInit {
	private readonly fb = inject(FormBuilder)
	private readonly aiTokenService = inject(AiTokenService)
	private readonly notificationService = inject(NotificationService)
	private readonly translate = inject(TranslateService)

	status = signal<AiTokenStatus | null>(null)
	saving = signal(false)
	removing = signal(false)
	showToken = signal(false)

	form: FormGroup = this.fb.group({
		token: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(512)]],
	})

	ngOnInit(): void {
		this.aiTokenService.status$.subscribe((s) => this.status.set(s))
		if (!this.aiTokenService.currentStatus) {
			this.aiTokenService.loadStatus().subscribe()
		}
	}

	toggleShowToken(): void {
		this.showToken.update((v) => !v)
	}

	save(): void {
		if (this.form.invalid || this.saving()) return
		const token = this.form.get('token')?.value as string
		this.saving.set(true)
		this.aiTokenService.updateToken(token).subscribe({
			next: () => {
				this.notificationService.showSuccess(this.translate.instant('ai.token.saved'))
				this.form.reset()
				this.saving.set(false)
			},
			error: (err) => {
				const msg =
					err?.error?.translatedMessage ??
					err?.error?.message ??
					this.translate.instant('ai.token.errors.not_configured')
				this.notificationService.showError(msg)
				this.saving.set(false)
			},
		})
	}

	remove(): void {
		this.notificationService.showConfirm(this.translate.instant('ai.token.form.remove'), () => {
			this.removing.set(true)
			this.aiTokenService.deleteToken().subscribe({
				next: () => {
					this.notificationService.showSuccess(this.translate.instant('ai.token.removed'))
					this.removing.set(false)
				},
				error: () => {
					this.notificationService.showError(this.translate.instant('ai.token.errors.not_configured'))
					this.removing.set(false)
				},
			})
		})
	}
}
