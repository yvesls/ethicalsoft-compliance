import { inject, provideAppInitializer, EnvironmentProviders, Provider } from '@angular/core'
import { TranslateService, provideTranslateService } from '@ngx-translate/core'
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader'
import { LanguageService, DEFAULT_LANGUAGE } from './language.service'

export function provideI18n(): (EnvironmentProviders | Provider)[] {
  return [
    ...provideTranslateService({ lang: DEFAULT_LANGUAGE, fallbackLang: DEFAULT_LANGUAGE }),
    ...provideTranslateHttpLoader({ prefix: '/assets/i18n/', suffix: '.json', useHttpBackend: true }),
    provideAppInitializer(() => {
      const languageService = inject(LanguageService)
      inject(TranslateService)
      return languageService.initialize()
    }),
  ]
}
