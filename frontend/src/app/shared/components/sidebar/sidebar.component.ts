import {
	Component,
	ElementRef,
	HostListener,
	Renderer2,
	DestroyRef,
	inject,
	OnInit,
	ChangeDetectionStrategy,
} from '@angular/core'
import { MenuService } from '../../../core/services/menu.service'
import { Observable } from 'rxjs'
import { LayoutStateService } from '../../../core/services/layout-state.service'
import { AuthenticationService } from '../../../core/services/authentication.service'
import { MenuItem } from '../../../core/config/menu.config'
import { CommonModule } from '@angular/common'
import { RouterModule } from '@angular/router'
import { takeUntilDestroyed } from '@angular/core/rxjs-interop'
import { LanguageSelectorComponent } from '../language-selector/language-selector.component'
import { TranslateModule } from '@ngx-translate/core'

@Component({
	selector: 'app-sidebar',
	imports: [CommonModule, RouterModule, LanguageSelectorComponent, TranslateModule],
	standalone: true,
	templateUrl: './sidebar.component.html',
	changeDetection: ChangeDetectionStrategy.Eager,
	styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent implements OnInit {
	isCollapsed = false
	private layoutStateService = inject(LayoutStateService)
	private menuService = inject(MenuService)
	private renderer = inject(Renderer2)
	private authService = inject(AuthenticationService)
	private el = inject(ElementRef)
	private destroyRef = inject(DestroyRef)
	menuItems$: Observable<MenuItem[]>
	sidebarOpened = true
	isMobile = window.innerWidth <= 992

	constructor() {
		this.menuItems$ = this.menuService.menuItems$
	}

	ngOnInit() {
		this.layoutStateService.isSidebarCollapsed$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((state) => {
			this.isCollapsed = state
			this.updateSidebarClass()
		})
		if (this.isMobile) {
			this.layoutStateService.sidebarMobileOpened$
				.pipe(takeUntilDestroyed(this.destroyRef))
				.subscribe((state) => {
					this.sidebarOpened = state
					this.updateSidebarClass()
				})
		}
	}

	@HostListener('document:click', ['$event'])
	onDocumentClick(event: MouseEvent): void {
		if (!this.isMobile || !this.sidebarOpened) return
		if (!(this.el.nativeElement as HTMLElement).contains(event.target as Node)) {
			this.sidebarOpened = false
			this.layoutStateService.setSidebarMobileState(false)
			this.updateSidebarClass()
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
