import { Injectable } from '@angular/core';
import { BehaviorSubject, catchError, map, Observable, of, tap } from 'rxjs';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { StorageService } from './storage.service';
import { AuthTokenInterface } from '../../shared/interfaces/auth-token.interface';
import { AuthInterface } from '../../shared/interfaces/auth.interface';
import { AuthStore } from '../../shared/stores/auth.store';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {
  private _authToken: AuthTokenInterface | null = null;
  private _user: UserModel | null = null;

  userRoles$ = new BehaviorSubject<string[]>([]);
  isLoggedIn$ = new BehaviorSubject<boolean>(false);

  constructor(
    private authStore: AuthStore,
    private storageService: StorageService,
    private router: Router
  ) {
    this._authToken = this.storageService.getAuthToken();
    if (this._authToken && this._authToken.accessToken) {
      this._user = this.decodeUser(this._authToken.accessToken);
      this.userRoles$.next(this._user?.roles || []);
      this.isLoggedIn$.next(true);
    }
  }

  login(credentials: AuthInterface): void {
    this.authStore.token(credentials).subscribe({
      next: (tokenData: AuthTokenInterface) => {
        this.setAuthToken(tokenData);
        console.log('retorno ', tokenData);
        //this.router.navigate(['/home']);
      },
      error: (error: any) => {
        console.error('Login error', error);
      }
    });
  }

  refreshToken(): Observable<boolean> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) return of(false);
    return this.authStore.refreshToken({ refreshToken }).pipe(
      tap((tokenData: AuthTokenInterface) => this.setAuthToken(tokenData)),
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
  }

  getToken(): string | null {
    return this._authToken?.accessToken || null;
  }

  getRefreshToken(): string | null {
    return this._authToken?.refreshToken || null;
  }

  isTokenExpired(): boolean {
    if (!this._user?.exp) return true;
    const now = Math.floor(Date.now() / 1000);
    return now >= this._user.exp;
  }

  private setAuthToken(tokenData: AuthTokenInterface | null): void {
    this._authToken = tokenData;
    this.storageService.setAuthToken(tokenData);
    this._user = tokenData ? this.decodeUser(tokenData.accessToken) : null;
    this.userRoles$.next(this._user?.roles || []);
    this.isLoggedIn$.next(!!this._user);
  }

  private clearAuthToken(): void {
    this._authToken = null;
    this._user = null;
    this.storageService.clearAuthToken();
    this.userRoles$.next([]);
    this.isLoggedIn$.next(false);
  }

  private decodeUser(token: string): UserModel {
    return jwtDecode<UserModel>(token);
  }
}

export interface UserModel {
  sub: string;
  exp: number;
  roles: string[];
}
