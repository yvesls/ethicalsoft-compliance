import { Component } from '@angular/core';
import { LayoutStateService } from '../../../core/sevices/layout-state.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  isCollapsed = false;

  constructor(private layoutStateService: LayoutStateService) {
    this.layoutStateService.isSidebarCollapsed$.subscribe(state => {
      this.isCollapsed = state;
    });
  }

  toggleSidebar() {
    this.layoutStateService.toggleSidebar();
  }
}
