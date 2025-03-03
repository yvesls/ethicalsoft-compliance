import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { filter } from 'rxjs/operators';
import { ROUTES_WITHOUT_LAYOUT } from '../config/layout.config';

@Injectable({
  providedIn: 'root'
})
export class LayoutService {
  private layoutVisibleSubject = new BehaviorSubject<boolean>(true);
  layoutVisible$ = this.layoutVisibleSubject.asObservable();

  constructor(private router: Router) {
    this.updateLayoutState(this.router.url);

    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((event: any) => {
      this.updateLayoutState(event.url);
    });
  }

  private updateLayoutState(url: string): void {
    const cleanUrl = url.split('?')[0];
    const hideLayout = ROUTES_WITHOUT_LAYOUT.includes(cleanUrl);
    this.layoutVisibleSubject.next(!hideLayout);
  }
}
