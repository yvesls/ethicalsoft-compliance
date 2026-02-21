import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { MenuService } from '../../../../core/services/menu.service';
import { RouterService } from '../../../../core/services/router.service';
import { TemplateStore } from '../../../../shared/stores/template.store';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { RoleService } from '../../../../core/services/role.service';

import { IterativoProjectFormComponent } from './iterativo-project-form.component';

describe('IterativoProjectFormComponent', () => {
  let component: IterativoProjectFormComponent;
  let fixture: ComponentFixture<IterativoProjectFormComponent>;

  beforeEach(async () => {
    const routerServiceMock = {
      currentUrl: '/projects/create',
      getRouteInfoParams: () => of({ vid: 'spec-vid', route: '/projects/create', params: {}, queryParams: {} }),
      navigateTo: jasmine.createSpy('navigateTo').and.resolveTo(true),
      setStoredCurrentPage: jasmine.createSpy('setStoredCurrentPage'),
      getStoredPageViewParams: () => null,
      setStoredPageViewParams: jasmine.createSpy('setStoredPageViewParams'),
    };

    await TestBed.configureTestingModule({
      imports: [IterativoProjectFormComponent],
      providers: [
        { provide: MenuService, useValue: {} },
        { provide: RouterService, useValue: routerServiceMock },
        { provide: TemplateStore, useValue: { getAllTemplates: () => of([]), getFullTemplate: () => of(null) } },
        { provide: ProjectStore, useValue: { createProject: () => of(null) } },
        { provide: RoleService, useValue: { getRoles: () => of([]) } },
        {
          provide: ModalService,
          useValue: {
            open: jasmine.createSpy('open'),
            close: jasmine.createSpy('close'),
            getActiveInstance: () => null,
          },
        },
        {
          provide: NotificationService,
          useValue: {
            showSuccess: jasmine.createSpy('showSuccess'),
            showError: jasmine.createSpy('showError'),
            showWarning: jasmine.createSpy('showWarning'),
          },
        },
      ],
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
