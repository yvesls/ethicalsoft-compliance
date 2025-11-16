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
import { ProjectDatesValidators } from '../../../../shared/validators/project-dates.validator';
import { FormUtils } from '../../../../shared/utils/form-utils';
import {
  BasePageComponent,
  RestoreParams,
} from '../../../../core/abstractions/base-page.component';
import { TemplateStore } from '../../../../shared/stores/template.store';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { TemplateActionModalComponent } from '../template-action-modal/template-action-modal.component';
import { CreateStageCascataModalComponent, NewStageCascataData } from '../create-stage-cascata-modal/create-stage-cascata-modal.component';

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
  weight: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

@Component({
  selector: 'app-cascata-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AccordionPanelComponent,
    InputComponent,
    SelectComponent,
  ],
  templateUrl: './cascata-project-form.component.html',
  styleUrls: ['./cascata-project-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CascataProjectFormComponent extends BasePageComponent implements OnInit {
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

  public templateOptions: SelectOption[] = [];
  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Cascata, label: 'Cascata' },
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
          { value: ProjectType.Cascata, disabled: true },
          [Validators.required],
        ],
        startDate: [
          null,
          [
            Validators.required,
            ProjectDatesValidators.startDateAllowsExistingStages()
          ]
        ],
        deadline: [
          null,
          [
            CustomValidators.minDateToday(),
            ProjectDatesValidators.deadlineAllowsExistingStages()
          ]
        ],
        steps: this.buildCascataStepsForm(),
        representatives: this.buildRepresentativesForm(),
        questionnaires: this.fb.array([]), // Inicializa vazio
      },
      {
        validators: [
          CustomValidators.dateRange(
            'startDate',
            'deadline',
            'A data de início não pode ser maior que o prazo limite.'
          ),
          ProjectDatesValidators.stageApplicationRangeWithinDeadline(),
        ],
      }
    );

    this.syncQuestionnairesWithSteps();
    this.loadTemplates();
    this.setupTemplateListener();
    this.setupDateListeners();
  }

  protected override loadParams(params: any, queryParams?: any): void {
  }

  private loadTemplates(): void {
    this.templateStore.getAllTemplates()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (templates) => {
          const cascataTemplates = templates.filter(t => t.type === ProjectType.Cascata);
          this.templateOptions = cascataTemplates.map(t => ({
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

  private setupDateListeners(): void {
    this.projectForm.get('startDate')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((startDate) => {
        if (startDate) {
          this.recalculateAllStageRanges();
        }
      });

    this.projectForm.get('deadline')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.projectForm.get('startDate')?.updateValueAndValidity({ emitEvent: false });
        this.stepsFormArray.controls.forEach(stepControl => {
          stepControl.updateValueAndValidity({ emitEvent: false });
        });
      });
  }

  private recalculateAllStageRanges(): void {
    const startDate = this.projectForm.get('startDate')?.value;
    if (!startDate) return;

    let cumulativeDays = 0;

    this.stepsFormArray.controls.forEach((stepControl, index) => {
      const durationDays = stepControl.get('durationDays')?.value || 0;

      if (durationDays > 0) {
        const stageStartDate = new Date(startDate);
        const actualStageStart = FormUtils.addBusinessDays(stageStartDate, cumulativeDays);

        const openingOffset = Math.round(durationDays * 0.1);
        const closingOffset = Math.round(durationDays * 0.9);

        const applicationStartDate = FormUtils.addBusinessDays(actualStageStart, openingOffset);
        const applicationEndDate = FormUtils.addBusinessDays(actualStageStart, closingOffset);

        stepControl.patchValue({
          applicationStartDate: FormUtils.formatDateISO(applicationStartDate),
          applicationEndDate: FormUtils.formatDateISO(applicationEndDate),
          dateRange: `${FormUtils.formatDateBR(FormUtils.formatDateISO(applicationStartDate))} - ${FormUtils.formatDateBR(FormUtils.formatDateISO(applicationEndDate))}`
        }, { emitEvent: false });

        cumulativeDays += durationDays;
      }
    });

    this.sortStepsByApplicationDate();
    this.syncQuestionnairesWithSteps();
    this.cdr.detectChanges();
  }

  private sortStepsByApplicationDate(): void {
    const stepsArray = this.stepsFormArray.value;

    const sortedSteps = stepsArray.sort((a: any, b: any) => {
      const dateA = a.applicationStartDate ? new Date(a.applicationStartDate).getTime() : 0;
      const dateB = b.applicationStartDate ? new Date(b.applicationStartDate).getTime() : 0;
      return dateA - dateB;
    });

    while (this.stepsFormArray.length > 0) {
      this.stepsFormArray.removeAt(0);
    }

    sortedSteps.forEach((step: any) => {
      this.stepsFormArray.push(this.fb.group({
        name: [step.name, Validators.required],
        weight: [step.weight, [Validators.required, Validators.min(0)]],
        dateRange: [step.dateRange || ''],
        durationDays: [step.durationDays || 0],
        applicationStartDate: [step.applicationStartDate || ''],
        applicationEndDate: [step.applicationEndDate || ''],
      }));
    });
  }

  private loadTemplateData(templateId: string): void {
    this.templateStore.getFullTemplate(templateId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (template) => {
          if (template.stages && template.stages.length > 0) {
            const stepsData = template.stages.map(stage => ({
              name: stage.name,
              weight: stage.weight,
              dateRange: '',
              durationDays: 0,
              applicationStartDate: '',
              applicationEndDate: ''
            }));
            this.projectForm.setControl('steps', this.buildCascataStepsForm(stepsData));

            this.syncQuestionnairesWithSteps();
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

          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Erro ao carregar template completo:', error);
        }
      });
  }

  private buildCascataStepsForm(data?: any[]): FormArray {
    const defaults = [
      { name: 'Iniciação', weight: 3, dateRange: '', durationDays: 5 },
      { name: 'Requisitos', weight: 5, dateRange: '', durationDays: 21 },
      { name: 'Testes', weight: 1, dateRange: '', durationDays: 10 },
    ];
    const stepsData = data || defaults;
    const stepGroups = stepsData.map((step: any) => {
      return this.fb.group({
        name: [step.name, Validators.required],
        weight: [step.weight, [Validators.required, Validators.min(0)]],
        dateRange: [step.dateRange || ''],
        durationDays: [step.durationDays || 0],
        applicationStartDate: [step.applicationStartDate || ''],
        applicationEndDate: [step.applicationEndDate || ''],
      });
    });

    const stepsArray = this.fb.array(stepGroups, Validators.required);

    return stepsArray;
  }

  private buildRepresentativesForm(data?: Representative[]): FormArray {
    const representativesData = data || [];
    const repGroups = representativesData.map((rep: any) => {
      return this.fb.group({
        firstName: [rep.firstName, Validators.required],
        lastName: [rep.lastName, Validators.required],
        email: [rep.email, [Validators.required, Validators.email]],
        inclusionDate: [rep.inclusionDate, Validators.required],
        weight: [rep.weight, [Validators.required, Validators.min(0)]],
        roles: [rep.roles, Validators.required],
      });
    });
    return this.fb.array(repGroups);
  }

  addStep(): void {
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
    const modalRef = this.openModal<CreateStageCascataModalComponent>(CreateStageCascataModalComponent);

    if (modalRef) {
      modalRef.projectStartDate = this.projectForm.get('startDate')?.value;
      modalRef.projectDeadline = this.projectForm.get('deadline')?.value;
      modalRef.projectDurationDays = this.calculateProjectDuration();
      modalRef.existingStages = this.getExistingStagesData();
    }

    const subscription = modalRef.stageCreated.subscribe((newStage: NewStageCascataData) => {
      this.addNewStage(newStage);
      subscription.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  private openModal<T>(component: any): T {
    this.modalService.open(component, 'small-card');
    return (this.modalService as any).modalRef?.instance as T;
  }

  private addNewStage(newStage: NewStageCascataData): void {
    this.stepsFormArray.push(
      this.fb.group({
        name: [newStage.name, Validators.required],
        weight: [newStage.weight, [Validators.required, Validators.min(1)]],
        dateRange: [`${FormUtils.formatDateBR(newStage.applicationStartDate)} - ${FormUtils.formatDateBR(newStage.applicationEndDate)}`],
        durationDays: [newStage.durationDays],
        applicationStartDate: [newStage.applicationStartDate],
        applicationEndDate: [newStage.applicationEndDate],
      })
    );

    this.addQuestionnaireForStep(newStage.name, newStage.weight, newStage.applicationStartDate, newStage.applicationEndDate);
    this.cdr.detectChanges();
  }

  removeStep(index: number): void {
    if (this.stepsFormArray.length > 1) {
      const stepName = this.stepsFormArray.at(index).get('name')?.value;

      this.stepsFormArray.removeAt(index);

      this.removeQuestionnaireForStep(stepName);

      this.cdr.detectChanges();
    }
  }

  private addQuestionnaireForStep(
    stepName: string,
    stepWeight: number,
    startDate?: string,
    endDate?: string
  ): void {
    const newQuestionnaire = this.fb.group({
      name: [stepName, [Validators.required]],
      stageName: [stepName],
      weight: [stepWeight, [Validators.required, Validators.min(1)]],
      applicationStartDate: [startDate || new Date().toISOString().split('T')[0], [Validators.required]],
      applicationEndDate: [endDate || new Date().toISOString().split('T')[0], [Validators.required]],
    });

    this.questionnairesFormArray.push(newQuestionnaire);
  }

  private removeQuestionnaireForStep(stepName: string): void {
    const index = this.questionnairesFormArray.controls.findIndex(
      control => control.get('stageName')?.value === stepName
    );

    if (index !== -1) {
      this.questionnairesFormArray.removeAt(index);
    }
  }

  private syncQuestionnairesWithSteps(): void {
    while (this.questionnairesFormArray.length > 0) {
      this.questionnairesFormArray.removeAt(0);
    }

    this.stepsFormArray.controls.forEach((stepControl) => {
      const stepName = stepControl.get('name')?.value;
      const stepWeight = stepControl.get('weight')?.value;
      const startDate = stepControl.get('applicationStartDate')?.value;
      const endDate = stepControl.get('applicationEndDate')?.value;
      this.addQuestionnaireForStep(stepName, stepWeight, startDate, endDate);
    });

    this.cdr.detectChanges();
  }

  addRepresentative(): void {
    const newRep = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      inclusionDate: [new Date().toLocaleDateString('pt-BR'), Validators.required],
      weight: [1, [Validators.required, Validators.min(0)]],
      roles: ['', Validators.required],
    });
    this.representativesFormArray.push(newRep);
    this.cdr.detectChanges();
  }

  removeRepresentative(index: number): void {
    this.representativesFormArray.removeAt(index);
    this.cdr.detectChanges();
  }

  private buildQuestionnairesForm(data?: Questionnaire[]): FormArray {
    if (!data || data.length === 0) {
      return this.fb.array([]);
    }

    return this.fb.array(
      data.map((q) =>
        this.fb.group({
          name: [q.name, [Validators.required]],
          weight: [q.weight, [Validators.required, Validators.min(1)]],
          applicationStartDate: [q.applicationStartDate, [Validators.required]],
          applicationEndDate: [q.applicationEndDate, [Validators.required]],
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

        this.projectForm.setControl(
          'steps',
          this.buildCascataStepsForm(formValue.steps || [])
        );

        this.projectForm.setControl(
          'representatives',
          this.buildRepresentativesForm(formValue.representatives || [])
        );

        if (formValue.questionnaires && formValue.questionnaires.length > 0) {
          this.projectForm.setControl('questionnaires', this.buildQuestionnairesForm(formValue.questionnaires));
        } else {
          this.syncQuestionnairesWithSteps();
        }

        this.projectForm.patchValue(formValue);
      }

      if (restoreParameter['panelStates']) {
        this.panelStates = restoreParameter['panelStates'];
      }
    }

    this.cdr.detectChanges();
  }

  getControl(name: string): AbstractControl | null {
    return this.projectForm.get(name);
  }

  get stepsFormArray(): FormArray {
    return this.getControl('steps') as FormArray;
  }

  get representativesFormArray(): FormArray {
    return this.getControl('representatives') as FormArray;
  }

  get questionnairesFormArray(): FormArray {
    return this.getControl('questionnaires') as FormArray;
  }

  trackByIndex(index: number): number {
    return index;
  }

  private calculateProjectDuration(): number {
    const startDate = this.projectForm.get('startDate')?.value;
    const deadline = this.projectForm.get('deadline')?.value;

    if (!startDate || !deadline) {
      return 0;
    }

    return FormUtils.calculateBusinessDays(new Date(startDate), new Date(deadline));
  }

  private getExistingStagesData(): { weight: number; durationDays: number }[] {
    return this.stepsFormArray.controls.map(control => ({
      weight: control.get('weight')?.value || 0,
      durationDays: control.get('durationDays')?.value || 0
    }));
  }

  getDeadlineErrorMessage(): string {
    const error = this.projectForm.errors?.['deadlineTooEarly'];
    if (!error) return '';

    const { stageName, latestStageEnd, deadline } = error;

    return `O prazo limite (${deadline}) não permite acomodar a etapa "${stageName}" que termina em ${latestStageEnd}. Por favor, estenda o prazo limite ou ajuste os pesos das etapas.`;
  }

  getStartDateErrorMessage(): string {
    const error = this.projectForm.errors?.['startDateTooLate'];
    if (!error) return '';

    const { startDate, deadline, projectedEndDate, requiredDays } = error;

    return `Com a data de início atual (${startDate}), as etapas terminariam em ${projectedEndDate}, ultrapassando o prazo limite (${deadline}). As etapas requerem ${requiredDays} dias úteis. Por favor, antecipe a data de início ou ajuste os pesos das etapas.`;
  }

  getStageExceedsDeadlineMessage(): string {
    const error = this.projectForm.errors?.['stageExceedsDeadline'];
    if (!error) return '';

    const { stageName, stageEndDate, deadline } = error;

    return `A etapa "${stageName}" tem data de término (${stageEndDate}) que ultrapassa o prazo limite do projeto (${deadline}). Ajuste o prazo limite do projeto ou reduza o peso desta etapa.`;
  }  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }
  }
}
