import { CommonModule } from '@angular/common'
import { Component, inject } from '@angular/core'
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
	imports: [CommonModule, ReactiveFormsModule, InputComponent],
	templateUrl: './reset-password.component.html',
	styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent extends BasePageComponent<ResetPasswordRouteParams> {
	form!: ResetPasswordFormGroup
	resetPassword: ResetPasswordInterface = createResetPassword()
	private readonly formBuilder = inject(FormBuilder)
	private readonly authStore = inject(AuthStore)
	private readonly notificationService = inject(NotificationService)

	readonly passwordValidationMessages = {
		required: 'Senha é obrigatória',
		minlength: 'A senha deve ter pelo menos 8 caracteres',
		weakPassword:
			'A senha precisa ter letras maiúsculas, minúsculas, números e caracteres especiais',
	}

	readonly confirmPasswordValidationMessages = {
		required: 'Confirmação de senha é obrigatória',
		passwordsMismatch: 'As senhas não coincidem',
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

		return hasUpperCase && hasLowerCase && hasNumber && hasSpecial
			? null
			: { weakPassword: true }
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
				this.notificationService.showSuccess('Senha redefinida com sucesso!')
				this.routerService.navigateTo('login')
			},
			error: (error: unknown) => {
				this.notificationService.showError(error)
			},
		})
	}

	private getEmailFromParams(params: RouteParams<ResetPasswordRouteParams>, queryParams?: Params): string | undefined {
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
