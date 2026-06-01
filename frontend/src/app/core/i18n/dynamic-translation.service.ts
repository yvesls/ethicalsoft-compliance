import { Injectable, inject } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { Observable, of, catchError, map, shareReplay } from 'rxjs'
import { environment } from '../../enviroments/environments'
import { LanguageService } from './language.service'

interface TranslateResponse {
  original: string
  translated: string
  language: string
}

@Injectable({ providedIn: 'root' })
export class DynamicTranslationService {
  private readonly http = inject(HttpClient)
  private readonly languageService = inject(LanguageService)

  private readonly inMemory = new Map<string, Observable<string>>()

  translate(text: string): Observable<string> {
    if (!text || text.trim().length === 0) {
      return of(text)
    }
    const language = this.languageService.currentLanguage
    if (language === 'pt-BR') {
      return of(text)
    }
    return this.translateTo(text, language)
  }

  translateTo(text: string, language: string): Observable<string> {
    const cacheKey = `${language}|${text}`
    const cached = this.inMemory.get(cacheKey)
    if (cached) return cached

    const request$ = this.http
      .post<TranslateResponse>(`${environment.apiBaseUrl}/api/i18n/translate`, { text, language })
      .pipe(
        map((response) => response.translated ?? text),
        catchError(() => of(text)),
        shareReplay(1),
      )
    this.inMemory.set(cacheKey, request$)
    return request$
  }

  clearCache(): void {
    this.inMemory.clear()
  }
}
