import { AuthRefreshTokenInterface } from './../../shared/interfaces/auth-refresh-token.interface';
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, catchError, map, Observable, of, tap, timer } from 'rxjs';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { AuthStore } from '../../shared/stores/auth.store';
import { AuthTokenInterface } from '../../shared/interfaces/auth-token.interface';
import { AuthInterface } from '../../shared/interfaces/auth.interface';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  private _authToken: AuthTokenInterface | null = null;
  private _user: UserModel | null = null;
  private refreshTimer: any;

  userRoles$ = new BehaviorSubject<string[]>([]);
  isLoggedIn$ = new BehaviorSubject<boolean>(false);

  constructor(
    private authStore: AuthStore,
    private router: Router,
    private notificationService: NotificationService,
    @Inject(PLATFORM_ID) private platformId: Object // 🔹 Identifica se está rodando no navegador ou no servidor (SSR)
  ) {
    this.loadStoredToken();
  }

  getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) { // 🔹 Garante que sessionStorage só será acessado no navegador
      return this._authToken?.accessToken || sessionStorage.getItem('accessToken');
    }
    return null;
  }

  private loadStoredToken(): void {
    if (isPlatformBrowser(this.platformId)) { // 🔹 Evita erro no SSR ao acessar sessionStorage
      const token = sessionStorage.getItem('accessToken');
      if (token) {
        this._authToken = { accessToken: token, refreshToken: '' };
        this._user = this.decodeUser(token);
        this.startSessionMonitor();
        this.isLoggedIn$.next(true);
        this.userRoles$.next(this._user?.roles || []);
      }
    }
  }

  login(credentials: AuthInterface): void {
    this.authStore.token(credentials).subscribe({
      next: (tokenData: AuthTokenInterface) => {
        this.setAuthToken(tokenData);
        this.notificationService.showSuccess('Logado com sucesso.');
        this.router.navigate(['/home']);
      },
      error: (error: any) => {
        this.notificationService.showError('Erro no login. Verifique suas credenciais.');
        console.error('Login error', error);
      }
    });
  }

  refreshToken(): Observable<boolean> {
    if (!this._authToken || !this._authToken.refreshToken) { // 🔹 Evita erro caso o refreshToken não esteja disponível
      return of(false);
    }

    const authRefresh: AuthRefreshTokenInterface = { refreshToken: this._authToken.refreshToken };
    return this.authStore.refreshToken(authRefresh).pipe(
      tap((tokenData: AuthTokenInterface) => {
        this.setAuthToken(tokenData);
      }),
      map(() => true),
      catchError((error) => {
        this.logout();
        return of(false);
      })
    );
  }

  logout(): void {
    this.clearAuthToken();
    this.router.navigate(['/login']);
    this.notificationService.showWarning('Sessão encerrada.');
  }

  private setAuthToken(tokenData: AuthTokenInterface | null): void {
    if (!tokenData) {
      this.clearAuthToken();
      return;
    }
    this._authToken = tokenData;

    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.setItem('accessToken', tokenData.accessToken);
    }

    this._user = this.decodeUser(tokenData.accessToken);
    this.userRoles$.next(this._user?.roles || []);
    this.isLoggedIn$.next(!!this._user);
    this.startSessionMonitor();
  }

  private clearAuthToken(): void {
    this._authToken = null;
    this._user = null;

    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem('accessToken');
    }

    this.userRoles$.next([]);
    this.isLoggedIn$.next(false);
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }
  }

  private decodeUser(token: string): UserModel {
    return jwtDecode<UserModel>(token);
  }

  private startSessionMonitor(): void {
    if (!this._authToken) return;
    const expirationTime = this._user?.exp ? this._user.exp * 1000 : 0;
    const timeUntilExpiration = expirationTime - Date.now();

    if (timeUntilExpiration > 0) {
      this.refreshTimer = setTimeout(() => {
        this.refreshToken().subscribe();
      }, timeUntilExpiration - 60000);
    }
  }
}

export interface UserModel {
  sub: string;
  exp: number;
  roles: string[];
  name: string;
  email: string;
  avatarUrl?: string;
}
