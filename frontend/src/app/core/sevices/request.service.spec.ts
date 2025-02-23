/* tslint:disable:no-unused-variable */

import { TestBed, inject } from '@angular/core/testing';
import { RequestService } from './request.service';

describe('Service: Request', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RequestService]
    });
  });

  it('should ...', inject([RequestService], (service: RequestService) => {
    expect(service).toBeTruthy();
  }));
});
