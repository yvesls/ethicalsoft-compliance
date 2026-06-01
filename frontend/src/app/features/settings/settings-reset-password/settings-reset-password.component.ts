import { CommonModule } from '@angular/common'
import { Component, OnInit, inject } from '@angular/core'
import {
	AbstractControl,
	FormBuilder,
	FormControl,
	FormGroup,
	ReactiveFormsModule,
	ValidationErrors,
	Validators,
} from '@angular/forms'
import { RouterModule } from '@angular/router'
import { catchError, finalize, switchMap, throwError } from 'rxjs'
import { InputComponent } from '../../../shared/components/input/input.component'
import { NotificationService } from '../../../core/services/notification.service'
import { AuthStore } from '../../../shared/stores/auth.store'
import { AuthenticationService } from '../../../core/services/authentication.service'
import { RouterService } from '../../../core/services/router.service'
import { ModalService } from '../../../core/services/modal.service'
import { FirstAccessModalComponent } from '../components/first-access-modal/first-access-modal.component'
import { AuthInterface } from '../../../shared/interfaces/auth/auth.interface'
import { TermsComponent } from '../../auth/terms/terms.component'
import { TranslateModule, TranslateService } from '@ngx-translate/core'

type ResetPasswordForm = FormGroup<{
	currentPassword: FormControl<string>
	newPassword: FormControl<string>
	confirmPassword: FormControl<string>
	acceptedTerms: FormControl<boolean>
}>

@Component({
	selector: 'app-settings-reset-password',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule, RouterModule, InputComponent, TranslateModule],
	templateUrl: './settings-reset-password.component.html',
	styleUrl: './settings-reset-password.component.scss',
})
export class SettingsResetPasswordComponent implements OnInit {
	private readonly formBuilder = inject(FormBuilder)
	private readonly authStore = inject(AuthStore)
	private readonly notificationService = inject(NotificationService)
	private readonly authenticationService = inject(AuthenticationService)
	private readonly routerService = inject(RouterService)
	private readonly modalService = inject(ModalService)
	private readonly translate = inject(TranslateService)

	private readonly invalidCurrentPasswordError = 'INVALID_CURRENT_PASSWORD'

	showTerms = false

	form: ResetPasswordForm = this.formBuilder.nonNullable.group(
		{
			currentPassword: this.formBuilder.nonNullable.control('', [Validators.required]),
			newPassword: this.formBuilder.nonNullable.control('', [
				Validators.required,
				Validators.minLength(8),
				(control) => this.passwordStrengthValidator(control),
			]),
			confirmPassword: this.formBuilder.nonNullable.control('', [Validators.required]),
			acceptedTerms: this.formBuilder.nonNullable.control(false, [Validators.requiredTrue]),
		},
		{ validators: (group) => this.passwordsMatchValidator(group) }
	)

	isSubmitting = false

	get passwordValidationMessages() {
		return {
			required: this.translate.instant('settings.validation.new_password_required'),
			minlength: this.translate.instant('settings.validation.min_length'),
			weakPassword: this.translate.instant('settings.validation.weak_password'),
		}
	}

	get confirmPasswordValidationMessages() {
		return {
			required: this.translate.instant('settings.validation.confirm_required'),
			passwordsMismatch: this.translate.instant('settings.validation.passwords_mismatch'),
		}
	}

	get currentPasswordValidationMessages() {
		return {
			required: this.translate.instant('settings.validation.current_required'),
		}
	}

	ngOnInit(): void {
		this.showFirstAccessModal()
		this.configureTermsVisibility()
	}

	private configureTermsVisibility(): void {
		this.showTerms = this.authenticationService.isFirstAccessPending()

		if (!this.showTerms) {
			const control = this.form.get('acceptedTerms')
			control?.clearValidators()
			control?.setValue(true, { emitEvent: false })
			control?.updateValueAndValidity({ emitEvent: false })
		}
	}

	resetPassword(): void {
		if (!this.form.valid) {
			this.form.markAllAsTouched()
			return
		}

		const user = this.authenticationService.getCurrentUser()
		if (!user?.email) {
			this.notificationService.showError(this.translate.instant('settings.errors.user_not_found'))
			return
		}

		const { newPassword, currentPassword } = this.form.getRawValue()
		this.isSubmitting = true
		this.authStore
			.token(this.buildAuthPayload(user.email, currentPassword))
			.pipe(
				catchError(() => {
					this.notificationService.showError(this.translate.instant('settings.errors.wrong_password'))
					return throwError(() => new Error(this.invalidCurrentPasswordError))
				}),
				switchMap(() =>
					this.authStore.resetPassword({
						email: user.email,
						newPassword,
						firstAccessFlow: true,
					})
				),
				finalize(() => (this.isSubmitting = false))
			)
			.subscribe({
				next: () => {
					this.authenticationService.markFirstAccessCompleted()
					this.notificationService.showSuccess(this.translate.instant('settings.messages.password_updated'))
					this.form.reset({
						currentPassword: '',
						newPassword: '',
						confirmPassword: '',
						acceptedTerms: false,
					})
					this.authenticationService.refreshToken().subscribe({
						complete: () => { void this.routerService.navigateTo('/settings') },
						error: () => { void this.routerService.navigateTo('/settings') },
					})
				},
				error: (error: unknown) => {
					if (!this.isInvalidCurrentPasswordError(error)) {
						this.notificationService.showError(error)
					}
				},
			})
	}

	private passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
		const value = control.value as string
		if (!value) {
			return null
		}

		const hasUpperCase = /[A-Z]/.test(value)
		const hasLowerCase = /[a-z]/.test(value)
		const hasNumber = /\d/.test(value)
		const hasSpecial = /[^A-Za-z0-9]/.test(value)

		return hasUpperCase && hasLowerCase && hasNumber && hasSpecial
			? null
			: { weakPassword: true }
	}

	private passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
		if (!(control instanceof FormGroup)) {
			return null
		}
		const newPassword = control.get('newPassword')?.value
		const confirmPassword = control.get('confirmPassword')?.value
		return newPassword === confirmPassword ? null : { passwordsMismatch: true }
	}

	private showFirstAccessModal(): void {
		if (!this.authenticationService.isFirstAccessPending()) {
			return
		}
		this.modalService.open(FirstAccessModalComponent, 'small-card')
	}

	openTerms(event: MouseEvent): void {
		event.preventDefault()
		event.stopPropagation()
		this.modalService.open(TermsComponent, 'small-card')
	}

	private isInvalidCurrentPasswordError(error: unknown): boolean {
		return (
			typeof error === 'object' &&
			error !== null &&
			'message' in error &&
			(error as Error).message === this.invalidCurrentPasswordError
		)
	}

	private buildAuthPayload(email: string, password: string): AuthInterface {
		return {
			username: email,
			password,
		}
	}
}
