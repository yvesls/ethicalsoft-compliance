import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, filter } from 'rxjs';
import { ROUTES_WITHOUT_LAYOUT } from '../config/layout.config';

@Injectable({
  providedIn: 'root'
})
export class LayoutService {
  private layoutVisibleSubject = new BehaviorSubject<boolean>(true);
  layoutVisible$ = this.layoutVisibleSubject.asObservable();

  constructor(private router: Router) {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        const hideLayout = ROUTES_WITHOUT_LAYOUT.includes(event.url);
        this.layoutVisibleSubject.next(!hideLayout);
      });
  }
}
