import { ComponentFixture, TestBed } from '@angular/core/testing'
import { of } from 'rxjs'
import { MenuService } from '../../../core/services/menu.service'
import { LayoutStateService } from '../../../core/services/layout-state.service'
import { AuthenticationService } from '../../../core/services/authentication.service'

import { SidebarComponent } from './sidebar.component'

describe('SidebarComponent', () => {
	let component: SidebarComponent
	let fixture: ComponentFixture<SidebarComponent>

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [SidebarComponent],
			providers: [
				{ provide: MenuService, useValue: { menuItems$: of([]) } },
				{
					provide: LayoutStateService,
					useValue: {
						isSidebarCollapsed$: of(false),
						sidebarMobileOpened$: of(true),
						toggleSidebar: jasmine.createSpy('toggleSidebar'),
						setSidebarMobileState: jasmine.createSpy('setSidebarMobileState'),
					},
				},
				{ provide: AuthenticationService, useValue: { logout: jasmine.createSpy('logout') } },
			],
		}).compileComponents()
	})

	beforeEach(() => {
		fixture = TestBed.createComponent(SidebarComponent)
		component = fixture.componentInstance
		fixture.detectChanges()
	})

	it('should create', () => {
		expect(component).toBeTruthy()
	})
})
