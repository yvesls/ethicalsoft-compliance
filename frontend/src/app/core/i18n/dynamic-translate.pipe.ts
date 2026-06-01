import { Pipe, PipeTransform, inject } from '@angular/core'
import { Observable, of, switchMap } from 'rxjs'
import { DynamicTranslationService } from './dynamic-translation.service'
import { LanguageService } from './language.service'

@Pipe({
  name: 'dynamicTranslate',
  standalone: true,
})
export class DynamicTranslatePipe implements PipeTransform {
  private readonly service = inject(DynamicTranslationService)
  private readonly languageService = inject(LanguageService)

  transform(value: string | null | undefined): Observable<string> {
    if (!value) {
      return of('')
    }
    return this.languageService.currentLanguage$.pipe(
      switchMap(() => this.service.translate(value)),
    )
  }
}
