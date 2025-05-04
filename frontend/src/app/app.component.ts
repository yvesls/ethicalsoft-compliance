import { Component, OnInit, Renderer2 } from '@angular/core'
import { CommonModule } from '@angular/common'
import { NgxSpinnerModule } from 'ngx-spinner'
import { HeaderComponent } from './shared/components/header/header.component'
import { FooterComponent } from './shared/components/footer/footer.component'
import { SidebarComponent } from './shared/components/sidebar/sidebar.component'
import { LayoutStateService } from './core/services/layout-state.service'
import { RouterOutlet } from '@angular/router'

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
}
