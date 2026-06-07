import { HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { catchError, throwError } from 'rxjs'
import { TranslateService } from '@ngx-translate/core'

export const errorTranslationInterceptorFn: HttpInterceptorFn = (req, next) => {
  const translate = inject(TranslateService)
  return next(req).pipe(
    catchError((error) => {
      if (error?.error && typeof error.error === 'object' && error.error.messageKey) {
        const translatedMessage = translate.instant(error.error.messageKey)
        error = {
          ...error,
          error: {
            ...error.error,
            translatedMessage,
          },
        }
      }
      return throwError(() => error)
    })
  )
}
