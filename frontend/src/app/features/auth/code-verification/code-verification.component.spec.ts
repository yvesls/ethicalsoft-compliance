import { ComponentFixture, TestBed } from '@angular/core/testing'
import { of } from 'rxjs'
import { MenuService } from '../../../core/services/menu.service'
import { RouterService } from '../../../core/services/router.service'
import { AuthStore } from '../../../shared/stores/auth.store'
import { NotificationService } from '../../../core/services/notification.service'

import { CodeVerificationComponent } from './code-verification.component'

describe('CodeVerificationComponent', () => {
	let component: CodeVerificationComponent
	let fixture: ComponentFixture<CodeVerificationComponent>

	beforeEach(async () => {
		const routerServiceMock = {
			currentUrl: '/code-verification',
			getRouteInfoParams: () =>
				of({ vid: 'spec-vid', route: '/code-verification', params: {}, queryParams: {} }),
			navigateTo: jasmine.createSpy('navigateTo').and.resolveTo(true),
			setStoredCurrentPage: jasmine.createSpy('setStoredCurrentPage'),
			getStoredPageViewParams: () => null,
			setStoredPageViewParams: jasmine.createSpy('setStoredPageViewParams'),
		}

		await TestBed.configureTestingModule({
			imports: [CodeVerificationComponent],
			providers: [
				{ provide: MenuService, useValue: {} },
				{ provide: RouterService, useValue: routerServiceMock },
				{ provide: AuthStore, useValue: { validateCode: () => of(void 0) } },
				{
					provide: NotificationService,
					useValue: {
						showSuccess: jasmine.createSpy('showSuccess'),
						showError: jasmine.createSpy('showError'),
					},
				},
			],
		}).compileComponents()

		fixture = TestBed.createComponent(CodeVerificationComponent)
		component = fixture.componentInstance
		fixture.detectChanges()
	})

	it('should create', () => {
		expect(component).toBeTruthy()
	})
})
