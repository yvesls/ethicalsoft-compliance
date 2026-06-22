import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { NgxSpinnerService } from 'ngx-spinner';
import { ModalService } from '../../../../core/services/modal.service';
import { AuthenticationService } from '../../../../core/services/authentication.service';

import { ProjectListPageComponent } from './project-list-page.component';

describe('ProjectListPageComponent', () => {
  let component: ProjectListPageComponent;
  let fixture: ComponentFixture<ProjectListPageComponent>;
  let queryParamMapSubject: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let projectStore: jasmine.SpyObj<ProjectStore>;
  let spinner: { show: jasmine.Spy; hide: jasmine.Spy };

  const emptyPageResponse = {
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
  };

  beforeEach(async () => {
    queryParamMapSubject = new BehaviorSubject(convertToParamMap({}));
    projectStore = jasmine.createSpyObj<ProjectStore>('ProjectStore', ['getProjects']);
    projectStore.getProjects.and.returnValue(of(emptyPageResponse));
    spinner = {
      show: jasmine.createSpy('show'),
      hide: jasmine.createSpy('hide'),
    };

    await TestBed.configureTestingModule({
      imports: [ProjectListPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ProjectStore,
          useValue: projectStore,
        },
        {
          provide: NgxSpinnerService,
          useValue: spinner,
        },
        { provide: ModalService, useValue: { open: jasmine.createSpy('open') } },
        { provide: AuthenticationService, useValue: { userRoles$: of([]) } },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: queryParamMapSubject.asObservable(),
            snapshot: { queryParamMap: convertToParamMap({}) },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectListPageComponent);
    component = fixture.componentInstance;
  });

  it('should create', fakeAsync(() => {
    fixture.detectChanges();
    tick(300);
    expect(component).toBeTruthy();
  }));

  it('should keep the loaded state when searching without changing filters', fakeAsync(() => {
    fixture.detectChanges();
    tick(300);

    expect(component.status()).toBe('loaded');
    expect(projectStore.getProjects).toHaveBeenCalledTimes(1);

    queryParamMapSubject.next(convertToParamMap({ page: '1' }));
    tick(300);

    expect(projectStore.getProjects).toHaveBeenCalledTimes(1);
    expect(component.status()).toBe('loaded');
    expect(spinner.show).toHaveBeenCalledTimes(1);
  }));
});
