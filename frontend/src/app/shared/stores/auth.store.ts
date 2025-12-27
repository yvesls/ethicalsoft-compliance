import { Injectable } from '@angular/core'
import { Observable } from 'rxjs'
import { BaseStore } from './base/base.store'
import { AuthInterface } from '../interfaces/auth/auth.interface'
import { AuthTokenInterface } from '../interfaces/auth/auth-token.interface'
import { AuthRefreshTokenInterface } from '../interfaces/auth/auth-refresh-token.interface'
import { ValidateCodeInterface } from '../interfaces/auth/validate-code.interface'
import { PasswordRecoveryInterface } from '../interfaces/auth/password-recovery.interface'
import { ResetPasswordInterface } from '../interfaces/auth/reset-password.interface'
import { RegisterInterface } from '../interfaces/auth/register.interface'

@Injectable({
	providedIn: 'root',
})
export class AuthStore extends BaseStore {
	constructor() {
		super('auth')
	}

	token(inputLogin: AuthInterface): Observable<AuthTokenInterface> {
		return this.requestService.makePost(this.getUrl('token'), { data: inputLogin })
	}

  logout(refreshToken: string): Observable<void> {
    return this.requestService.makePost(this.getUrl('logout'), {
      data: { refreshToken }
    });
  }

	refreshToken(refreshToken: AuthRefreshTokenInterface): Observable<AuthTokenInterface> {
		return this.requestService.makePost(this.getUrl('refresh-token'), { data: refreshToken })
	}

	checkToken(): Observable<string> {
		return this.requestService.makeGet(this.getUrl('check-token'), { useAuth: false })
	}

	register(register: RegisterInterface): Observable<void> {
		return this.requestService.makePost(this.getUrl('register'), { data: register, useAuth: false })
	}

	recover(passwordRecovery: PasswordRecoveryInterface): Observable<void> {
		return this.requestService.makePost(this.getUrl('recover-account'), { data: passwordRecovery })
	}

	validateCode(validateCode: ValidateCodeInterface): Observable<void> {
		return this.requestService.makePost(this.getUrl('validate-code'), { data: validateCode })
	}

	resetPassword(resetPassword: ResetPasswordInterface): Observable<void> {
		return this.requestService.makePost(this.getUrl('reset-password'), { data: resetPassword })
	}
}
