import { inject, Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProjectContextService } from '../services/project-context.service';
import { LoggerService } from '../services/logger.service';

@Injectable({
  providedIn: 'root',
})
export class ProjectContextInterceptor implements HttpInterceptor {
  private readonly projectContext = inject(ProjectContextService);

  intercept(
    req: HttpRequest<unknown>,
    next: HttpHandler
  ): Observable<HttpEvent<unknown>> {
    const projectId = this.projectContext.getCurrentProjectId();

    if (projectId && !req.headers.has('X-Project-Id')) {
      LoggerService.debug('ProjectContextInterceptor: Attaching X-Project-Id header.');
      const cloned = req.clone({
        setHeaders: { 'X-Project-Id': projectId },
      });
      return next.handle(cloned);
    }

    return next.handle(req);
  }
}
