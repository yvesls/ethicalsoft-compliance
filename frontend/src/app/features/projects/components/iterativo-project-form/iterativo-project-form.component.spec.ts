import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IterativoProjectFormComponent } from './iterativo-project-form.component';

describe('IterativoProjectFormComponent', () => {
  let component: IterativoProjectFormComponent;
  let fixture: ComponentFixture<IterativoProjectFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IterativoProjectFormComponent]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(IterativoProjectFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
