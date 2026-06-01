import { Injectable, PLATFORM_ID, inject } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { isPlatformBrowser } from '@angular/common'
import { BehaviorSubject, Observable, catchError, map, of, tap } from 'rxjs'
import { TranslateService } from '@ngx-translate/core'
import { environment } from '../../enviroments/environments'

export type LanguageCode = 'pt-BR' | 'en-US' | 'es-ES'

export interface SupportedLanguage {
  code: LanguageCode
  label: string
  isDefault: boolean
}

export const DEFAULT_LANGUAGE: LanguageCode = 'pt-BR'
const STORAGE_KEY = 'ethicalsoft.language'

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly http = inject(HttpClient)
  private readonly translate = inject(TranslateService)
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID))

  private readonly _currentLanguage = new BehaviorSubject<LanguageCode>(DEFAULT_LANGUAGE)
  readonly currentLanguage$ = this._currentLanguage.asObservable()

  private readonly _supportedLanguages = new BehaviorSubject<SupportedLanguage[]>([])
  readonly supportedLanguages$ = this._supportedLanguages.asObservable()

  get currentLanguage(): LanguageCode {
    return this._currentLanguage.value
  }

  initialize(): Promise<void> {
    this.registerLanguages()

    if (!this.isBrowser) {
      return Promise.resolve()
    }

    const stored = this.readFromStorage()
    this.applyLanguage(stored ?? DEFAULT_LANGUAGE)

    if (!this.hasAuthToken()) {
      return Promise.resolve()
    }

    return new Promise<void>((resolve) => {
      this.loadFromBackend().subscribe({
        next: (language) => {
          if (language) this.applyLanguage(language)
          resolve()
        },
        error: () => resolve(),
      })
    })
  }

  loadSupportedLanguages(): Observable<SupportedLanguage[]> {
    return this.http.get<SupportedLanguage[]>(`${environment.apiBaseUrl}/api/i18n/languages`).pipe(
      tap((languages) => this._supportedLanguages.next(languages)),
      catchError(() => of(this.staticSupportedLanguages())),
    )
  }

  changeLanguage(language: LanguageCode): Observable<LanguageCode> {
    this.applyLanguage(language)
    return this.http
      .patch<{ language: LanguageCode }>(`${environment.apiBaseUrl}/api/i18n/me/language`, { language })
      .pipe(
        map((response) => response.language),
        tap((applied) => this.applyLanguage(applied)),
        catchError(() => of(language)),
      )
  }

  private applyLanguage(language: LanguageCode): void {
    const normalized = this.normalize(language)
    if (this._currentLanguage.value !== normalized) {
      this._currentLanguage.next(normalized)
    }
    this.translate.use(normalized).subscribe({ error: () => undefined })
    if (this.isBrowser) {
      this.writeToStorage(normalized)
      document.documentElement.lang = normalized
    }
  }

  private loadFromBackend(): Observable<LanguageCode | null> {
    return this.http
      .get<{ language: LanguageCode }>(`${environment.apiBaseUrl}/api/i18n/me/language`)
      .pipe(
        map((response) => response.language),
        catchError(() => of(null)),
      )
  }

  private registerLanguages(): void {
    const codes: LanguageCode[] = ['pt-BR', 'en-US', 'es-ES']
    this.translate.addLangs(codes)
    this._supportedLanguages.next(this.staticSupportedLanguages())
  }

  private staticSupportedLanguages(): SupportedLanguage[] {
    return [
      { code: 'pt-BR', label: 'Português (Brasil)', isDefault: true },
      { code: 'en-US', label: 'English (US)', isDefault: false },
      { code: 'es-ES', label: 'Español', isDefault: false },
    ]
  }

  private normalize(language: string | null | undefined): LanguageCode {
    if (!language) return DEFAULT_LANGUAGE
    const code = language.trim()
    if (code === 'pt-BR' || code === 'en-US' || code === 'es-ES') return code
    return DEFAULT_LANGUAGE
  }

  private readFromStorage(): LanguageCode | null {
    if (!this.isBrowser) return null
    try {
      const value = localStorage.getItem(STORAGE_KEY)
      return value ? this.normalize(value) : null
    } catch {
      return null
    }
  }

  private writeToStorage(language: LanguageCode): void {
    if (!this.isBrowser) return
    try {
      localStorage.setItem(STORAGE_KEY, language)
    } catch {
      // ignora indisponibilidade do storage (modo privado)
    }
  }

  private hasAuthToken(): boolean {
    if (!this.isBrowser) return false
    try {
      return !!(sessionStorage.getItem('auth_token_session') || localStorage.getItem('refresh_token'))
    } catch {
      return false
    }
  }
}
