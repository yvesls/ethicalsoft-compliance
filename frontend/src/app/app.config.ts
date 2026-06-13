import { ApplicationConfig, PLATFORM_ID, inject, provideAppInitializer } from '@angular/core'
import { isPlatformBrowser } from '@angular/common'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { routes } from './app.routes'
import { provideClientHydration } from '@angular/platform-browser'
import { BrowserAnimationsModule, provideAnimations } from '@angular/platform-browser/animations'
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http'
import { tokenInterceptorFn } from './core/interceptors/token.interceptor.fn'
import { spinnerInterceptorFn } from './core/interceptors/spinner.interceptor.fn'
import { projectContextInterceptorFn } from './core/interceptors/project-context.interceptor.fn'
import { errorTranslationInterceptorFn } from './core/interceptors/error-translation.interceptor.fn'
import { languageHeaderInterceptorFn } from './core/interceptors/language-header.interceptor.fn'

import { provideTranslateService } from '@ngx-translate/core'
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader'
import { LanguageService } from './core/i18n/language.service'
import { AiTokenService } from './core/ai/ai-token.service'
import { AuthenticationService } from './core/services/authentication.service'
import { catchError, of } from 'rxjs'

import { provideEchartsCore } from 'ngx-echarts'
import * as echarts from 'echarts/core'
import { BarChart, HeatmapChart, RadarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  VisualMapComponent,
  TitleComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideClientHydration(),
    provideAnimations(),
    BrowserAnimationsModule,
    provideHttpClient(
      withInterceptors([spinnerInterceptorFn, tokenInterceptorFn, projectContextInterceptorFn, languageHeaderInterceptorFn, errorTranslationInterceptorFn]),
      withFetch()
    ),
    provideTranslateService({
      defaultLanguage: 'pt-BR',
      loader: provideTranslateHttpLoader({ prefix: './assets/i18n/', suffix: '.json' }),
    }),
    provideEchartsCore({ echarts }),
    provideAppInitializer(() => {
      const platformId = inject(PLATFORM_ID)
      if (isPlatformBrowser(platformId)) {
        echarts.use([
          BarChart,
          HeatmapChart,
          RadarChart,
          GridComponent,
          TooltipComponent,
          LegendComponent,
          VisualMapComponent,
          TitleComponent,
          CanvasRenderer,
        ])
      }
    }),
    provideAppInitializer(() => {
      const languageService = inject(LanguageService)
      return languageService.initialize()
    }),
    provideAppInitializer(() => {
      const aiTokenService = inject(AiTokenService)
      const authService = inject(AuthenticationService)
      if (authService.getToken()) {
        aiTokenService.loadStatus().pipe(catchError(() => of(null))).subscribe()
      }
    }),
  ],
}
