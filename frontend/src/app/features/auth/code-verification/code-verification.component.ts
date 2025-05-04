import { Component, QueryList, ViewChildren, ElementRef } from '@angular/core'
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms'
import { NotificationService } from '../../../core/services/notification.service'
import { CommonModule } from '@angular/common'
import { AuthStore } from '../../../shared/stores/auth.store'
import { createValidateCode, ValidateCodeInterface } from '../../../shared/interfaces/auth/validate-code.interface'
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component'

@Component({
	selector: 'app-code-verification',
	imports: [CommonModule, ReactiveFormsModule],
	templateUrl: './code-verification.component.html',
	styleUrls: ['./code-verification.component.scss'],
})
export class CodeVerificationComponent extends BasePageComponent {
	form!: FormGroup
	@ViewChildren('codeInput') codeInputs!: QueryList<ElementRef>
	private validateCode: ValidateCodeInterface = createValidateCode()

	constructor(
		private formBuilder: FormBuilder,
		private notificationService: NotificationService,
		private authStore: AuthStore
	) {
		super()
	}

	protected override onInit(): void {
		this.initForm()
	}

	protected override save() {
		return {
			validateCode: this.validateCode,
		}
	}

	protected override restore(restoreParameter: RestoreParams<any>): void {
		if (!restoreParameter.hasParams) {
			return
		}

		if (restoreParameter['validateCode']) {
			this.validateCode = restoreParameter['validateCode']
		}
	}

	protected override loadParams(params: any, queryParams?: any): void {
		if (!params) return

		if (!this.validateCode) {
			this.validateCode = createValidateCode()
		}

		if (!!params.email) {
			this.validateCode.email = params.email
		}
	}

	private initForm() {
		const codeControls = Array(6)
			.fill('')
			.map(() => this.formBuilder.control('', Validators.required))
		this.form = this.formBuilder.group({
			code: this.formBuilder.array(codeControls),
		})
	}

	get codeControls(): FormArray {
		return this.form.get('code') as FormArray
	}

	onInput(event: Event, index: number) {
		const input = event.target as HTMLInputElement
		if (input.value.length === 1 && index < 5) {
			const nextInput = this.codeInputs.toArray()[index + 1]?.nativeElement
			nextInput?.focus()
		}
	}

	onPaste(event: ClipboardEvent) {
		const pastedText = event.clipboardData?.getData('text') || ''
		const codeArray = pastedText.split('')

		codeArray.forEach((char, i) => {
			if (this.codeControls.controls[i]) {
				this.codeControls.controls[i].setValue(char)
			}
		})

		event.preventDefault()
	}

	verifyCode() {
		const code = this.codeControls.value.join('')
		this.validateCode.code = code

		this.authStore.validateCode(this.validateCode).subscribe({
			next: () => {
				this.notificationService.showSuccess('Código de recuperação validado com sucesso.')
				this.routerService.navigateTo('reset-password', {
					params: {
						email: this.validateCode.email,
					},
				})
			},
			error: (error) => {
				this.notificationService.showError(error)
			},
		})
	}
}
