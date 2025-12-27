import { ApplicationConfig } from '@angular/core'
import { provideRouter, withComponentInputBinding } from '@angular/router'
import { routes } from './app.routes'
import { provideClientHydration } from '@angular/platform-browser'
import { BrowserAnimationsModule, provideAnimations } from '@angular/platform-browser/animations'
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http'
import { tokenInterceptorFn } from './core/interceptors/token.interceptor.fn'
import { spinnerInterceptorFn } from './core/interceptors/spinner.interceptor.fn'
import { projectContextInterceptorFn } from './core/interceptors/project-context.interceptor.fn'

export const appConfig: ApplicationConfig = {
	providers: [
		provideRouter(routes, withComponentInputBinding()),
		provideClientHydration(),
		provideAnimations(),
		BrowserAnimationsModule,
		provideHttpClient(
			withInterceptors([spinnerInterceptorFn, tokenInterceptorFn, projectContextInterceptorFn]),
			withFetch()
		),
	],
}
