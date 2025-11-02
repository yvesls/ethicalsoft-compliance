/* tslint:disable:no-unused-variable */

import { TestBed, inject } from '@angular/core/testing';
import { ProjectStore } from './project.store';

describe('Service: Project', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ProjectStore]
    });
  });

  it('should ...', inject([ProjectStore], (service: ProjectStore) => {
    expect(service).toBeTruthy();
  }));
});
