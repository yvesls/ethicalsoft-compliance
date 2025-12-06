import { Component, DestroyRef, OnInit, inject } from '@angular/core'
import { CommonModule } from '@angular/common'
import { NgxSpinnerModule } from 'ngx-spinner'
import { HeaderComponent } from './shared/components/header/header.component'
import { FooterComponent } from './shared/components/footer/footer.component'
import { SidebarComponent } from './shared/components/sidebar/sidebar.component'
import { LayoutStateService } from './core/services/layout-state.service'
import { RouterOutlet } from '@angular/router'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'

@Component({
	selector: 'app-root',
	standalone: true,
	imports: [RouterOutlet, CommonModule, NgxSpinnerModule, SidebarComponent, HeaderComponent, FooterComponent],
	templateUrl: './app.component.html',
})
export class AppComponent implements OnInit {
	title = 'frontend'
	showLayout!: boolean
	isSidebarCollapsed!: boolean

	private readonly layoutStateService = inject(LayoutStateService)
	private readonly destroyRef = inject(DestroyRef)

	ngOnInit(): void {
		this.layoutStateService.showLayout$
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe((state) => {
				this.showLayout = state
			})

		this.layoutStateService.isSidebarCollapsed$
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe((state) => {
				this.isSidebarCollapsed = state
			})
	}
}
