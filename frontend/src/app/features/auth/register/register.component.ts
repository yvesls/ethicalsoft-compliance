import { CommonModule } from '@angular/common'
import { Component, inject } from '@angular/core'
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms'
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component'
import { RouteParams } from '../../../core/services/router.service'
import { ModalService } from '../../../core/services/modal.service'
import { NotificationService } from '../../../core/services/notification.service'
import { AuthStore } from '../../../shared/stores/auth.store'
import { InputComponent } from '../../../shared/components/input/input.component'
import { CustomValidators } from '../../../shared/validators/custom.validator'
import { createRegister, RegisterInterface } from '../../../shared/interfaces/auth/register.interface'
import { TermsComponent } from '../terms/terms.component'

type RegisterFormGroup = FormGroup<{
	firstName: FormControl<string>
	lastName: FormControl<string>
	email: FormControl<string>
	password: FormControl<string>
	confirmPassword: FormControl<string>
	acceptedTerms: FormControl<boolean>
}>

interface RegisterRouteParams extends Record<string, unknown> {
	formValue?: RegisterFormValue
}

interface RegisterFormValue {
	firstName: string
	lastName: string
	email: string
	password: string
	confirmPassword: string
	acceptedTerms: boolean
}

@Component({
	selector: 'app-register',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule, InputComponent],
	templateUrl: './register.component.html',
	styleUrls: ['./register.component.scss'],
})
export class RegisterComponent extends BasePageComponent<RegisterRouteParams> {
	form!: RegisterFormGroup
	private readonly formBuilder = inject(FormBuilder)
	private readonly authStore = inject(AuthStore)
	private readonly notificationService = inject(NotificationService)
	private readonly modalService = inject(ModalService)

	protected override onInit(): void {
		this._initForm()
	}

	protected override save(): RouteParams<RegisterRouteParams> {
		return {
			formValue: this.form.getRawValue(),
		}
	}

	protected override restore(restoreParameter: RestoreParams<RegisterRouteParams>): void {
		if (!restoreParameter.hasParams) {
			return
		}

		const storedFormValue = restoreParameter['formValue']
		if (isRegisterFormValue(storedFormValue)) {
			this.form.patchValue(storedFormValue)
		}
	}

	protected override loadParams(params: RouteParams<RegisterRouteParams>): void {
		const storedFormValue = params['formValue']
		if (isRegisterFormValue(storedFormValue)) {
			this.form.patchValue(storedFormValue)
		}
	}

	private _initForm(): void {
		this.form = this.formBuilder.nonNullable.group(
			{
				firstName: this.formBuilder.nonNullable.control('', [Validators.required]),
				lastName: this.formBuilder.nonNullable.control('', [Validators.required]),
				email: this.formBuilder.nonNullable.control('', [Validators.required, Validators.email]),
				password: this.formBuilder.nonNullable.control('', [Validators.required, CustomValidators.passwordValidator()]),
				confirmPassword: this.formBuilder.nonNullable.control('', [Validators.required]),
				acceptedTerms: this.formBuilder.nonNullable.control(false, [Validators.requiredTrue]),
			},
			{ validators: CustomValidators.passwordMatchValidator() }
		)
	}

	register(): void {
		if (!this.form.valid) {
			this.form.markAllAsTouched()
			return
		}
		const formValue = this.form.getRawValue()
		const registerPayload: RegisterInterface = {
			...createRegister(),
			firstName: formValue.firstName,
			lastName: formValue.lastName,
			email: formValue.email,
			password: formValue.password,
			acceptedTerms: formValue.acceptedTerms,
		}

		this.authStore.register(registerPayload).subscribe({
			next: () => {
				this.notificationService.showSuccess('Registrado com sucesso.')
				this.routerService.navigateTo('login')
			},
			error: (error: unknown) => {
				this.notificationService.showError(error)
			},
		})
	}

	goToLogin(): void {
		this.routerService.navigateTo('login', undefined, this.form.touched)
	}

	openTermsAndPolicies(event: MouseEvent): void {
		event.stopPropagation()
		event.preventDefault()
		this.modalService.open(TermsComponent, 'small-card')
	}
}

function isRegisterFormValue(value: unknown): value is RegisterFormValue {
	return (
		typeof value === 'object' &&
		value !== null &&
		'firstName' in value &&
		typeof (value as RegisterFormValue).firstName === 'string' &&
		'lastName' in value &&
		typeof (value as RegisterFormValue).lastName === 'string' &&
		'email' in value &&
		typeof (value as RegisterFormValue).email === 'string' &&
		'password' in value &&
		typeof (value as RegisterFormValue).password === 'string' &&
		'confirmPassword' in value &&
		typeof (value as RegisterFormValue).confirmPassword === 'string' &&
		'acceptedTerms' in value &&
		typeof (value as RegisterFormValue).acceptedTerms === 'boolean'
	)
}
