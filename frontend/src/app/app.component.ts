import { Component, OnInit } from '@angular/core'
import { ActivatedRoute, NavigationEnd, Router, RouterOutlet } from '@angular/router'
import { CommonModule } from '@angular/common'
import { NgxSpinnerModule } from 'ngx-spinner'
import { HeaderComponent } from './shared/components/header/header.component'
import { FooterComponent } from './shared/components/footer/footer.component'
import { SidebarComponent } from './shared/components/sidebar/sidebar.component'
import { LayoutStateService } from './core/services/layout-state.service'
import { filter } from 'rxjs'
import { RouterService } from './core/services/router.service'

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

	constructor(private layoutStateService: LayoutStateService) {}

	ngOnInit(): void {
		this.layoutStateService.showLayout$.subscribe((state) => {
			this.showLayout = state
		})

		this.layoutStateService.isSidebarCollapsed$.subscribe((state) => {
			this.isSidebarCollapsed = state
		})
	}

	toggleSidebar(): void {
		this.layoutStateService.toggleSidebar()
	}
}
