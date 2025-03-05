import { NotificationService } from '../../../core/services/notification.service';
import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  imports: [],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {

  constructor(private notificationService: NotificationService)
  {}

  openNotifications() {
    this.notificationService.showWarning('Funcionalidade não implementada ainda.');
  }
}
