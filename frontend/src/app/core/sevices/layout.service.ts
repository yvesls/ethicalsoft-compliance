import { Injectable } from '@angular/core';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';
import { ReplaySubject } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { ROUTES_WITHOUT_LAYOUT } from '../config/layout.config';

@Injectable({
  providedIn: 'root'
})
export class LayoutService {
  private layoutVisibleSubject = new ReplaySubject<boolean>(1);
  layoutVisible$ = this.layoutVisibleSubject.asObservable();

  constructor(private router: Router) {
    this.router.events.pipe(filter(event => event instanceof NavigationStart)).subscribe(() => {
      this.layoutVisibleSubject.next(undefined as unknown as boolean);
    });

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd), take(1))
      .subscribe((event: any) => {
        const cleanUrl = event.url.split('?')[0];
        const hideLayout = ROUTES_WITHOUT_LAYOUT.includes(cleanUrl);
        this.layoutVisibleSubject.next(!hideLayout);
      });
  }
}
