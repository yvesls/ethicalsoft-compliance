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
import { StagesDeadlineValidator } from '../../../../shared/validators/stages-deadline.validator';
import { FormUtils } from '../../../../shared/utils/form-utils';
import { capitalizeWords } from '../../../../core/utils/common-utils';
import {
  BasePageComponent,
  RestoreParams,
} from '../../../../core/abstractions/base-page.component';
import { TemplateStore } from '../../../../shared/stores/template.store';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { RouterService } from '../../../../core/services/router.service';
import { TemplateActionModalComponent } from '../template-action-modal/template-action-modal.component';
import { StageCascataModalComponent, StageCascataData } from '../stage-cascata-modal/stage-cascata-modal.component';
import { RepresentativeModalComponent, RepresentativeData } from '../representative-modal/representative-modal.component';
import { QuestionData } from '../question-modal/question-modal.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';

export interface Representative {
  firstName: string;
  lastName: string;
  email: string;
  weight: number;
  roles: string[];
}

export interface Questionnaire {
  name: string;
  sequence: number;
  applicationStartDate: string;
  applicationEndDate: string;
  stageName?: string;
  questions?: QuestionData[];
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
  public override routerService = inject(RouterService);

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

  private hasRestoredSteps = false;
  private lastValidPanelIndex = 0;
  private selectedTemplateData: any = null;

