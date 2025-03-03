import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  @Output() toggle = new EventEmitter<void>();
  isCollapsed = false;
  arrow = 'left';

  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
    this.arrow = this.isCollapsed ? 'right' : 'left';
    this.toggle.emit();
  }
}
