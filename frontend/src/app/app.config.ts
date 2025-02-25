import { config } from './app.config.server';
import { NgxSpinnerConfig } from './../../node_modules/ngx-spinner/lib/config.d';
import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { tokenInterceptorFn } from './core/interceptors/token.interceptor.fn';
import { httpSpinnerInterceptorFn } from './core/interceptors/http-spinner.interceptor.fn';
import { NgxSpinnerModule } from 'ngx-spinner';


const spinnerConfig: NgxSpinnerModule = {
  type: 'ball-scale-multiple',
  bdColor: 'rgba(0, 0, 0, 0.5)',
  color: '#fff'
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideClientHydration(),
    provideAnimations(),
    importProvidersFrom(NgxSpinnerModule.forRoot(spinnerConfig)),
    provideHttpClient(
      withInterceptors([httpSpinnerInterceptorFn, tokenInterceptorFn]),
      withFetch()
    )
  ]
};
