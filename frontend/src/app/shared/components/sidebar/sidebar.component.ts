import { Component, ElementRef, HostListener, OnDestroy, OnInit, Renderer2 } from '@angular/core'
import { MenuService } from '../../../core/services/menu.service'
import { Observable, Subject, takeUntil } from 'rxjs'
import { LayoutStateService } from '../../../core/services/layout-state.service'
import { AuthenticationService } from '../../../core/services/authentication.service'
import { MenuItem } from '../../../core/config/menu.config'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'

@Component({
	selector: 'app-sidebar',
	imports: [CommonModule, RouterModule],
	standalone: true,
	templateUrl: './sidebar.component.html',
	styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent {
	isCollapsed = false
	menuItems$: Observable<MenuItem[]>
	sidebarOpened = true
	isMobile = window.innerWidth <= 992

	constructor(
		private layoutStateService: LayoutStateService,
		private menuService: MenuService,
		private renderer: Renderer2,
		private authService: AuthenticationService,
		private el: ElementRef
	) {
		this.menuItems$ = this.menuService.menuItems$
	}

	ngOnInit() {
		this.layoutStateService.isSidebarCollapsed$.subscribe((state) => {
			this.isCollapsed = state
			this.updateSidebarClass()
		})
		if (this.isMobile) {
			this.layoutStateService.sidebarMobileOpened$.subscribe((state) => {
				this.isCollapsed = state
				this.updateSidebarClass()
			})
		}
	}

	toggleSidebar(): void {
		if (this.isMobile) {
			this.sidebarOpened = !this.sidebarOpened
			this.layoutStateService.setSidebarMobileState(this.sidebarOpened)
		} else {
			this.layoutStateService.toggleSidebar()
		}
		this.updateSidebarClass()
	}

	private updateSidebarClass(): void {
		if (this.isMobile) {
			this.renderer.addClass(this.el.nativeElement, this.sidebarOpened ? 'opened' : 'collapsed')
			this.renderer.removeClass(this.el.nativeElement, this.sidebarOpened ? 'collapsed' : 'opened')
		} else {
			this.renderer.addClass(this.el.nativeElement, this.isCollapsed ? 'collapsed' : 'opened')
			this.renderer.removeClass(this.el.nativeElement, this.isCollapsed ? 'opened' : 'collapsed')
		}
	}

	exit() {
		this.authService.logout()
	}
}
