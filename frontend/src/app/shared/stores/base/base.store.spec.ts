import { TestBed } from '@angular/core/testing'
import { BaseStore } from './base.store'
import { RequestService } from '../../../core/services/request.service'
import { Injectable } from '@angular/core'

@Injectable()
class TestStore extends BaseStore {
	constructor() {
		super('test')
	}
}

describe('Service: BaseStore', () => {
	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				TestStore,
				{ provide: RequestService, useValue: { apiUrl: '', makeGet: () => null, makePost: () => null } },
			],
		})
	})

	it('should create concrete store', () => {
		const service = TestBed.inject(TestStore)
		expect(service).toBeTruthy()
	})
})
