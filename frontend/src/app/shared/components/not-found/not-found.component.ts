import { Component, inject, ChangeDetectionStrategy } from '@angular/core'
import { RouterService } from '../../../core/services/router.service'
import { TranslateModule } from '@ngx-translate/core'

@Component({
	selector: 'app-not-found',
	standalone: true,
	imports: [TranslateModule],
	templateUrl: './not-found.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrls: ['./not-found.component.scss'],
})
export class NotFoundComponent {
	private routerService = inject(RouterService)

	goToHome() {
		this.routerService.navigateTo('/')
	}
}
