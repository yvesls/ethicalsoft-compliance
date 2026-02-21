import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { NgxSpinnerService } from 'ngx-spinner';
import { ModalService } from '../../../../core/services/modal.service';
import { AuthenticationService } from '../../../../core/services/authentication.service';

import { ProjectListPageComponent } from './project-list-page.component';

describe('ProjectListPageComponent', () => {
  let component: ProjectListPageComponent;
  let fixture: ComponentFixture<ProjectListPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectListPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ProjectStore,
          useValue: {
            getProjects: () =>
              of({
                content: [],
                pageable: {
                  offset: 0,
                  pageNumber: 0,
                  pageSize: 10,
                  paged: true,
                  sort: { empty: true, sorted: false, unsorted: true },
                },
                size: 10,
                totalElements: 0,
                totalPages: 0,
                number: 0,
                first: true,
                last: true,
                empty: true,
                numberOfElements: 0,
              }),
          },
        },
        {
          provide: NgxSpinnerService,
          useValue: {
            show: jasmine.createSpy('show'),
            hide: jasmine.createSpy('hide'),
          },
        },
        { provide: ModalService, useValue: { open: jasmine.createSpy('open') } },
        { provide: AuthenticationService, useValue: { userRoles$: of([]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectListPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
