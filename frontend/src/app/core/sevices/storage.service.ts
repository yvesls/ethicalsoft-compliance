import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { AuthTokenInterface } from '../../shared/interfaces/auth-token.interface';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class StorageService {

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  private authTokenKey = 'auth_token';

  setAuthToken(token: AuthTokenInterface | null): void {
    if (token) {
      localStorage.setItem(this.authTokenKey, JSON.stringify(token));
    } else {
      localStorage.removeItem(this.authTokenKey);
    }
  }

  getAuthToken(): AuthTokenInterface | null {
    if (isPlatformBrowser(this.platformId)) {
      const tokenStr = localStorage.getItem(this.authTokenKey);
      return tokenStr ? JSON.parse(tokenStr) : null;
    }
    return null;
  }

  clearAuthToken(): void {
    localStorage.removeItem(this.authTokenKey);
  }
}
