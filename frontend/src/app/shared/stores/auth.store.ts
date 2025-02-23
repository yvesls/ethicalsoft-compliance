import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseStore } from './base/base.store';
import { AuthInterface } from '../interfaces/auth.interface';
import { AuthTokenInterface } from '../interfaces/auth-token.interface';
import { AuthRefreshTokenInterface } from '../interfaces/auth-refresh-token.interface';

const AUTH_API = 'auth'
const CHECK_TOKEN = 'check-token'

@Injectable({
  providedIn: 'root'
})
export class AuthStore extends BaseStore {
  constructor() {
    super(AUTH_API);
  }

  token(inputLogin: AuthInterface): Observable<AuthTokenInterface> {
    return this.requestService.makePost(this.getUrl('token'), { data: inputLogin });
  }

  refreshToken(refreshToken: AuthRefreshTokenInterface): Observable<AuthTokenInterface> {
    return this.requestService.makePost(this.getUrl('refresh-token'), { data: refreshToken });
  }

  checkToken(): Observable<string> {
    return this.requestService.makeGet(this.getUrl(CHECK_TOKEN), { useAuth: false });
  }

}
