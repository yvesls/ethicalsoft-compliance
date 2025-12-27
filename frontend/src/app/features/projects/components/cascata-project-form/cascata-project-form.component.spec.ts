import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CascataProjectFormComponent } from './cascata-project-form.component';

describe('CascataProjectFormComponent', () => {
  let component: CascataProjectFormComponent;
  let fixture: ComponentFixture<CascataProjectFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CascataProjectFormComponent]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CascataProjectFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
