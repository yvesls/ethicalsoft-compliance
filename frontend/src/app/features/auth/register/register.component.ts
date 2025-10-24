import { RegisterInterface } from './../../../shared/interfaces/auth/register.interface'
import { Component } from '@angular/core'
import { CommonModule } from '@angular/common'
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms'
import { InputComponent } from '../../../shared/components/input/input.component'
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component'
import { AuthStore } from '../../../shared/stores/auth.store'
import { createRegister } from '../../../shared/interfaces/auth/register.interface'
import { NotificationService } from '../../../core/services/notification.service'
import { ModalService } from '../../../core/services/modal.service'
import { TermsComponent } from '../terms/terms.component'
import { NavigateParams } from '../../../core/services/router.service'
import { CustomValidators } from '../../../shared/validators/custom.validator'

@Component({
	selector: 'app-register',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule, InputComponent],
	templateUrl: './register.component.html',
	styleUrls: ['./register.component.scss'],
})
export class RegisterComponent extends BasePageComponent {
	form!: FormGroup
	private registerInterface: RegisterInterface = createRegister()

	constructor(
		private formBuilder: FormBuilder,
		private authStore: AuthStore,
		private notificationService: NotificationService,
		private modalService: ModalService
	) {
		super()
	}

	protected override onInit(): void {
		this._initForm()
	}

	protected override save() {
		return {
			formValue: this.form.getRawValue(),
		}
	}

	protected override restore(restoreParameter: RestoreParams<any>): void {
		if (!restoreParameter.hasParams) {
			return
		}

		if (restoreParameter['formValue']) {
			this.form.patchValue(restoreParameter['formValue'])
		}
	}

	protected override loadParams(params: any, queryParams?: any): void {}

	private _initForm() {
		this.form = this.formBuilder.group(
			{
				firstName: ['', Validators.required],
				lastName: ['', Validators.required],
				email: ['', Validators.compose([Validators.email, Validators.required])],
				password: ['', Validators.compose([Validators.required, CustomValidators.passwordValidator()])],
				confirmPassword: ['', Validators.required],
				acceptedTerms: [false, Validators.requiredTrue],
			},
			{ validators: CustomValidators.passwordMatchValidator() }
		)
	}

	register() {
		if (this.form.valid) {
			const formValue = this.form.getRawValue()
			this.registerInterface.firstName = formValue.firstName
			this.registerInterface.lastName = formValue.lastName
			this.registerInterface.email = formValue.email
			this.registerInterface.password = formValue.password
			this.registerInterface.acceptedTerms = formValue.acceptedTerms

			this.authStore.register(this.registerInterface).subscribe({
				next: () => {
					this.notificationService.showSuccess('Registrado com sucesso.')
					this.routerService.navigateTo('login')
				},
				error: (error) => {
					this.notificationService.showError(error)
				},
			})
		}
	}

	goToLogin() {
		this.routerService.navigateTo('login', {} as NavigateParams<any>, this.form.touched)
	}

	openTermsAndPolicies(event: MouseEvent) {
		event.stopPropagation()
		event.preventDefault()
		this.modalService.open(TermsComponent, 'small-card')
	}
}
