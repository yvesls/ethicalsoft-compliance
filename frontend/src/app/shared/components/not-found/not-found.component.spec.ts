import { ComponentFixture, TestBed } from '@angular/core/testing'
import { RouterService } from '../../../core/services/router.service'

import { NotFoundComponent } from './not-found.component'

describe('NotFoundComponent', () => {
	let component: NotFoundComponent
	let fixture: ComponentFixture<NotFoundComponent>

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [NotFoundComponent],
			providers: [
				{ provide: RouterService, useValue: { navigateTo: jasmine.createSpy('navigateTo').and.resolveTo(true) } },
			],
		}).compileComponents()

		fixture = TestBed.createComponent(NotFoundComponent)
		component = fixture.componentInstance
		fixture.detectChanges()
	})

	it('should create', () => {
		expect(component).toBeTruthy()
	})
})
