import { TestBed, inject } from '@angular/core/testing';
import { LayoutStateService } from './layout-state.service';

describe('Service: LayoutState', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LayoutStateService]
    });
  });

  it('should ...', inject([LayoutStateService], (service: LayoutStateService) => {
    expect(service).toBeTruthy();
  }));
});
