import { Component } from '@angular/core';
import { RouterService } from '../../../core/sevices/router.service';

@Component({
  selector: 'app-not-found',
  standalone: true,
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.scss']
})
export class NotFoundComponent {
  constructor(private routerService: RouterService) {}

  goToHome() {
    this.routerService.navigateTo('/');
  }
}
