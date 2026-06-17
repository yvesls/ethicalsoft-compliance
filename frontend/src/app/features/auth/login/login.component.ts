import { CommonModule } from '@angular/common'
import { AfterViewInit, Component, ElementRef, inject, ViewChild } from '@angular/core'
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms'
import { BasePageComponent, RestoreParams } from '../../../core/abstractions/base-page.component'
import { RouteParams } from '../../../core/services/router.service'
import { AuthenticationService } from '../../../core/services/authentication.service'
import { GoogleAuthService } from '../../../core/services/google-auth.service'
import { AuthInterface } from '../../../shared/interfaces/auth/auth.interface'
import { TranslateModule } from '@ngx-translate/core'
import { InputComponent } from '../../../shared/components/input/input.component'

type LoginFormGroup = FormGroup<{
	username: FormControl<string>
	password: FormControl<string>
	keepSession: FormControl<boolean>
}>

interface LoginRouteParams extends Record<string, unknown> {
	formValue?: AuthInterface & { keepSession: boolean }
}

@Component({
	selector: 'app-login',
	standalone: true,
	imports: [CommonModule, ReactiveFormsModule, InputComponent, TranslateModule],
	templateUrl: './login.component.html',
	styleUrls: ['./login.component.scss'],
})
export class LoginComponent extends BasePageComponent<LoginRouteParams> implements AfterViewInit {
	@ViewChild('googleBtn') googleBtnRef!: ElementRef<HTMLDivElement>

	form!: LoginFormGroup
	private readonly formBuilder = inject(FormBuilder)
	private readonly authService = inject(AuthenticationService)
	private readonly googleAuthService = inject(GoogleAuthService)
	private googleCredentialSub?: ReturnType<typeof this.googleAuthService.credential$.subscribe>

	protected override onInit(): void {
		this._initForm()
	}

	ngAfterViewInit(): void {
		this.googleCredentialSub = this.googleAuthService.credential$.subscribe(idToken => {
			const keepSession = this.form.get('keepSession')?.value ?? false
			this.authService.googleLogin(idToken, keepSession)
		})
		this.googleAuthService.renderButton(this.googleBtnRef.nativeElement)
	}

	override ngOnDestroy(): void {
		this.googleCredentialSub?.unsubscribe()
		super.ngOnDestroy()
	}

	protected override save(): RouteParams<LoginRouteParams> {
		return {
			formValue: this.form.getRawValue(),
		}
	}

	protected override restore(restoreParameter: RestoreParams<LoginRouteParams>): void {
		if (!restoreParameter.hasParams) {
			return
		}

		const storedFormValue = restoreParameter['formValue']
		if (isLoginFormValue(storedFormValue)) {
			this.form.patchValue(storedFormValue)
		}
	}

	protected override loadParams(params: RouteParams<LoginRouteParams>): void {
		const storedFormValue = params['formValue']
		if (isLoginFormValue(storedFormValue)) {
			this.form.patchValue(storedFormValue)
		}
	}

	private _initForm(): void {
		this.form = this.formBuilder.nonNullable.group({
			username: this.formBuilder.nonNullable.control('', [Validators.required, Validators.email]),
			password: this.formBuilder.nonNullable.control('', [Validators.required]),
			keepSession: this.formBuilder.nonNullable.control(false),
		})
	}

	login(): void {
		if (!this.form.valid) {
			this.form.markAllAsTouched()
			return
		}
		const { username, password, keepSession } = this.form.getRawValue()
		const credentials: AuthInterface = { username, password }
		this.authService.login(credentials, keepSession)
	}

	goToRecover(): void {
		this.routerService.navigateTo('recover-account')
	}

	goToRegister(): void {
		this.routerService.navigateTo('register')
	}
}

function isLoginFormValue(value: unknown): value is AuthInterface & { keepSession: boolean } {
	return (
		typeof value === 'object' &&
		value !== null &&
		'username' in value &&
		typeof (value as AuthInterface).username === 'string' &&
		'password' in value &&
		typeof (value as AuthInterface).password === 'string' &&
		'keepSession' in value &&
		typeof (value as { keepSession: boolean }).keepSession === 'boolean'
	)
}
