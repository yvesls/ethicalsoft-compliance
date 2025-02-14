/* tslint:disable:no-unused-variable */

import { TestBed, inject } from '@angular/core/testing';
import { AuthStore } from './auth.store';

describe('Service: Auth', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthStore]
    });
  });

  it('should ...', inject([AuthStore], (service: AuthStore) => {
    expect(service).toBeTruthy();
  }));
});
