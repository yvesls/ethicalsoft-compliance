import { HttpEvent, HttpHandler, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenInterceptor } from './token.interceptor';
import { Observable } from 'rxjs';

export const tokenInterceptorFn: HttpInterceptorFn = (req: HttpRequest<any>, next: HttpHandlerFn): Observable<HttpEvent<any>> => {
  const interceptor = inject(TokenInterceptor);
  const nextHandler: HttpHandler = { handle: next };
  return interceptor.intercept(req, nextHandler);
};
