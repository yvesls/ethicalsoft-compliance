import { ComponentFixture, TestBed } from '@angular/core/testing'
import { of } from 'rxjs'
import { MenuService } from '../../../core/services/menu.service'
import { RouterService } from '../../../core/services/router.service'
import { AuthenticationService } from '../../../core/services/authentication.service'

import { LoginComponent } from './login.component'

describe('LoginComponent', () => {
	let component: LoginComponent
	let fixture: ComponentFixture<LoginComponent>

	beforeEach(async () => {
		const routerServiceMock = {
			currentUrl: '/login',
			getRouteInfoParams: () => of({ vid: 'spec-vid', route: '/login', params: {}, queryParams: {} }),
			navigateTo: jasmine.createSpy('navigateTo').and.resolveTo(true),
			setStoredCurrentPage: jasmine.createSpy('setStoredCurrentPage'),
			getStoredPageViewParams: () => null,
			setStoredPageViewParams: jasmine.createSpy('setStoredPageViewParams'),
		}

		await TestBed.configureTestingModule({
			imports: [LoginComponent],
			providers: [
				{ provide: MenuService, useValue: {} },
				{ provide: RouterService, useValue: routerServiceMock },
				{ provide: AuthenticationService, useValue: { login: jasmine.createSpy('login') } },
			],
		}).compileComponents()

		fixture = TestBed.createComponent(LoginComponent)
		component = fixture.componentInstance
		fixture.detectChanges()
	})

	it('should create', () => {
		expect(component).toBeTruthy()
	})
})
