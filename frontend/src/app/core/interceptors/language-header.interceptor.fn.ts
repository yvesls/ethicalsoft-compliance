import { HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { LanguageService } from '../i18n/language.service'

export const languageHeaderInterceptorFn: HttpInterceptorFn = (req, next) => {
  const languageService = inject(LanguageService)
  if (req.headers.has('Accept-Language')) {
    return next(req)
  }
  const language = languageService.currentLanguage
  if (!language) {
    return next(req)
  }
  const cloned = req.clone({ setHeaders: { 'Accept-Language': language } })
  return next(cloned)
}
