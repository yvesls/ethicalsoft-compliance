import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { map, Observable, tap } from 'rxjs';
import { AuthenticationService } from '../sevices/authentication.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthenticationService, private router: Router) {}

  canActivate(): Observable<boolean> {
    return this.authService.isLoggedIn$.pipe(
      tap((loggedIn: any) => {
        if (!loggedIn) {
          this.router.navigate(['/login']);
        }
      }),
      map((loggedIn) => loggedIn)
    );
  }
}
