import { CommonModule } from '@angular/common'
import { Component, inject } from '@angular/core'
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms'
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component'
import { RouteParams } from '../../../core/services/router.service'
import { NotificationService } from '../../../core/services/notification.service'
import {
	createPasswordRecovery,
	PasswordRecoveryInterface,
} from '../../../shared/interfaces/auth/password-recovery.interface'
import { AuthStore } from '../../../shared/stores/auth.store'
import { InputComponent } from '../../../shared/components/input/input.component'

type RecoverFormGroup = FormGroup<{ email: FormControl<string> }>

interface RecoverRouteParams extends Record<string, unknown> {
	formValue?: { email: string }
}

@Component({
	selector: 'app-recover',
	imports: [CommonModule, ReactiveFormsModule, InputComponent],
	templateUrl: './recover.component.html',
	styleUrl: './recover.component.scss',
})
	export class RecoverComponent extends BasePageComponent<RecoverRouteParams> {
	form!: RecoverFormGroup
	private passwordRecovery: PasswordRecoveryInterface = createPasswordRecovery()
	private readonly formBuilder = inject(FormBuilder)
	private readonly authStore = inject(AuthStore)
	private readonly notificationService = inject(NotificationService)

	protected override onInit(): void {
		this._initForm()
	}

	protected override save(): RouteParams<RecoverRouteParams> {
		return {
			formValue: this.form.getRawValue(),
		}
	}

	protected override restore(restoreParameter: RestoreParams<RecoverRouteParams>): void {
		if (!restoreParameter.hasParams) {
			return
		}
		const storedFormValue = restoreParameter['formValue']
		if (isRecoverFormValue(storedFormValue)) {
			this.form.patchValue(storedFormValue)
		}
	}

	protected override loadParams(params: RouteParams<RecoverRouteParams>): void {
		const storedFormValue = params['formValue']
		if (isRecoverFormValue(storedFormValue)) {
			this.form.patchValue(storedFormValue)
		}
	}

	private _initForm(): void {
		this.form = this.formBuilder.nonNullable.group({
			email: this.formBuilder.nonNullable.control('', [Validators.required, Validators.email]),
		})
	}

	sendRecoveryEmail(): void {
		if (!this.form.valid) {
			this.form.markAllAsTouched()
			return
		}
		const { email } = this.form.getRawValue()
		this.passwordRecovery.email = email

		this.authStore.recover(this.passwordRecovery).subscribe({
			next: () => {
				this.notificationService.showSuccess(
					'Código de recuperação enviado com sucesso. Cheque seu email e siga as instruções.'
				)
				this.routerService.navigateTo('code-verification', {
					params: {
						email,
					},
				})
			},
			error: (error: unknown) => {
				this.notificationService.showError(error)
			},
		})
	}
}

function isRecoverFormValue(value: unknown): value is { email: string } {
	return typeof value === 'object' && value !== null && 'email' in value && typeof (value as { email: string }).email === 'string'
}
