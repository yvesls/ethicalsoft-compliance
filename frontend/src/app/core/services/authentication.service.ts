import { Injectable, Inject, PLATFORM_ID } from '@angular/core'
import { isPlatformBrowser } from '@angular/common'
import { BehaviorSubject, catchError, map, Observable, of, tap } from 'rxjs'
import { jwtDecode } from 'jwt-decode'
import { AuthStore } from '../../shared/stores/auth.store'
import { AuthTokenInterface } from '../../shared/interfaces/auth/auth-token.interface'
import { AuthInterface } from '../../shared/interfaces/auth/auth.interface'
import { NotificationService } from './notification.service'
import { RouterService } from './router.service'
import { StorageService } from './storage.service'
import { LoggerService } from './logger.service'

@Injectable({
	providedIn: 'root',
})
export class AuthenticationService {
	private _authToken: AuthTokenInterface | null = null
	private _user: UserInterface | null = null
	private refreshTimer: any

	private readonly SESSION_REFRESH_TOKEN_KEY = 'refresh_token_session'
	private readonly KEEP_SESSION_KEY = 'keep_session'

	userRoles$ = new BehaviorSubject<string[]>([])
	isLoggedIn$ = new BehaviorSubject<boolean>(false)

	constructor(
		private authStore: AuthStore,
		private routerService: RouterService,
		private notificationService: NotificationService,
		private storageService: StorageService,
		@Inject(PLATFORM_ID) private platformId: Object
	) {
		this.loadStoredToken()
	}

	isAuthenticated$(): Observable<boolean> {
		return of(this.isAuthenticated())
	}

	isAuthenticated(): boolean {
		if (isPlatformBrowser(this.platformId)) {
			return !!this.getToken()
		}
		return false
	}

	getToken(): string | null {
		if (isPlatformBrowser(this.platformId)) {
			if (this._authToken?.accessToken) {
				return this._authToken.accessToken
			}

			const sessionToken = sessionStorage.getItem('auth_token_session')
			if (sessionToken) {
				return sessionToken
			}

			const keepSession = localStorage.getItem(this.KEEP_SESSION_KEY) === 'true'
			if (keepSession) {
				return this.storageService.getAuthToken()
			}
		}
		LoggerService.warn('AuthenticationService: No token found.')
		return null
	}

	private getRefreshToken(): string | null {
		if (isPlatformBrowser(this.platformId)) {
			const sessionRefreshToken = sessionStorage.getItem(this.SESSION_REFRESH_TOKEN_KEY)
			if (sessionRefreshToken) {
				return sessionRefreshToken
			}

			const keepSession = localStorage.getItem(this.KEEP_SESSION_KEY) === 'true'
			if (keepSession) {
				const authData = localStorage.getItem('refresh_token')
				return authData || null
			}
		}
		LoggerService.warn('AuthenticationService: No refresh token found.')
		return null
	}

	private loadStoredToken(): void {
		if (isPlatformBrowser(this.platformId)) {
			const token = this.getToken()
			const refreshToken = this.getRefreshToken()

			if (token) {
				this._authToken = {
					accessToken: token,
					refreshToken: refreshToken || '',
				}
				this._user = this.decodeUser(token)
				this.startSessionMonitor()
				this.isLoggedIn$.next(true)
				this.userRoles$.next(this._user?.roles || [])
			} else {
				LoggerService.warn('AuthenticationService: Token not found in storage.')
			}
		}
	}

	login(credentials: AuthInterface, keepSession: boolean): void {
		this.authStore.token(credentials).subscribe({
			next: (tokenData: AuthTokenInterface) => {
				this.setAuthToken(tokenData, keepSession)
				this.routerService.navigateTo('/home')
			},
			error: (error: any) => {
				LoggerService.error('AuthenticationService: Error during login', error)
				this.notificationService.showError(error)
			},
		})
	}

