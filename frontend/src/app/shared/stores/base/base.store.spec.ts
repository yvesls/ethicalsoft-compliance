/* tslint:disable:no-unused-variable */

import { TestBed, inject } from '@angular/core/testing';
import { BaseStore } from './base.store';

describe('Service: BaseStore', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [BaseStore]
    });
  });

  it('should ...', inject([BaseStore], (service: BaseStore) => {
    expect(service).toBeTruthy();
  }));
});
