import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, catchError, map, Observable, of, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { AuthStore } from '../../shared/stores/auth.store';
import { AuthTokenInterface } from '../../shared/interfaces/auth/auth-token.interface';
import { AuthInterface } from '../../shared/interfaces/auth/auth.interface';
import { NotificationService } from './notification.service';
import { RouterService } from './router.service';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  private _authToken: AuthTokenInterface | null = null;
  private _user: UserInterface | null = null;
  private refreshTimer: any;

  userRoles$ = new BehaviorSubject<string[]>([]);
  isLoggedIn$ = new BehaviorSubject<boolean>(false);

  constructor(
    private authStore: AuthStore,
    private routerService: RouterService,
    private notificationService: NotificationService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.loadStoredToken();
  }

  isAuthenticated$(): Observable<boolean> {
    return of(this.isAuthenticated());
  }

  isAuthenticated(): boolean {
    if (isPlatformBrowser(this.platformId)) {
      return !!sessionStorage.getItem('accessToken');
    }
    return false;
  }

  getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return this._authToken?.accessToken || sessionStorage.getItem('accessToken');
    }
    return null;
  }

  private loadStoredToken(): void {
    if (isPlatformBrowser(this.platformId)) {
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
        this.routerService.navigateTo('/home');
      },
      error: (error: any) => {
        this.notificationService.showError(error);
      }
    });
  }

  refreshToken(): Observable<boolean> {
    if (!this._authToken || !this._authToken.refreshToken) {
      return of(false);
    }

    return this.authStore.refreshToken({ refreshToken: this._authToken.refreshToken }).pipe(
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
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.clear();
      localStorage.clear();
    }

    this._authToken = null;
    this._user = null;

    this.userRoles$.next([]);
    this.isLoggedIn$.next(false);

    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }

    this.routerService.navigateTo('/login');
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

  private decodeUser(token: string): UserInterface {
    const decoded: any = jwtDecode(token);

    return {
      sub: decoded.sub,
      exp: decoded.exp,
      roles: decoded.roles || [],
      name: decoded.name || "",
      email: decoded.email || "",
      avatarUrl: decoded.avatarUrl || "",
    };
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

  getUserRoles(): string[] {
    return this._user?.roles || [];
  }
}

export interface UserInterface {
  sub: string;
  exp: number;
  roles: string[];
  name: string;
  email: string;
  avatarUrl?: string;
}
