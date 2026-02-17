import { ComponentFixture, TestBed } from '@angular/core/testing'
import { of } from 'rxjs'
import { MenuService } from '../../../core/services/menu.service'
import { RouterService } from '../../../core/services/router.service'
import { AuthStore } from '../../../shared/stores/auth.store'
import { NotificationService } from '../../../core/services/notification.service'

import { RecoverComponent } from './recover.component'

describe('RecoverComponent', () => {
	let component: RecoverComponent
	let fixture: ComponentFixture<RecoverComponent>

	beforeEach(async () => {
		const routerServiceMock = {
			currentUrl: '/recover-account',
			getRouteInfoParams: () =>
				of({ vid: 'spec-vid', route: '/recover-account', params: {}, queryParams: {} }),
			navigateTo: jasmine.createSpy('navigateTo').and.resolveTo(true),
			setStoredCurrentPage: jasmine.createSpy('setStoredCurrentPage'),
			getStoredPageViewParams: () => null,
			setStoredPageViewParams: jasmine.createSpy('setStoredPageViewParams'),
		}

		await TestBed.configureTestingModule({
			imports: [RecoverComponent],
			providers: [
				{ provide: MenuService, useValue: {} },
				{ provide: RouterService, useValue: routerServiceMock },
				{ provide: AuthStore, useValue: { recover: () => of(void 0) } },
				{
					provide: NotificationService,
					useValue: {
						showSuccess: jasmine.createSpy('showSuccess'),
						showError: jasmine.createSpy('showError'),
					},
				},
			],
		}).compileComponents()

		fixture = TestBed.createComponent(RecoverComponent)
		component = fixture.componentInstance
		fixture.detectChanges()
	})

	it('should create', () => {
		expect(component).toBeTruthy()
	})
})
