import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http'
import { inject } from '@angular/core'
import { TranslateService } from '@ngx-translate/core'
import { catchError, throwError } from 'rxjs'

export const errorTranslationInterceptorFn: HttpInterceptorFn = (req, next) => {
  const translate = inject(TranslateService)

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      enrichErrorBody(error, translate)
      return throwError(() => error)
    }),
  )
}

function enrichErrorBody(error: HttpErrorResponse, translate: TranslateService): void {
  if (!error) return

  const body = parseBody(error.error)
  if (body === null) return

  const key = typeof body['errorKey'] === 'string' ? (body['errorKey'] as string) : undefined
  const message = typeof body['message'] === 'string' ? (body['message'] as string) : undefined

  let translated: string | undefined = message
  if (key) {
    const value = translate.instant(key)
    if (value && value !== key) {
      translated = value
    }
  }

  try {
    ;(body as Record<string, unknown>)['translatedMessage'] = translated
    if (error.error !== body) {
      try {
        Object.defineProperty(error, 'error', { value: body, configurable: true, writable: true })
      } catch {
        // alguns runtimes podem ter o campo readonly; o consumidor pode ler translatedMessage via cast
      }
    }
  } catch {
    // body imutável (string, frozen, etc.): nada a fazer; o consumidor usa message como fallback.
  }
}

function parseBody(raw: unknown): Record<string, unknown> | null {
  if (raw && typeof raw === 'object' && !(raw instanceof Blob)) {
    return raw as Record<string, unknown>
  }
  if (typeof raw === 'string') {
    const trimmed = raw.trim()
    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) return null
    try {
      const parsed = JSON.parse(trimmed)
      return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : null
    } catch {
      return null
    }
  }
  return null
}
