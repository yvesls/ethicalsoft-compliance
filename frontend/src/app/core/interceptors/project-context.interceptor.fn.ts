import { HttpEvent, HttpHandler, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProjectContextInterceptor } from './project-context.interceptor';

export const projectContextInterceptorFn: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const interceptor = inject(ProjectContextInterceptor);
  const nextHandler: HttpHandler = { handle: next };
  return interceptor.intercept(req, nextHandler);
};
