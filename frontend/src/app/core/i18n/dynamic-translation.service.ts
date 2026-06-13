import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay } from 'rxjs/operators';
import { environment } from '../../enviroments/environments';
import { LanguageCode, LanguageService } from './language.service';

@Injectable({ providedIn: 'root' })
export class DynamicTranslationService {
  private readonly http = inject(HttpClient);
  private readonly languageService = inject(LanguageService);
  private readonly cache = new Map<string, Observable<string>>();

  translate(text: string, language?: LanguageCode): Observable<string> {
    const lang = language ?? this.languageService.currentLanguageCode;
    if (!text || lang === 'pt-BR') return of(text);
    const key = `${lang}::${text}`;
    if (!this.cache.has(key)) {
      this.cache.set(
        key,
        this.http
          .post<{ translated: string }>(
            `${environment.apiBaseUrl}/api/i18n/translate`,
            { text, language: lang }
          )
          .pipe(
            map((r) => r.translated ?? text),
            catchError(() => of(text)),
            shareReplay(1)
          )
      );
    }
    return this.cache.get(key)!;
  }
}
