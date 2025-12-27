import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectTypeSelectionComponent } from './project-type-selection.component';

describe('ProjectTypeSelectionComponent', () => {
  let component: ProjectTypeSelectionComponent;
  let fixture: ComponentFixture<ProjectTypeSelectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectTypeSelectionComponent]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectTypeSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
