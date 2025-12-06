import { CommonModule } from '@angular/common'
import { Component, ElementRef, QueryList, ViewChildren, inject } from '@angular/core'
import {
	FormArray,
	FormBuilder,
	FormControl,
	FormGroup,
	ReactiveFormsModule,
	Validators,
} from '@angular/forms'
import { Params } from '@angular/router'
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component'
import { NotificationService } from '../../../core/services/notification.service'
import { RouteParams } from '../../../core/services/router.service'
import { AuthStore } from '../../../shared/stores/auth.store'
import { createValidateCode, ValidateCodeInterface } from '../../../shared/interfaces/auth/validate-code.interface'

type CodeControl = FormControl<string>
type CodeControlArray = FormArray<CodeControl>
type CodeVerificationForm = FormGroup<{ code: CodeControlArray }>

interface CodeVerificationParams extends Record<string, unknown> {
	email?: string
	validateCode?: ValidateCodeInterface
}

@Component({
	selector: 'app-code-verification',
	imports: [CommonModule, ReactiveFormsModule],
	templateUrl: './code-verification.component.html',
	styleUrls: ['./code-verification.component.scss'],
})
export class CodeVerificationComponent extends BasePageComponent<CodeVerificationParams> {
	form!: CodeVerificationForm
	@ViewChildren('codeInput') codeInputs!: QueryList<ElementRef<HTMLInputElement>>
	private readonly formBuilder = inject(FormBuilder)
	private readonly notificationService = inject(NotificationService)
	private readonly authStore = inject(AuthStore)
	private validateCode: ValidateCodeInterface = createValidateCode()

	protected override onInit(): void {
		this.initForm()
	}

	protected override save(): RouteParams<CodeVerificationParams> {
		return {
			email: this.validateCode.email,
			validateCode: this.validateCode,
		}
	}

	protected override restore(restoreParameter: RestoreParams<CodeVerificationParams>): void {
		if (!restoreParameter.hasParams) {
			return
		}

		const storedValidateCode = restoreParameter['validateCode']
		if (isValidateCode(storedValidateCode)) {
			this.validateCode = storedValidateCode
			const restoredCode = storedValidateCode.code.split('').slice(0, this.codeControls.length)
			for (const [index, char] of restoredCode.entries()) {
				this.codeControls.at(index)?.setValue(char)
			}
		}
	}

	protected override loadParams(params: RouteParams<CodeVerificationParams>, queryParams?: Params): void {
		const emailParam = this.extractEmail(params, queryParams)
		if (emailParam) {
			this.validateCode.email = emailParam
		}
	}

	private initForm(): void {
		const codeControls = Array.from({ length: 6 }, () =>
			this.formBuilder.control('', {
				nonNullable: true,
				validators: Validators.required,
			})
		)
		const codeArray = this.formBuilder.array<CodeControl>(codeControls)
		this.form = this.formBuilder.group({
			code: codeArray,
		})
	}

	get codeControls(): CodeControlArray {
		return this.form.get('code') as CodeControlArray
	}

	onInput(event: Event, index: number): void {
		if (!(event.target instanceof HTMLInputElement)) {
			return
		}
		if (event.target.value.length === 1 && index < 5) {
			const nextInput = this.codeInputs.toArray()[index + 1]?.nativeElement
			nextInput?.focus()
		}
	}

	onPaste(event: ClipboardEvent): void {
		const pastedText = event.clipboardData?.getData('text') || ''
		const codeArray = pastedText.split('')

		for (const [index, char] of codeArray.entries()) {
			this.codeControls.at(index)?.setValue(char)
		}

		event.preventDefault()
	}

	verifyCode(): void {
		const code = this.codeControls.getRawValue().join('')
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

	private extractEmail(params: RouteParams<CodeVerificationParams>, queryParams?: Params): string | undefined {
		const emailFromParams = typeof params['email'] === 'string' ? params['email'] : undefined
		const emailFromPayload = typeof params.p?.email === 'string' ? params.p.email : undefined
		const emailFromQuery = typeof queryParams?.['email'] === 'string' ? queryParams['email'] : undefined
		return emailFromParams ?? emailFromPayload ?? emailFromQuery
	}
}

function isValidateCode(value: unknown): value is ValidateCodeInterface {
	return (
		typeof value === 'object' &&
		value !== null &&
		'email' in value &&
		typeof (value as ValidateCodeInterface).email === 'string' &&
		'code' in value &&
		typeof (value as ValidateCodeInterface).code === 'string'
	)
}
