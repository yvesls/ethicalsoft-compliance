import { Injectable, inject } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { BehaviorSubject, Observable, catchError, map, of } from 'rxjs'
import { TranslateService } from '@ngx-translate/core'
import { AuthenticationService } from '../services/authentication.service'
import { environment } from '../../enviroments/environments'

export type LanguageCode = 'pt-BR' | 'en-US' | 'es-ES'

export interface SupportedLanguage {
  code: LanguageCode
  label: string
}

const SUPPORTED_LANGUAGES: SupportedLanguage[] = [
  { code: 'pt-BR', label: 'Português (BR)' },
  { code: 'en-US', label: 'English (US)' },
  { code: 'es-ES', label: 'Español (ES)' },
]

const DEFAULT_LANGUAGE: LanguageCode = 'pt-BR'
const STORAGE_KEY = 'app_language'

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly http = inject(HttpClient)
  private readonly translate = inject(TranslateService)
  private readonly authService = inject(AuthenticationService)

  private readonly _supportedLanguages$ = new BehaviorSubject<SupportedLanguage[]>(SUPPORTED_LANGUAGES)
  private readonly _currentLanguage$ = new BehaviorSubject<LanguageCode>(DEFAULT_LANGUAGE)

  readonly supportedLanguages$ = this._supportedLanguages$.asObservable()
  readonly currentLanguage$ = this._currentLanguage$.asObservable()

  private hasAuthToken(): boolean {
    return !!this.authService.getToken()
  }

  initialize(): Promise<void> {
    const codes = SUPPORTED_LANGUAGES.map((l) => l.code)
    this.translate.addLangs(codes)
    this.translate.setDefaultLang(DEFAULT_LANGUAGE)
    const stored = this.readFromStorage()
    this.applyLanguage(stored ?? DEFAULT_LANGUAGE)

    if (!this.hasAuthToken()) {
      return Promise.resolve()
    }

    return new Promise<void>((resolve) => {
      this.http
        .get<{ language: LanguageCode }>(`${environment.apiBaseUrl}/api/i18n/me/language`)
        .subscribe({
          next: (r) => {
            this.applyLanguage(r.language)
            resolve()
          },
          error: () => resolve(),
        })
    })
  }

  changeLanguage(code: LanguageCode): Observable<LanguageCode> {
    this.writeToStorage(code)
    this.applyLanguage(code)
    return this.http
      .patch<{ language: LanguageCode }>(
        `${environment.apiBaseUrl}/api/i18n/me/language`,
        { language: code }
      )
      .pipe(
        map((r) => r.language),
        catchError(() => of(code))
      )
  }

  get currentLanguageCode(): LanguageCode {
    return this._currentLanguage$.value
  }

  private applyLanguage(code: LanguageCode): void {
    if (this._currentLanguage$.value !== code) {
      this._currentLanguage$.next(code)
    }
    this.translate.use(code)
    this.writeToStorage(code)
  }

  private readFromStorage(): LanguageCode | null {
    try {
      if (typeof localStorage === 'undefined') return null
      const stored = localStorage.getItem(STORAGE_KEY)
      const valid = SUPPORTED_LANGUAGES.map((l) => l.code)
      return valid.includes(stored as LanguageCode) ? (stored as LanguageCode) : null
    } catch {
      return null
    }
  }

  private writeToStorage(language: LanguageCode): void {
    try {
      if (typeof localStorage === 'undefined') return
      localStorage.setItem(STORAGE_KEY, language)
    } catch {
      // ignora
    }
  }
}
