import { NotificationService } from '../../../core/services/notification.service';
import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { RouterService } from '../../../core/services/router.service';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
	selector: 'app-header',
	imports: [],
	templateUrl: './header.component.html',
	styleUrl: './header.component.scss',
})
export class HeaderComponent implements OnInit {
	routerPath: string = ''
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

	constructor(
		private routerService: RouterService,
		private notificationService: NotificationService
	) {}

	ngOnInit(): void {
    this.routerPath = this.routerService.getFormattedRoute();

    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.routerPath = this.routerService.getFormattedRoute();
    });
  }

	openNotifications() {
		this.notificationService.showWarning('Funcionalidade não implementada ainda.')
	}
}
