import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AiTokenStatus, UpdateAiTokenRequest } from './ai-token.types';

@Injectable({ providedIn: 'root' })
export class AiTokenService {
  private readonly http = inject(HttpClient);
  private readonly _status$ = new BehaviorSubject<AiTokenStatus | null>(null);

  readonly status$ = this._status$.asObservable();

  loadStatus(): Observable<AiTokenStatus> {
    return this.http
      .get<AiTokenStatus>(`${environment.apiBaseUrl}/api/ai/me/token/status`)
      .pipe(tap((status) => this._status$.next(status)));
  }

  updateToken(token: string): Observable<AiTokenStatus> {
    return this.http
      .put<AiTokenStatus>(`${environment.apiBaseUrl}/api/ai/me/token`, { token })
      .pipe(tap((status) => this._status$.next(status)));
  }

  deleteToken(): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/api/ai/me/token`).pipe(
      tap(() =>
        this._status$.next({
          configured: false,
          provider: 'groq-cloud',
          tokenHint: null,
          updatedAt: null,
        })
      )
    );
  }

  get hasToken(): boolean {
    return this._status$.value?.configured === true;
  }
}
