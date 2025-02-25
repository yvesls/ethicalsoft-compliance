import { HttpEvent, HttpHandler, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpSpinnerInterceptor } from './http-spinner.interceptor';

export const httpSpinnerInterceptorFn: HttpInterceptorFn = (req: HttpRequest<any>, next: HttpHandlerFn): Observable<HttpEvent<any>> => {
  const interceptor = inject(HttpSpinnerInterceptor);
  const nextHandler: HttpHandler = { handle: next };

  return interceptor.intercept(req, nextHandler);
};
