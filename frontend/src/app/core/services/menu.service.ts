import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { AuthenticationService } from './authentication.service';
import { MENU_CONFIG, MenuItem } from '../config/menu.config';

@Injectable({
  providedIn: 'root',
})
export class MenuService {
  private menuItemsSubject = new BehaviorSubject<MenuItem[]>([]);
  menuItems$ = this.menuItemsSubject.asObservable();

  constructor(private authService: AuthenticationService) {
    this.authService.userRoles$.subscribe(userRoles => {
      this.updateMenu(userRoles);
    });
  }

  private updateMenu(userRoles: string[]): void {
    const filteredMenu = this.filterMenu(MENU_CONFIG, userRoles);
    this.menuItemsSubject.next(filteredMenu);
  }

  private filterMenu(menu: MenuItem[], userRoles: string[]): MenuItem[] {
    return menu
      .filter(item => !item.roles || item.roles.some(role => userRoles.includes(role)))
      .map(item => ({
        ...item,
        children: item.children ? this.filterMenu(item.children, userRoles) : undefined,
      }));
  }

}
