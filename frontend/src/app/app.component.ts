import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NgxSpinnerModule } from 'ngx-spinner';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { LayoutService } from './core/sevices/layout.service';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { LayoutStateService } from './core/sevices/layout-state.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    CommonModule,
    NgxSpinnerModule,
    SidebarComponent,
    HeaderComponent,
    FooterComponent,
  ],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnInit {
  title = 'frontend';
  isSidebarCollapsed = false;
  showLayout: boolean | null = null;

  constructor(
    private layoutService: LayoutService,
    private layoutStateService: LayoutStateService
  ) {}

  ngOnInit(): void {
    this.layoutService.layoutVisible$.subscribe(visible => {
      this.showLayout = visible;
    });
    this.layoutStateService.isSidebarCollapsed$.subscribe(state => {
      this.isSidebarCollapsed = state;
    });
  }
}
