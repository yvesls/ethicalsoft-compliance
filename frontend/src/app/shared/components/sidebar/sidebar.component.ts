import { Component } from '@angular/core';
import { MenuService } from '../../../core/services/menu.service';
import { Observable } from 'rxjs';
import { LayoutStateService } from '../../../core/services/layout-state.service';
import { MenuItem } from '../../../core/config/menu.config';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  imports: [CommonModule, RouterModule],
  standalone: true,
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  isCollapsed = false;
  menuItems$: Observable<MenuItem[]>;

  constructor(
    private layoutStateService: LayoutStateService,
    private menuService: MenuService
  ) {
    this.menuItems$ = this.menuService.menuItems$;
    this.layoutStateService.isSidebarCollapsed$.subscribe(state => {
      this.isCollapsed = state;
    });
  }

  toggleSidebar() {
    this.layoutStateService.toggleSidebar();
  }
}
