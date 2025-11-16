import {
  Component,
  ChangeDetectionStrategy,
  inject,
  DestroyRef,
  ChangeDetectorRef,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  FormArray,
} from '@angular/forms';

import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { SelectOption } from '../../../../shared/components/select/select.component';
import { AccordionPanelComponent } from '../../../../shared/components/accordion-panel/accordion-panel.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent } from '../../../../shared/components/select/select.component';

import { CustomValidators } from '../../../../shared/validators/custom.validator';
import {
  BasePageComponent,
  RestoreParams,
} from '../../../../core/abstractions/base-page.component';
import { TemplateStore } from '../../../../shared/stores/template.store';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { TemplateActionModalComponent } from '../template-action-modal/template-action-modal.component';
import { CreateStageIterativeModalComponent, NewStageIterativeData } from '../create-stage-iterative-modal/create-stage-iterative-modal.component';

export interface Representative {
  firstName: string;
  lastName: string;
  email: string;
  inclusionDate: string;
  weight: number;
  roles: string;
}

export interface Questionnaire {
  name: string;
  iteration: string;
  weight: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

@Component({
  selector: 'app-iterativo-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AccordionPanelComponent,
    InputComponent,
    SelectComponent,
  ],
  templateUrl: './iterativo-project-form.component.html',
  styleUrls: ['./iterativo-project-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IterativoProjectFormComponent extends BasePageComponent implements OnInit {
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  private templateStore = inject(TemplateStore);
  private modalService = inject(ModalService);
  private notificationService = inject(NotificationService);

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public panelStates = {
    project: true,
    steps: false,
    representatives: false,
    questionnaires: false,
  };

  public iterativeSteps = [
    'Iniciação',
    'Desenvolvimento',
    'Requisitos',
    'Testes',
  ];

  public templateOptions: SelectOption[] = [];
  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Iterativo, label: 'Iterativo Incremental' },
  ];

  constructor() {
    super();
  }

  protected override onInit(): void {
    this.projectForm = this.fb.group(
      {
        template: [null, [Validators.required]],
        name: ['', [Validators.required]],
        type: [
          { value: ProjectType.Iterativo, disabled: true },
          [Validators.required],
        ],
        startDate: [null, [Validators.required]],
        deadline: [null, [CustomValidators.minDateToday()]],
        iterationDuration: [null, [Validators.required, Validators.min(1)]],
        iterationCount: [null, [Validators.required, Validators.min(1)]],
        steps: this.buildIterativoStepsForm(),
        representatives: this.buildRepresentativesForm(),
        questionnaires: this.buildQuestionnairesForm(),
      },
      {
        validators: [
          CustomValidators.dateRange(
            'startDate',
            'deadline',
            'A data de início não pode ser maior que o prazo limite.'
          ),
        ],
      }
    );

    this.loadTemplates();
    this.setupTemplateListener();
  }

  protected override loadParams(params: any, queryParams?: any): void {

  }

