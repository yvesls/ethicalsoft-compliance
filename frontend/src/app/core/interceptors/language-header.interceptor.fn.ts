import { HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { LanguageService } from '../i18n/language.service'

export const languageHeaderInterceptorFn: HttpInterceptorFn = (req, next) => {
  const languageService = inject(LanguageService)
  const lang = languageService.currentLanguageCode
  const cloned = req.clone({
    setHeaders: { 'Accept-Language': lang },
  })
  return next(cloned)
}
