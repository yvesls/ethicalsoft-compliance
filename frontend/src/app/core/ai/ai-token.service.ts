import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { RequestService } from '../services/request.service';
import { environment } from '../../enviroments/environments';
import { AiTokenStatus } from './ai-token.types';

@Injectable({ providedIn: 'root' })
export class AiTokenService {
  private readonly requestService = inject(RequestService);
  private readonly _status$ = new BehaviorSubject<AiTokenStatus | null>(null);
  readonly status$ = this._status$.asObservable();

  constructor() {
    this.requestService.apiUrl = environment.apiBaseUrl;
  }

  loadStatus(): Observable<AiTokenStatus> {
    return this.requestService.makeGet<AiTokenStatus>(
      'api/ai/me/token/status',
      { useAuth: true }
    ).pipe(tap((status) => this._status$.next(status)));
  }

  updateToken(token: string): Observable<AiTokenStatus> {
    return this.requestService.makePut<AiTokenStatus>(
      'api/ai/me/token',
      { useAuth: true, data: { token } }
    ).pipe(tap((status) => this._status$.next(status)));
  }

  deleteToken(): Observable<void> {
    return this.requestService.makeDelete<void>(
      'api/ai/me/token',
      { useAuth: true }
    ).pipe(
      tap(() => this._status$.next({
        configured: false,
        provider: 'groq-cloud',
        tokenHint: null,
        updatedAt: null,
      }))
    );
  }

  get hasToken(): boolean {
    return this._status$.value?.configured === true;
  }

  get currentStatus(): AiTokenStatus | null {
    return this._status$.value;
  }
}
