import { Injectable } from '@angular/core';
import {
  CanActivate,
  CanActivateChild,
  CanMatch,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Route,
  UrlSegment,
  UrlTree,
  Router
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthenticationService } from '../sevices/authentication.service';
import { NotificationService } from '../sevices/notification.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild, CanMatch {

  constructor(
    private authService: AuthenticationService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
    return this.verifyAccess();
  }

  canActivateChild(childRoute: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
    return this.verifyAccess();
  }

  canLoad(route: Route, segments: UrlSegment[]): Observable<boolean | UrlTree> {
    return this.verifyAccess();
  }

  canMatch(route: Route, segments: UrlSegment[]): Observable<boolean | UrlTree> {
    return this.verifyAccess();
  }

  private verifyAccess(): Observable<boolean | UrlTree> {
    return this.authService.isAuthenticated$().pipe(
      switchMap(isAuth => {
        if (isAuth) {
          return of(true);
        }
        return this.authService.refreshToken().pipe(
          switchMap(refreshSuccess => {
            if (refreshSuccess) return of(true);
            this.notificationService.showWarning("Usuário não autenticado! Faça o login para acessar esse recurso.");
            return of(this.router.createUrlTree(['/login']));
          }),
          catchError(() => of(this.router.createUrlTree(['/login'])))
        );
      }),
      catchError(() => of(this.router.createUrlTree(['/login'])))
    );
  }
}
