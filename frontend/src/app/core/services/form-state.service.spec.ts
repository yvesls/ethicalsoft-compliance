import { TestBed, inject } from '@angular/core/testing';
import { FormStateService } from './form-state.service';

describe('Service: FormState', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [FormStateService]
    });
  });

  it('should ...', inject([FormStateService], (service: FormStateService) => {
    expect(service).toBeTruthy();
  }));
});