	refreshToken(): Observable<boolean> {
		if (!this._authToken || !this._authToken.refreshToken) {
			LoggerService.warn('AuthenticationService: No refresh token available, unable to refresh.')
			return of(false)
		}

		return this.authStore.refreshToken({ refreshToken: this._authToken.refreshToken }).pipe(
			tap((tokenData: AuthTokenInterface) => {
				this.setAuthToken(tokenData, localStorage.getItem(this.KEEP_SESSION_KEY) === 'true')
			}),
			map(() => true),
			catchError((error) => {
				LoggerService.error('AuthenticationService: Error during token refresh', error)
				this.logout()
				this.notificationService.showError('Session expired, please log in again.')
				return of(false)
			})
		)
	}

  private clearLocalTokens(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  }

	logout(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const refreshToken = this.getRefreshToken();

    sessionStorage.clear();
    localStorage.clear();

    this._authToken = null;
    this._user = null;
    this.userRoles$.next([]);
    this.isLoggedIn$.next(false);

    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }

    if (refreshToken) {
      this.authStore.logout(refreshToken).subscribe({
        error: (err) => {
          LoggerService.error('AuthenticationService: Backend logout failed', err);
        }
      });
    }

    this.routerService.rawNavigate('login');
  }

	private setAuthToken(tokenData: AuthTokenInterface | null, keepSession: boolean = false): void {
		if (!tokenData) {
			LoggerService.warn('AuthenticationService: Token data is null, clearing authentication.')
			this.clearAuthToken()
			return
		}

		this._authToken = tokenData

		if (isPlatformBrowser(this.platformId)) {
			if (keepSession) {
				localStorage.setItem(this.KEEP_SESSION_KEY, 'true')
				this.storageService.setAuthToken(tokenData.accessToken)

				if (tokenData.refreshToken) {
					localStorage.setItem('refresh_token', tokenData.refreshToken)
				}
			} else {
				localStorage.removeItem(this.KEEP_SESSION_KEY)
				this.storageService.clearAuthToken()
				localStorage.removeItem('refresh_token')

				sessionStorage.setItem('auth_token_session', tokenData.accessToken)
				if (tokenData.refreshToken) {
					sessionStorage.setItem(this.SESSION_REFRESH_TOKEN_KEY, tokenData.refreshToken)
				}
			}
		}

		this._user = this.decodeUser(tokenData.accessToken)

		this.userRoles$.next(this._user?.roles || [])
		this.isLoggedIn$.next(!!this._user)
		this.startSessionMonitor()
	}

	private clearAuthToken(): void {
		this._authToken = null
		this._user = null

		if (isPlatformBrowser(this.platformId)) {
			sessionStorage.removeItem('auth_token_session')
			sessionStorage.removeItem(this.SESSION_REFRESH_TOKEN_KEY)
		}

		this.userRoles$.next([])
		this.isLoggedIn$.next(false)
		if (this.refreshTimer) {
			clearTimeout(this.refreshTimer)
		}
	}

	private decodeUser(token: string): UserInterface {
		const decoded: any = jwtDecode(token)

		return {
			sub: decoded.sub,
			exp: decoded.exp,
			roles: decoded.roles || [],
			isFirstAccess: decoded.isFirstAccess || false,
			name: decoded.name || '',
			email: decoded.email || '',
			avatarUrl: decoded.avatarUrl || '',
		}
	}

	private startSessionMonitor(): void {
		if (!this._authToken) return
		const expirationTime = this._user?.exp ? this._user.exp * 1000 : 0
		const timeUntilExpiration = expirationTime - Date.now()

		if (timeUntilExpiration > 0) {
			this.refreshTimer = setTimeout(() => {
				this.refreshToken().subscribe()
			}, timeUntilExpiration - 60000)
		}
	}

	getUserRoles(): string[] {
		return this._user?.roles || []
	}
}

export interface UserInterface {
	sub: string
	exp: number
	roles: string[]
	name: string
	email: string
	isFirstAccess: boolean
	avatarUrl?: string
}