  private loadTemplates(): void {
    this.templateStore.getAllTemplates()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (templates) => {
          const iterativoTemplates = templates.filter(t => t.type === ProjectType.Iterativo);
          this.templateOptions = iterativoTemplates.map(t => ({
            value: t.id,
            label: t.name
          }));
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Erro ao carregar templates:', error);
        }
      });
  }

  private setupTemplateListener(): void {
    this.projectForm.get('template')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((templateId) => {
        if (templateId) {
          this.loadTemplateData(templateId);
        }
      });
  }

  private loadTemplateData(templateId: string): void {
    this.templateStore.getFullTemplate(templateId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (template) => {
          if (template.defaultIterationDuration) {
            this.projectForm.patchValue({
              iterationDuration: template.defaultIterationDuration
            });
          }
          if (template.defaultIterationCount) {
            this.projectForm.patchValue({
              iterationCount: template.defaultIterationCount
            });
          }

          if (template.iterations && template.iterations.length > 0) {
           this.iterativeSteps = template.iterations.map(iter => iter.name);

            const stepsFormGroup = this.fb.group({});
            template.iterations.forEach(iter => {
              const controlName = iter.name.toLowerCase().replace(/\s+/g, '_');
              stepsFormGroup.addControl(controlName, this.fb.control(iter.weight, Validators.required));
            });
            this.projectForm.setControl('steps', stepsFormGroup);
          }

          if (template.representatives && template.representatives.length > 0) {
            const repsData = template.representatives.map(rep => ({
              firstName: rep.firstName,
              lastName: rep.lastName,
              email: rep.email,
              inclusionDate: new Date().toLocaleDateString('pt-BR'),
              weight: rep.weight,
              roles: rep.roleNames.join(', ')
            }));
            this.projectForm.setControl('representatives', this.buildRepresentativesForm(repsData));
          }

          if (template.questionnaires && template.questionnaires.length > 0) {
            const questionnairesData = template.questionnaires.map(q => ({
              name: q.name,
              iteration: q.iterationRefName || '',
              weight: 1,
              applicationStartDate: new Date().toISOString().split('T')[0],
              applicationEndDate: new Date().toISOString().split('T')[0]
            }));
            this.projectForm.setControl('questionnaires', this.buildQuestionnairesForm(questionnairesData));
          }

          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('❌ Erro ao carregar template completo iterativo:', error);
        }
      });
  }

  private _initForm(): void {
    this.projectForm = this.fb.group(
      {
        template: [null, [Validators.required]],
        name: ['', [Validators.required]],
        type: [
          { value: ProjectType.Iterativo, disabled: true },
          [Validators.required],
        ],
        startDate: [null, [Validators.required]],
        deadline: [null, [CustomValidators.minDateToday()]],
        iterationDuration: [null, [Validators.required, Validators.min(1)]],
        iterationCount: [null, [Validators.required, Validators.min(1)]],
        steps: this.buildIterativoStepsForm(),
        representatives: this.buildRepresentativesForm(),
        questionnaires: this.buildQuestionnairesForm(),
      },
      {
        validators: [
          CustomValidators.dateRange(
            'startDate',
            'deadline',
            'A data de início não pode ser maior que o prazo limite.'
          ),
        ],
      }
    );
  }

  private buildIterativoStepsForm(data?: any): FormGroup {
    const defaults = {
      initiation: 3,
      development: 3,
      requirements: 5,
      tests: 1,
    };
    const formData = data || defaults;
    return this.fb.group({
      initiation: [formData.initiation, Validators.required],
      development: [formData.development, Validators.required],
      requirements: [formData.requirements, Validators.required],
      tests: [formData.tests, Validators.required],
    });
  }

  private buildRepresentativesForm(data?: Representative[]): FormArray {
    const defaults: Representative[] = [
      {
        firstName: 'Maria',
        lastName: 'Visconde',
        email: 'cliente1@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 1,
        roles: 'Desenvolvedor(a), Líder de Equipe',
      },
      {
        firstName: 'João',
        lastName: 'Ferreira',
        email: 'cliente2@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 2,
        roles: 'Desenvolvedor(a)',
      },
      {
        firstName: 'Carlos',
        lastName: 'Moreno',
        email: 'cliente3@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 1,
        roles: 'Analista de Negócios',
      },
      {
        firstName: 'Samara',
        lastName: 'Alviverde',
        email: 'cliente4@projeto.com',
        inclusionDate: '21/12/2024',
        weight: 3,
        roles: 'Gerente de Projeto',
      },
    ];

    const representativesData = data || defaults;

    return this.fb.array(
      representativesData.map((rep) =>
        this.fb.group({
          firstName: [rep.firstName, [Validators.required]],
          lastName: [rep.lastName, [Validators.required]],
          email: [rep.email, [Validators.required, Validators.email]],
          inclusionDate: [rep.inclusionDate, [Validators.required]],
          weight: [rep.weight, [Validators.required, Validators.min(1)]],
          roles: [rep.roles, [Validators.required]],
        })
      )
    );
  }

  protected override save(): any {
    return {
      formValue: this.projectForm.getRawValue(),
      panelStates: this.panelStates,
    };
  }

  protected override restore(restoreParameter: RestoreParams<any>): void {
    if (restoreParameter.hasParams) {
      if (restoreParameter['formValue']) {
        const formValue = restoreParameter['formValue'];
        if (formValue.steps) {
          this.iterativeSteps = Object.keys(formValue.steps).map(key =>
            key.split('_').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ')
          );
        }

        this.projectForm.setControl(
          'steps',
          this.buildIterativoStepsForm(formValue.steps || {})
        );

        this.projectForm.setControl(
          'representatives',
          this.buildRepresentativesForm(formValue.representatives || [])
        );

        this.projectForm.setControl(
          'questionnaires',
          this.buildQuestionnairesForm(formValue.questionnaires || [])
        );

        this.projectForm.patchValue(formValue);
      }

      if (restoreParameter['panelStates']) {
        this.panelStates = restoreParameter['panelStates'];
      }
    }

    this.cdr.markForCheck();
  }

  getControl(name: string): AbstractControl | null {
    return this.projectForm.get(name);
  }

  get stepsFormGroup(): FormGroup {
    return this.getControl('steps') as FormGroup;
  }

  get representativesFormArray(): FormArray {
    return this.getControl('representatives') as FormArray;
  }

  get questionnairesFormArray(): FormArray {
    return this.getControl('questionnaires') as FormArray;
  }

  addRepresentative(): void {
    const newRep = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      inclusionDate: [new Date().toLocaleDateString('pt-BR'), [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]],
      roles: ['', [Validators.required]],
    });
    this.representativesFormArray.push(newRep);
    this.cdr.detectChanges();
  }

  removeRepresentative(index: number): void {
    this.representativesFormArray.removeAt(index);
    this.cdr.detectChanges();
  }

  private buildQuestionnairesForm(data?: Questionnaire[]): FormArray {
    const iterationCount = this.projectForm?.get('iterationCount')?.value || 4;
    const defaults: Questionnaire[] = Array.from({ length: iterationCount }, (_, i) => ({
      name: `Questionário ${i + 1}`,
      iteration: `Sprint ${i + 1}`,
      weight: 1,
      applicationStartDate: new Date().toISOString().split('T')[0],
      applicationEndDate: new Date().toISOString().split('T')[0],
    }));

    const questionnairesData = data || defaults;

    return this.fb.array(
      questionnairesData.map((q) =>
        this.fb.group({
          name: [q.name, [Validators.required]],
          iteration: [q.iteration, [Validators.required]],
          weight: [q.weight, [Validators.required, Validators.min(1)]],
          applicationStartDate: [q.applicationStartDate, [Validators.required]],
          applicationEndDate: [q.applicationEndDate, [Validators.required]],
        })
      )
    );
  }

  addQuestionnaire(): void {
    const newQ = this.fb.group({
      name: ['', [Validators.required]],
      iteration: ['', [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]],
      applicationStartDate: [new Date().toISOString().split('T')[0], [Validators.required]],
      applicationEndDate: [new Date().toISOString().split('T')[0], [Validators.required]],
    });
    this.questionnairesFormArray.push(newQ);
    this.cdr.detectChanges();
  }

  removeQuestionnaire(index: number): void {
    this.questionnairesFormArray.removeAt(index);
    this.cdr.detectChanges();
  }

  getIterativeStepWeight(stepName: string): number {
    const controlName = stepName.toLowerCase().replace(/\s+/g, '_');
    const control = this.stepsFormGroup?.get(controlName);
    return control ? control.value || 0 : 0;
  }

  addIterativeStep(): void {
    const modalRef = this.openModal<TemplateActionModalComponent>(TemplateActionModalComponent);

    const actionSubscription = modalRef.actionSelected.subscribe((action: 'create' | 'import') => {
      if (action === 'create') {
        const closeSubscription = this.modalService.modalClosed$
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe(() => {
            this.openCreateStageModal();
            closeSubscription.unsubscribe();
          });
      } else if (action === 'import') {
        this.notificationService.showWarning('Funcionalidade em desenvolvimento');
      }
      actionSubscription.unsubscribe();
    });
  }

  private openCreateStageModal(): void {
    const modalRef = this.openModal<CreateStageIterativeModalComponent>(CreateStageIterativeModalComponent);

    const subscription = modalRef.stageCreated.subscribe((newStage: NewStageIterativeData) => {
      this.addNewIterativeStage(newStage);
      subscription.unsubscribe();
    });
  }

  private openModal<T>(component: any): T {
    this.modalService.open(component, 'small-card');
    return (this.modalService as any).modalRef?.instance as T;
  }

  private addNewIterativeStage(newStage: NewStageIterativeData): void {
    this.iterativeSteps.push(newStage.name);
    const controlName = newStage.name.toLowerCase().replace(/\s+/g, '_');
    this.stepsFormGroup.addControl(
      controlName,
      this.fb.control(newStage.weight, [Validators.required, Validators.min(0)])
    );

    this.cdr.detectChanges();
  }

  removeIterativeStep(stepName: string): void {
    if (this.iterativeSteps.length > 1) {
      const index = this.iterativeSteps.indexOf(stepName);
      if (index > -1) {
        this.iterativeSteps.splice(index, 1);
        this.stepsFormGroup.removeControl(
          stepName.toLowerCase().replace(/\s+/g, '_')
        );
        this.cdr.detectChanges();
      }
    }
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }
  }
}