  private readonly PANEL_ORDER = ['project', 'steps', 'representatives', 'questionnaires'] as const;

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
            Validators.required,
            CustomValidators.minDateToday(),
            ProjectDatesValidators.deadlineAllowsExistingStages()
          ]
        ],
        steps: this.buildCascataStepsForm(),
        representatives: this.buildRepresentativesForm(),
        questionnaires: this.fb.array([]),
      },
      {
        validators: [
          CustomValidators.dateRange(
            'startDate',
            'deadline',
            'A data de início não pode ser maior que o prazo limite.'
          ),
          ProjectDatesValidators.stageApplicationRangeWithinDeadline(),
          StagesDeadlineValidator.stagesFitWithinDeadline()
        ],
      }
    );

    this.syncQuestionnairesWithSteps();
    this.loadTemplates();
    this.setupTemplateListener();
    this.setupDateListeners();
  }

  protected override loadParams(params: any, queryParams?: any): void {
    if (params?.questionnaireUpdate) {
      this.applyQuestionnaireUpdate(params.questionnaireUpdate);
      delete params.questionnaireUpdate;
    }
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

  private hasUserModifiedSteps(): boolean {
    const stepsArray = this.projectForm.get('steps') as FormArray;
    return this.hasRestoredSteps || (stepsArray && stepsArray.length > 0);
  }

  private setupDateListeners(): void {
    this.projectForm.get('startDate')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((startDate) => {
        if (startDate && this.stepsFormArray.length > 0) {
          this.recalculateAllStageRanges();
        }
      });

    this.projectForm.get('deadline')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((deadline) => {
        if (deadline && this.stepsFormArray.length > 0) {
          this.recalculateAllStageRanges();
        }

        this.projectForm.get('startDate')?.updateValueAndValidity({ emitEvent: false });
        this.stepsFormArray.controls.forEach(stepControl => {
          stepControl.updateValueAndValidity({ emitEvent: false });
        });
      });
  }

  private recalculateAllStageRanges(): void {
    const startDate = this.projectForm.get('startDate')?.value;
    if (!startDate) return;

    const sortedSteps = this.getSortedStepsBySequence();

    let previousStageEndDate: Date = new Date(startDate);

    sortedSteps.forEach((stepInfo) => {
      const stepControl = this.stepsFormArray.at(stepInfo.index);
      const durationDays = Number(stepControl.get('durationDays')?.value) || 0;

      if (durationDays > 0) {
        const stageStartDate = new Date(previousStageEndDate);

        const openingOffset = Math.round(durationDays * 0.1);
        const closingOffset = Math.round(durationDays * 0.9);

        const applicationStartDate = FormUtils.addBusinessDays(stageStartDate, openingOffset);
        const applicationEndDate = FormUtils.addBusinessDays(stageStartDate, closingOffset);

        stepControl.patchValue({
          applicationStartDate: FormUtils.formatDateISO(applicationStartDate),
          applicationEndDate: FormUtils.formatDateISO(applicationEndDate),
          dateRange: `${FormUtils.formatDateBR(FormUtils.formatDateISO(applicationStartDate))} - ${FormUtils.formatDateBR(FormUtils.formatDateISO(applicationEndDate))}`
        }, { emitEvent: false });

        previousStageEndDate = FormUtils.addBusinessDays(stageStartDate, durationDays);
      }
    });

    this.syncQuestionnairesWithSteps();
    this.cdr.detectChanges();
  }

  private getSortedStepsBySequence(): { index: number; sequence: number }[] {
    return this.stepsFormArray.controls
      .map((control, index) => ({
        index,
        sequence: control.get('sequence')?.value || 999
      }))
      .sort((a, b) => a.sequence - b.sequence);
  }

  private loadTemplateData(templateId: string): void {
    this.templateStore.getFullTemplate(templateId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (template) => {
          this.selectedTemplateData = template;

          this.projectForm.patchValue({
            name: template.name
          });

          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Erro ao carregar template completo:', error);
        }
      });
  }

  private buildCascataStepsForm(data?: any[]): FormArray {
    const stepsData = data || [];
    const stepGroups = stepsData.map((step: any) => {
      return this.fb.group({
        name: [step.name, Validators.required],
        weight: [step.weight, [Validators.required, Validators.min(0)]],
        sequence: [step.sequence || 1, [Validators.required, Validators.min(1)]],
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
        weight: [rep.weight, [Validators.required, Validators.min(1)]],
        roles: [rep.roles, Validators.required],
      });
    });
    return this.fb.array(repGroups);
  }

  addStep(): void {
    this.modalService.open(TemplateActionModalComponent, 'small-card');
    const modalRef = (this.modalService as any).modalRef?.instance as TemplateActionModalComponent;

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
    const projectStart = this.projectForm.get('startDate')?.value;
    const projectDeadline = this.projectForm.get('deadline')?.value;
    const existingStages = this.getExistingStagesData();

    this.modalService.open(StageCascataModalComponent, 'small-card', {
      projectStartDate: projectStart,
      projectDeadline: projectDeadline,
      projectDurationDays: this.calculateProjectDuration(),
      existingStages: existingStages
    });

    const modalRef = (this.modalService as any).modalRef?.instance as StageCascataModalComponent;

    const createdSubscription = modalRef.stageCreated.subscribe((newStage: StageCascataData) => {
      this.addNewStage(newStage);
      createdSubscription.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  editStep(index: number): void {
    const stepFormGroup = this.stepsFormArray.at(index) as FormGroup;
    const projectStart = this.projectForm.get('startDate')?.value;
    const projectDeadline = this.projectForm.get('deadline')?.value;
    const existingStages = this.getExistingStagesDataExcluding(index);

    this.modalService.open(StageCascataModalComponent, 'medium-card', {
      mode: ActionType.EDIT,
      editData: {
        id: String(index),
        name: stepFormGroup.get('name')?.value,
        weight: stepFormGroup.get('weight')?.value,
        sequence: stepFormGroup.get('sequence')?.value,
        durationDays: stepFormGroup.get('durationDays')?.value,
        applicationStartDate: stepFormGroup.get('applicationStartDate')?.value,
        applicationEndDate: stepFormGroup.get('applicationEndDate')?.value
      },
      projectStartDate: projectStart,
      projectDeadline: projectDeadline,
      projectDurationDays: this.calculateProjectDuration(),
      existingStages: existingStages
    });

    const modalRef = (this.modalService as any).modalRef?.instance as StageCascataModalComponent;

    const updatedSubscription = modalRef.stageUpdated.subscribe((updatedStage: StageCascataData) => {
      this.updateStage(index, updatedStage);
      updatedSubscription.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  private getExistingStagesDataExcluding(excludeIndex: number): { weight: number; durationDays: number; sequence: number }[] {
    return this.stepsFormArray.controls
      .map((control, idx) => ({
        weight: Number(control.get('weight')?.value) || 0,
        durationDays: Number(control.get('durationDays')?.value) || 0,
        sequence: Number(control.get('sequence')?.value) || 1,
        index: idx
      }))
      .filter(stage => stage.index !== excludeIndex)
      .map(stage => ({
        weight: stage.weight,
        durationDays: stage.durationDays,
        sequence: stage.sequence
      }));
  }

  private handleSequenceOnCreate(newSequence: number): void {
    this.stepsFormArray.controls.forEach((control) => {
      const currentSequence = control.get('sequence')?.value;

      if (currentSequence >= newSequence) {
        control.patchValue({ sequence: currentSequence + 1 }, { emitEvent: false });
      }
    });
  }

  private handleSequenceOnEdit(editIndex: number, newSequence: number): void {
    const editedStepControl = this.stepsFormArray.at(editIndex);
    const oldSequence = editedStepControl.get('sequence')?.value;

    if (oldSequence === newSequence) {
      return;
    }

    const conflictIndex = this.stepsFormArray.controls.findIndex(
      (control, idx) => idx !== editIndex && control.get('sequence')?.value === newSequence
    );

    if (conflictIndex !== -1) {
      const conflictControl = this.stepsFormArray.at(conflictIndex);
      conflictControl.patchValue({ sequence: oldSequence }, { emitEvent: false });
    }
    editedStepControl.patchValue({ sequence: newSequence }, { emitEvent: false });
  }

  private updateStage(index: number, updatedStage: StageCascataData): void {
    const stepFormGroup = this.stepsFormArray.at(index) as FormGroup;
    const oldStepName = stepFormGroup.get('name')?.value;

    this.handleSequenceOnEdit(index, updatedStage.sequence);

    stepFormGroup.patchValue({
      name: updatedStage.name,
      weight: updatedStage.weight,
      sequence: updatedStage.sequence,
      durationDays: updatedStage.durationDays,
      dateRange: `${FormUtils.formatDateBR(updatedStage.applicationStartDate)} - ${FormUtils.formatDateBR(updatedStage.applicationEndDate)}`,
      applicationStartDate: updatedStage.applicationStartDate,
      applicationEndDate: updatedStage.applicationEndDate
    });

    this.recalculateAllStageRanges();
    this.cdr.detectChanges();
  }

  private addNewStage(newStage: StageCascataData): void {
    this.handleSequenceOnCreate(newStage.sequence);

    this.stepsFormArray.push(
      this.fb.group({
        name: [newStage.name, Validators.required],
        weight: [newStage.weight, [Validators.required, Validators.min(1)]],
        sequence: [newStage.sequence, [Validators.required, Validators.min(1)]],
        dateRange: [`${FormUtils.formatDateBR(newStage.applicationStartDate)} - ${FormUtils.formatDateBR(newStage.applicationEndDate)}`],
        durationDays: [newStage.durationDays],
        applicationStartDate: [newStage.applicationStartDate],
        applicationEndDate: [newStage.applicationEndDate],
      })
    );

    this.recalculateAllStageRanges();
    this.cdr.detectChanges();
  }

  removeStep(index: number): void {
    if (this.stepsFormArray.length > 1) {
      const stepName = this.stepsFormArray.at(index).get('name')?.value;

      this.stepsFormArray.removeAt(index);
      this.recalculateAllStageRanges();
      this.cdr.detectChanges();
    }
  }

  private syncQuestionnairesWithSteps(): void {
    const previousValues = this.questionnairesFormArray.getRawValue() as Questionnaire[];
    const questionnaireByStage = new Map(
      (previousValues || []).map((item) => [item.stageName || item.name, item])
    );
    while (this.questionnairesFormArray.length > 0) {
      this.questionnairesFormArray.removeAt(0);
    }

    const sortedSteps = this.getSortedStepsBySequence();

    sortedSteps.forEach((stepInfo) => {
      const stepControl = this.stepsFormArray.at(stepInfo.index);
      const stepName = stepControl.get('name')?.value;
      const startDate = stepControl.get('applicationStartDate')?.value || '';
      const endDate = stepControl.get('applicationEndDate')?.value || '';
      const cached = questionnaireByStage.get(stepName);

      this.questionnairesFormArray.push(
        this.fb.group({
          name: [stepName, [Validators.required]],
          stageName: [stepName],
          sequence: [stepInfo.sequence],
          applicationStartDate: [startDate],
          applicationEndDate: [endDate],
          questions: [cached?.questions || []],
        })
      );
    });

    this.cdr.detectChanges();
  }

  addRepresentative(): void {
    this.modalService.open(RepresentativeModalComponent, 'medium-card', {
      mode: ActionType.CREATE
    });

    const modalRef = (this.modalService as any).modalRef?.instance as RepresentativeModalComponent;

    const createdSubscription = modalRef.representativeCreated.subscribe((newRepresentative: RepresentativeData) => {
      const newRep = this.fb.group({
        firstName: [newRepresentative.firstName, Validators.required],
        lastName: [newRepresentative.lastName, Validators.required],
        email: [newRepresentative.email, [Validators.required, Validators.email]],
        weight: [newRepresentative.weight, [Validators.required, Validators.min(1)]],
        roles: [newRepresentative.roles, Validators.required],
      });
      this.representativesFormArray.push(newRep);
      createdSubscription.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  editRepresentative(index: number): void {
    const repFormGroup = this.representativesFormArray.at(index) as FormGroup;

    this.modalService.open(RepresentativeModalComponent, 'medium-card', {
      mode: ActionType.EDIT,
      editData: {
        id: String(index),
        firstName: repFormGroup.get('firstName')?.value,
        lastName: repFormGroup.get('lastName')?.value,
        email: repFormGroup.get('email')?.value,
        weight: repFormGroup.get('weight')?.value,
        roles: repFormGroup.get('roles')?.value
      }
    });

    const modalRef = (this.modalService as any).modalRef?.instance as RepresentativeModalComponent;

    const updatedSubscription = modalRef.representativeUpdated.subscribe((updatedRepresentative: RepresentativeData) => {
      repFormGroup.patchValue({
        firstName: updatedRepresentative.firstName,
        lastName: updatedRepresentative.lastName,
        email: updatedRepresentative.email,
        weight: updatedRepresentative.weight,
        roles: updatedRepresentative.roles
      });
      updatedSubscription.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  removeRepresentative(index: number): void {
    this.representativesFormArray.removeAt(index);
    this.cdr.detectChanges();
  }

  editQuestionnaire(index: number): void {
    const questionnaireGroup = this.questionnairesFormArray.at(index) as FormGroup | null;
    const questionnaire = questionnaireGroup?.getRawValue();
    if (!questionnaire) {
      this.notificationService.showWarning('Não foi possível carregar o questionário selecionado.');
      return;
    }
    const projectName = this.projectForm.get('name')?.value || 'Novo Projeto';

    // Buscar as perguntas do template correspondente
    const questions = this.getQuestionsForQuestionnaire(questionnaire);

    this.routerService.navigateTo('/projects/questionnaire/cascata', {
      params: {
        p: {
          projectName: projectName,
          questionnaireIndex: index,
          sequence: questionnaire.sequence,
          name: questionnaire.name,
          applicationStartDate: questionnaire.applicationStartDate,
          applicationEndDate: questionnaire.applicationEndDate,
          stageName: questionnaire.stageName,
          questions: questions
        }
      }
    });
  }

  private getQuestionsForQuestionnaire(questionnaire: any): QuestionData[] {
    if (questionnaire?.questions?.length) {
      return questionnaire.questions;
    }

    if (!this.selectedTemplateData?.questionnaires) {
      return [];
    }

    // Encontrar o questionário correspondente no template pela etapa
    const templateQuestionnaire = this.selectedTemplateData.questionnaires.find(
      (tq: any) => tq.stageName === questionnaire.stageName || tq.name === questionnaire.name
    );

    if (!templateQuestionnaire?.questions) {
      return [];
    }

    // Mapear as perguntas do template para o formato esperado pelo componente
    return templateQuestionnaire.questions.map((question: any, index: number) => ({
      id: `${Date.now()}-${index}`,
      value: question.value,
      roleNames: Array.from(question.roleNames || [])
    }));
  }

  private applyQuestionnaireUpdate(update: any): void {
    if (!update || typeof update.questionnaireIndex !== 'number') {
      return;
    }

    const questionnaireGroup = this.questionnairesFormArray.at(update.questionnaireIndex) as FormGroup | null;
    if (!questionnaireGroup) {
      return;
    }

    questionnaireGroup.patchValue(
      {
        name: update.name,
        sequence: update.sequence,
        applicationStartDate: update.applicationStartDate,
        applicationEndDate: update.applicationEndDate,
        stageName: update.stageName || questionnaireGroup.get('stageName')?.value,
      },
      { emitEvent: false }
    );

    const questionsControl = questionnaireGroup.get('questions');
    if (questionsControl) {
      questionsControl.setValue(update.questions || []);
    } else {
      questionnaireGroup.addControl('questions', this.fb.control(update.questions || []));
    }

    questionnaireGroup.markAsDirty();
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
          sequence: [q.sequence, [Validators.required, Validators.min(1)]],
          applicationStartDate: [q.applicationStartDate || ''],
          applicationEndDate: [q.applicationEndDate || ''],
          stageName: [q.stageName || q.name],
          questions: [q.questions || []],
        })
      )
    );
  }

  protected override save(): any {
    return {
      formValue: this.projectForm.getRawValue(),
      panelStates: this.panelStates,
      lastValidPanelIndex: this.lastValidPanelIndex,
    };
  }

  protected override restore(restoreParameter: RestoreParams<any>): void {
    if (restoreParameter.hasParams) {
      if (restoreParameter['formValue']) {
        const formValue = restoreParameter['formValue'];

        if (formValue.steps && formValue.steps.length > 0) {
          this.hasRestoredSteps = true;
        }

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

      if (typeof restoreParameter['lastValidPanelIndex'] === 'number') {
        this.lastValidPanelIndex = restoreParameter['lastValidPanelIndex'];
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

  private distributeEqualDurationDays(): void {
    const totalDays = this.calculateProjectDuration();
    if (totalDays <= 0) return;

    const stepsCount = this.stepsFormArray.length;
    if (stepsCount === 0) return;

    const daysPerStage = Math.floor(totalDays / stepsCount);
    const remainder = totalDays % stepsCount;

    this.stepsFormArray.controls.forEach((control, index) => {
      const extraDay = index < remainder ? 1 : 0;
      const assignedDays = daysPerStage + extraDay;
      control.get('durationDays')?.setValue(assignedDays);
    });
  }

  private getExistingStagesData(): { weight: number; durationDays: number; sequence: number }[] {
    return this.stepsFormArray.controls.map(control => ({
      weight: Number(control.get('weight')?.value) || 0,
      durationDays: Number(control.get('durationDays')?.value) || 0,
      sequence: Number(control.get('sequence')?.value) || 1
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
  }

  getStagesExceedDeadlineMessage(): string {
    const error = this.projectForm.errors?.['stagesExceedDeadline'];
    if (!error) return '';

    return error.message || '';
  }

  hasStagesExceedDeadlineError(): boolean {
    return !!this.projectForm.errors?.['stagesExceedDeadline'];
  }

  continueToNextPanel(currentPanelKey: keyof typeof this.panelStates): void {
    const currentIndex = this.PANEL_ORDER.indexOf(currentPanelKey as any);

    if (!this.isPanelValid(currentPanelKey)) {
      this.projectForm.markAllAsTouched();
      this.notificationService.showWarning('Por favor, preencha todos os campos obrigatórios antes de continuar.');
      return;
    }

    this.panelStates[currentPanelKey] = false;
    this.lastValidPanelIndex = Math.max(this.lastValidPanelIndex, currentIndex + 1);

    const nextPanelKey = this.PANEL_ORDER[currentIndex + 1];
    if (nextPanelKey) {
      this.loadPanelData(nextPanelKey);
      this.panelStates[nextPanelKey] = true;
    }

    this.cdr.detectChanges();
  }

  private loadPanelData(panelKey: keyof typeof this.panelStates): void {
    if (!this.selectedTemplateData) return;

    switch (panelKey) {
      case 'steps':
        if (this.selectedTemplateData.stages && this.selectedTemplateData.stages.length > 0 && !this.hasUserModifiedSteps()) {
          const stepsData = this.selectedTemplateData.stages.map((stage: any) => ({
            name: stage.name,
            weight: stage.weight,
            sequence: stage.sequence || 1,
            dateRange: '',
            durationDays: 0,
            applicationStartDate: '',
            applicationEndDate: ''
          }));
          this.projectForm.setControl('steps', this.buildCascataStepsForm(stepsData));

          this.distributeEqualDurationDays();
          this.recalculateAllStageRanges();
        }
        break;

      case 'representatives':
        if (this.selectedTemplateData.representatives && this.selectedTemplateData.representatives.length > 0) {
          const repsArray = this.projectForm.get('representatives') as FormArray;
          if (repsArray.length === 0) {
            const repsData = this.selectedTemplateData.representatives.map((rep: any) => ({
              firstName: capitalizeWords(rep.firstName),
              lastName: capitalizeWords(rep.lastName),
              email: rep.email,
              weight: rep.weight,
              roles: rep.roleNames || []
            }));
            this.projectForm.setControl('representatives', this.buildRepresentativesForm(repsData));
          }
        }
        break;

      case 'questionnaires':
        this.syncQuestionnairesWithSteps();
        break;
    }
  }

  private isPanelValid(panelKey: keyof typeof this.panelStates): boolean {
    switch (panelKey) {
      case 'project':
        const requiredControls = ['template', 'name', 'startDate', 'deadline'];
        const allRequiredValid = requiredControls.every(controlName => this.projectForm.get(controlName)?.valid);
        const groupErrors = this.projectForm.errors;
        const hasRelevantGroupError = groupErrors?.['dateRange'] || groupErrors?.['deadlineTooEarly'] || groupErrors?.['startDateTooLate'];
        return allRequiredValid && !hasRelevantGroupError;

      case 'steps':
        const stepsArray = this.projectForm.get('steps') as FormArray;
        return stepsArray && stepsArray.valid && stepsArray.length > 0 && !this.hasStagesExceedDeadlineError();

      case 'representatives':
        const repsArray = this.projectForm.get('representatives') as FormArray;
        return repsArray && repsArray.valid && repsArray.length > 0;

      case 'questionnaires':
        return true;

      default:
        return false;
    }
  }

  canOpenPanel(panelKey: keyof typeof this.panelStates): boolean {
    const panelIndex = this.PANEL_ORDER.indexOf(panelKey as any);
    return panelIndex <= this.lastValidPanelIndex;
  }

  onPanelToggled(panelKey: keyof typeof this.panelStates, newState: boolean): void {
    this.panelStates[panelKey] = newState;
    this.cdr.detectChanges();
  }

  onAttemptedToggle(panelKey: keyof typeof this.panelStates): void {
    this.notificationService.showWarning('Complete os painéis anteriores antes de acessar este.');
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }
  }
}
