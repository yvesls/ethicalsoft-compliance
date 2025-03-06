import { NotificationService } from '../../../core/services/notification.service';
import { Component, OnInit } from '@angular/core';
import { RouterService } from '../../../core/services/router.service';

@Component({
  selector: 'app-header',
  imports: [],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent implements OnInit {
  routerPath: string = '';

  constructor(
    private routerService: RouterService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.routerPath = this.routerService.getFormattedRoute();
  }

  openNotifications() {
    this.notificationService.showWarning('Funcionalidade não implementada ainda.');
  }
}
