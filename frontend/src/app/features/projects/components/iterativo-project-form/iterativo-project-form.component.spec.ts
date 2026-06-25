import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { MenuService } from '../../../../core/services/menu.service';
import { RouterService } from '../../../../core/services/router.service';
import { TemplateStore } from '../../../../shared/stores/template.store';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { RoleService } from '../../../../core/services/role.service';
import { ProjectType } from '../../../../shared/enums/project-type.enum';

import { IterativoProjectFormComponent } from './iterativo-project-form.component';

describe('IterativoProjectFormComponent', () => {
  let component: IterativoProjectFormComponent;
  let fixture: ComponentFixture<IterativoProjectFormComponent>;
  let templateStoreMock: {
    getAllTemplates: jasmine.Spy;
    getFullTemplate: jasmine.Spy;
  };

  beforeEach(async () => {
    templateStoreMock = {
      getAllTemplates: jasmine.createSpy('getAllTemplates').and.returnValue(of([])),
      getFullTemplate: jasmine.createSpy('getFullTemplate').and.returnValue(of(null)),
    };

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
        { provide: TemplateStore, useValue: templateStoreMock },
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

  it('should load questionnaire name and weight from selected template', () => {
    const fullTemplate = {
      id: 'template-1',
      name: 'Template Iterativo',
      description: 'desc',
      visibility: 'PRIVATE' as const,
      type: ProjectType.Iterativo,
      defaultIterationCount: 1,
      defaultIterationDuration: 5,
      iterations: [{ name: 'Sprint 1', weight: 10 }],
      questionnaires: [
        {
          name: 'Questionario Inicial',
          iterationRefName: 'Sprint 1',
          weight: 35,
          questions: [],
        },
      ],
    };

    templateStoreMock.getFullTemplate.and.returnValue(of(fullTemplate));

    component.projectForm.patchValue({
      template: 'template-1',
      startDate: '2026-06-22',
      deadline: '2026-06-30',
      iterationDuration: 5,
      iterationCount: 1,
    });

    component.onPanelToggled('stages', true);
    component['loadPanelData']('stages');
    component['loadPanelData']('questionnaires');

    const questionnaire = component.questionnairesFormArray.at(0);

    expect(questionnaire?.get('name')?.value).toBe('Questionario Inicial');
    expect(questionnaire?.get('iteration')?.value).toBe('Sprint 1');
    expect(questionnaire?.get('weight')?.value).toBe(35);
  });
});
