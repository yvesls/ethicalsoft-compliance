import { Component, inject, ChangeDetectionStrategy } from '@angular/core'
import {
	AbstractControl,
	FormBuilder,
	FormControl,
	FormGroup,
	ReactiveFormsModule,
	ValidationErrors,
	Validators,
} from '@angular/forms'
import { Params } from '@angular/router'
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component'
import { RouteParams } from '../../../core/services/router.service'
import { NotificationService } from '../../../core/services/notification.service'
import { TranslateModule, TranslateService } from '@ngx-translate/core'
import { InputComponent } from '../../../shared/components/input/input.component'
import { AuthStore } from '../../../shared/stores/auth.store'
import { createResetPassword, ResetPasswordInterface } from '../../../shared/interfaces/auth/reset-password.interface'

type ResetPasswordFormGroup = FormGroup<{
	password: FormControl<string>
	confirmPassword: FormControl<string>
}>

interface ResetPasswordRouteParams extends Record<string, unknown> {
	formValue?: ResetPasswordFormValue
	resetPassword?: ResetPasswordInterface
	email?: string
}

interface ResetPasswordFormValue {
	password: string
	confirmPassword: string
}

@Component({
	selector: 'app-reset-password',
	standalone: true,
	imports: [ReactiveFormsModule, InputComponent, TranslateModule],
	templateUrl: './reset-password.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent extends BasePageComponent<ResetPasswordRouteParams> {
	form!: ResetPasswordFormGroup
	resetPassword: ResetPasswordInterface = createResetPassword()
	private readonly formBuilder = inject(FormBuilder)
	private readonly authStore = inject(AuthStore)
	private readonly notificationService = inject(NotificationService)
	private readonly translate = inject(TranslateService)

	get passwordValidationMessages() {
		return {
			required: this.translate.instant('auth.validation.password_required'),
			minlength: this.translate.instant('auth.validation.password_min_length'),
			weakPassword: this.translate.instant('auth.errors.weak_password'),
		}
	}

	get confirmPasswordValidationMessages() {
		return {
			required: this.translate.instant('auth.validation.confirm_password_required'),
			passwordsMismatch: this.translate.instant('auth.errors.passwords_do_not_match'),
		}
	}

	protected override onInit(): void {
		this._initForm()
	}

	protected override save(): RouteParams<ResetPasswordRouteParams> {
		return {
			formValue: this.form.getRawValue(),
			resetPassword: this.resetPassword,
			email: this.resetPassword.email,
		}
	}

	protected override restore(restoreParameter: RestoreParams<ResetPasswordRouteParams>): void {
		if (!restoreParameter.hasParams) {
			return
		}

		const storedFormValue = restoreParameter['formValue']
		if (isResetPasswordFormValue(storedFormValue)) {
			this.form.patchValue(storedFormValue)
		}

		const storedResetPassword = restoreParameter['resetPassword']
		if (isResetPasswordInterface(storedResetPassword)) {
			this.resetPassword = storedResetPassword
		}
	}

	protected override loadParams(params: RouteParams<ResetPasswordRouteParams>, queryParams?: Params): void {
		const storedFormValue = params['formValue']
		if (isResetPasswordFormValue(storedFormValue)) {
			this.form.patchValue(storedFormValue)
		}

		const storedResetPassword = params['resetPassword']
		if (isResetPasswordInterface(storedResetPassword)) {
			this.resetPassword = storedResetPassword
		}

		const emailParam = this.getEmailFromParams(params, queryParams)
		if (emailParam) {
			this.resetPassword.email = emailParam
		}
	}

	private _initForm(): void {
		this.form = this.formBuilder.nonNullable.group(
			{
				password: this.formBuilder.nonNullable.control('', [
					Validators.required,
					Validators.minLength(8),
					(control) => this.passwordStrengthValidator(control),
				]),
				confirmPassword: this.formBuilder.nonNullable.control('', [Validators.required]),
			},
			{
				validators: (group) => this.passwordsMatchValidator(group),
			}
		)
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

		return hasUpperCase && hasLowerCase && hasNumber && hasSpecial ? null : { weakPassword: true }
	}

	private passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
		if (!(control instanceof FormGroup)) {
			return null
		}
		const password = control.get('password')?.value
		const confirmPassword = control.get('confirmPassword')?.value
		return password === confirmPassword ? null : { passwordsMismatch: true }
	}

	reset(): void {
		if (!this.form.valid) {
			this.form.markAllAsTouched()
			return
		}
		const { password } = this.form.getRawValue()
		this.resetPassword.newPassword = password

		this.authStore.resetPassword(this.resetPassword).subscribe({
			next: () => {
				this.notificationService.showSuccess(this.translate.instant('auth.messages.password_reset'))
				this.routerService.navigateTo('login')
			},
			error: (error: unknown) => {
				this.notificationService.showError(error)
			},
		})
	}

	private getEmailFromParams(
		params: RouteParams<ResetPasswordRouteParams>,
		queryParams?: Params
	): string | undefined {
		const emailFromParams = typeof params['email'] === 'string' ? params['email'] : undefined
		const emailFromPayload = typeof params.p?.email === 'string' ? params.p.email : undefined
		const emailFromQuery = typeof queryParams?.['email'] === 'string' ? queryParams['email'] : undefined
		return emailFromParams ?? emailFromPayload ?? emailFromQuery
	}
}

function isResetPasswordFormValue(value: unknown): value is ResetPasswordFormValue {
	return (
		typeof value === 'object' &&
		value !== null &&
		'password' in value &&
		typeof (value as ResetPasswordFormValue).password === 'string' &&
		'confirmPassword' in value &&
		typeof (value as ResetPasswordFormValue).confirmPassword === 'string'
	)
}

function isResetPasswordInterface(value: unknown): value is ResetPasswordInterface {
	return (
		typeof value === 'object' &&
		value !== null &&
		'email' in value &&
		typeof (value as ResetPasswordInterface).email === 'string'
	)
}
