import { TestBed } from '@angular/core/testing'

import { NavigationSourceService } from './navigation-source.service'

describe('NavigationSourceService', () => {
	let service: NavigationSourceService

	beforeEach(() => {
		TestBed.configureTestingModule({})
		service = TestBed.inject(NavigationSourceService)
	})

	it('should be created', () => {
		expect(service).toBeTruthy()
	})
})
