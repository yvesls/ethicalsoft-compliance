import { TestBed, inject } from '@angular/core/testing';
import { RouterService } from './router.service';

describe('Service: Router', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RouterService]
    });
  });

  it('should ...', inject([RouterService], (service: RouterService) => {
    expect(service).toBeTruthy();
  }));
});
