import { TestBed, inject } from '@angular/core/testing'
import { StorageService } from './storage.service'

describe('Service: Storage', () => {
	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [StorageService],
		})
	})

	it('should ...', inject([StorageService], (service: StorageService) => {
		expect(service).toBeTruthy()
	}))
})
