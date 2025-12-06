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
import { AccordionPanelComponent } from '../../../../shared/components/accordion-panel/accordion-panel.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';

import { CustomValidators } from '../../../../shared/validators/custom.validator';
import { capitalizeWords } from '../../../../core/utils/common-utils';
import {
  BasePageComponent,
  RestoreParams,
} from '../../../../core/abstractions/base-page.component';
import { TemplateStore } from '../../../../shared/stores/template.store';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { GenericParams, RouteParams, RouterService } from '../../../../core/services/router.service';
import { TemplateActionModalComponent } from '../template-action-modal/template-action-modal.component';
import { StageIterativeModalComponent, StageIterativeData } from '../stage-iterative-modal/stage-iterative-modal.component';
import { RepresentativeModalComponent, RepresentativeData } from '../representative-modal/representative-modal.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { FormUtils } from '../../../../shared/utils/form-utils';
import { Subscription } from 'rxjs';
import { QuestionData } from '../question-modal/question-modal.component';
import {
  ProjectTemplate,
  TemplateQuestionDTO,
  TemplateQuestionnaireDTO,
  TemplateRepresentativeDTO,
  TemplateStageDTO,
} from '../../../../shared/interfaces/template/template.interface';

export interface Representative {
  firstName: string;
  lastName: string;
  email: string;
  weight: number;
  roles: string[];
}

export interface Questionnaire {
  name: string;
  iteration: string;
  weight: number;
  dateRange: string;
  questions?: QuestionData[];
}

export interface Iteration {
  name: string;
  weight: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

export interface Stage {
  name: string;
  weight: number;
}

interface QuestionnaireUpdatePayload extends Questionnaire {
  questionnaireIndex: number;
  questions: QuestionData[];
}

type PanelKey = 'project' | 'stages' | 'representatives' | 'questionnaires';
type PanelStates = Record<PanelKey, boolean>;

interface IterativoProjectRouteParams extends GenericParams {
  questionnaireUpdate?: QuestionnaireUpdatePayload;
}

interface IterativoProjectFormValue {
  template: string | null;
  name: string;
  type: ProjectType;
  startDate: string | null;
  deadline: string | null;
  iterationDuration: number;
  iterationCount: number | null;
  iterations?: Iteration[];
  stages?: Stage[];
  representatives?: Representative[];
  questionnaires?: Questionnaire[];
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
export class IterativoProjectFormComponent extends BasePageComponent<IterativoProjectRouteParams> implements OnInit {
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  private templateStore = inject(TemplateStore);
  private modalService = inject(ModalService);
  private notificationService = inject(NotificationService);
  public override routerService = inject(RouterService);

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public panelStates: PanelStates = {
    project: true,
    stages: false,
    representatives: false,
    questionnaires: false,
  };

  public templateOptions: SelectOption[] = [];
  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Iterativo, label: 'Iterativo Incremental' },
  ];

  private selectedTemplateData: ProjectTemplate | null = null;
  private lastValidPanelIndex = 0;
  private readonly PANEL_ORDER: PanelKey[] = ['project', 'stages', 'representatives', 'questionnaires'];

  private suppressIterationValueChanges = false;
  private iterationsValueChangesSub?: Subscription;

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
        iterationDuration: [10, [Validators.required, Validators.min(1)]],
        iterationCount: [null],
        iterations: this.fb.array([]),
        stages: this.fb.array([]),
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
        ],
      }
    );

    this.loadTemplates();
    this.setupTemplateListener();
    this.setupIterationCountCalculation();
    this.setupIterationListener();
  }

  protected override loadParams(params: RouteParams<IterativoProjectRouteParams>): void {
    const questionnaireUpdate = params['questionnaireUpdate'] as QuestionnaireUpdatePayload | undefined;
    if (questionnaireUpdate) {
      this.applyQuestionnaireUpdate(questionnaireUpdate);
      delete params['questionnaireUpdate'];
    }
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

  private setupIterationCountCalculation(): void {
    const startDateControl = this.projectForm.get('startDate');
    const deadlineControl = this.projectForm.get('deadline');
    const iterationDurationControl = this.projectForm.get('iterationDuration');
    const iterationCountControl = this.projectForm.get('iterationCount');

    const calculateIterationCount = () => {
      const startDate = startDateControl?.value;
      const deadline = deadlineControl?.value;
      const iterationDuration = iterationDurationControl?.value;

      if (startDate && deadline && iterationDuration && iterationDuration > 0) {
        const start = new Date(startDate);
        const end = new Date(deadline);
        const businessDays = FormUtils.calculateBusinessDays(start, end);
        const count = Math.floor(businessDays / iterationDuration);
        iterationCountControl?.setValue(count > 0 ? count : 1, { emitEvent: false });
      } else {
        iterationCountControl?.setValue(null, { emitEvent: false });
      }
      this.cdr.markForCheck();
    };

    startDateControl?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(calculateIterationCount);

    deadlineControl?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(calculateIterationCount);

    iterationDurationControl?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(calculateIterationCount);
  }

  private setupIterationListener(): void {
    const startDateControl = this.projectForm.get('startDate');
    const deadlineControl = this.projectForm.get('deadline');
    const iterationDurationControl = this.projectForm.get('iterationDuration');

    const regenerateIterations = () => {
      const startDate = startDateControl?.value;
      const deadline = deadlineControl?.value;
      const iterationDuration = iterationDurationControl?.value;

      if (startDate && deadline && iterationDuration && iterationDuration > 0) {
        if (this.panelStates.stages) {
          this.generateIterations();
        }
      }
    };

    startDateControl?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(regenerateIterations);

    deadlineControl?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(regenerateIterations);

    iterationDurationControl?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(regenerateIterations);
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

          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Erro ao carregar template completo iterativo:', error);
        }
      });
  }

  private buildStagesForm(data?: Stage[]): FormArray {
    const stagesData = data || [];
    return this.fb.array(
      stagesData.map((stage) =>
        this.fb.group({
          name: [stage.name, [Validators.required]],
          weight: [stage.weight, [Validators.required, Validators.min(0)]],
        })
      )
    );
  }

  private buildIterationsForm(data?: Iteration[]): FormArray {
    const iterationsData = data || [];
    return this.fb.array(
      iterationsData.map((iteration, index) =>
        this.fb.group({
          name: [iteration.name || `Iteração ${index + 1}`, [Validators.required]],
          applicationStartDate: [iteration.applicationStartDate, [Validators.required]],
          applicationEndDate: [iteration.applicationEndDate, [Validators.required]],
          dateRange: [`${FormUtils.formatDateBR(iteration.applicationStartDate)} - ${FormUtils.formatDateBR(iteration.applicationEndDate)}`],
        })
      )
    );
  }

  private buildQuestionnairesForm(data?: Questionnaire[]): FormArray {
    const questionnaires = data || [];
    return this.fb.array(
      questionnaires.map((questionnaire) =>
        this.fb.group({
          name: [questionnaire.name, [Validators.required]],
          iteration: [questionnaire.iteration, [Validators.required]],
          weight: [questionnaire.weight, [Validators.required, Validators.min(0)]],
          dateRange: [questionnaire.dateRange],
          questions: [questionnaire.questions || []],
        })
      )
    );
  }

  private hasUserModifiedStages(): boolean {
    const stagesArray = this.projectForm.get('stages') as FormArray;
    return stagesArray && stagesArray.length > 0;
  }

  private buildRepresentativesForm(data?: Representative[]): FormArray {
    const representativesData = data || [];

    return this.fb.array(
      representativesData.map((rep) =>
        this.fb.group({
          firstName: [rep.firstName, [Validators.required]],
          lastName: [rep.lastName, [Validators.required]],
          email: [rep.email, [Validators.required, Validators.email]],
          weight: [rep.weight, [Validators.required, Validators.min(1)]],
          roles: [rep.roles, [Validators.required]],
        })
      )
    );
  }

  protected override save(): RouteParams<IterativoProjectRouteParams> {
    return {
      formValue: this.projectForm.getRawValue(),
      panelStates: this.panelStates,
      lastValidPanelIndex: this.lastValidPanelIndex,
    };
  }

  protected override restore(restoreParameter: RestoreParams<IterativoProjectRouteParams>): void {
    if (restoreParameter.hasParams) {
      const formValue = restoreParameter['formValue'] as IterativoProjectFormValue | undefined;
      this.applyRestoreFormValue(formValue);
      this.restorePanelMetadata(restoreParameter);
    }
    this.cdr.markForCheck();
  }

  getControl(name: string): AbstractControl | null {
    return this.projectForm.get(name);
  }

  get iterationsFormArray(): FormArray {
    return this.getControl('iterations') as FormArray;
  }

  get stagesFormArray(): FormArray {
    return this.getControl('stages') as FormArray;
  }

  get representativesFormArray(): FormArray {
    return this.getControl('representatives') as FormArray;
  }

  get questionnairesFormArray(): FormArray {
    return this.getControl('questionnaires') as FormArray;
  }

  addRepresentative(): void {
    this.modalService.open(RepresentativeModalComponent, 'medium-card', {
      mode: ActionType.CREATE
    });

    const modalRef = this.getModalInstance<RepresentativeModalComponent>();
    if (!modalRef) {
      return;
    }

    const createdSubscription = modalRef.representativeCreated.subscribe((newRepresentative: RepresentativeData) => {
      const newRep = this.fb.group({
        firstName: [newRepresentative.firstName, [Validators.required]],
        lastName: [newRepresentative.lastName, [Validators.required]],
        email: [newRepresentative.email, [Validators.required, Validators.email]],
        weight: [newRepresentative.weight, [Validators.required, Validators.min(1)]],
        roles: [newRepresentative.roles, [Validators.required]],
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

    const modalRef = this.getModalInstance<RepresentativeModalComponent>();
    if (!modalRef) {
      return;
    }

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
    const questionnaire = questionnaireGroup?.getRawValue() as Questionnaire | undefined;
    if (!questionnaire) {
      this.notificationService.showWarning('Não foi possível carregar o questionário selecionado.');
      return;
    }
    const projectName = this.projectForm.get('name')?.value || 'Novo Projeto';

    // Buscar as perguntas do template correspondente
  const questions = this.getQuestionsForQuestionnaire(questionnaire);

    // Navegar para a página de edição de questionário com os parâmetros
    this.routerService.navigateTo('/projects/questionnaire/iterativo', {
      params: {
        p: {
          projectName: projectName,
          questionnaireIndex: index,
          name: questionnaire.name,
          weight: questionnaire.weight,
          iteration: questionnaire.iteration,
          questions: questions
        }
      }
    });
  }

  private getQuestionsForQuestionnaire(questionnaire: Questionnaire | undefined): QuestionData[] {
    if (!questionnaire) {
      return [];
    }

    if (questionnaire.questions?.length) {
      return questionnaire.questions;
    }

    const templateQuestionnaire = this.selectedTemplateData?.questionnaires?.find(
      (tq: TemplateQuestionnaireDTO) =>
        tq.iterationRefName === questionnaire.iteration || tq.name === questionnaire.name
    );

    if (!templateQuestionnaire?.questions) {
      return [];
    }

    return templateQuestionnaire.questions.map((question: TemplateQuestionDTO, index: number) => ({
      id: `${Date.now()}-${index}`,
      value: question.value,
      roleNames: Array.from(question.roleNames || [])
    }));
  }

  private applyQuestionnaireUpdate(update: QuestionnaireUpdatePayload | undefined): void {
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
        weight: update.weight,
        iteration: update.iteration || questionnaireGroup.get('iteration')?.value,
        dateRange: update.dateRange || questionnaireGroup.get('dateRange')?.value,
      },
      { emitEvent: false }
    );

    const questionsControl = questionnaireGroup.get('questions');
    if (questionsControl) {
      questionsControl.setValue(update.questions ?? []);
    } else {
      questionnaireGroup.addControl('questions', this.fb.control(update.questions ?? []));
    }

    questionnaireGroup.markAsDirty();
    this.cdr.markForCheck();
  }

  addStage(): void {
    this.modalService.open(TemplateActionModalComponent, 'small-card');
    const modalRef = this.getModalInstance<TemplateActionModalComponent>();
    if (!modalRef) {
      return;
    }

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
    this.modalService.open(StageIterativeModalComponent, 'small-card');
    const modalRef = this.getModalInstance<StageIterativeModalComponent>();
    if (!modalRef) {
      return;
    }

    const createdSubscription = modalRef.stageCreated.subscribe((newStage: StageIterativeData) => {
      this.addNewStage(newStage);
      createdSubscription.unsubscribe();
    });
  }

  editStage(index: number): void {
    const stageFormGroup = this.stagesFormArray.at(index) as FormGroup;

    this.modalService.open(StageIterativeModalComponent, 'small-card', {
      mode: ActionType.EDIT,
      editData: {
        id: String(index),
        name: stageFormGroup.get('name')?.value,
        weight: stageFormGroup.get('weight')?.value
      }
    });

    const modalRef = this.getModalInstance<StageIterativeModalComponent>();
    if (!modalRef) {
      return;
    }

    const updatedSubscription = modalRef.stageUpdated.subscribe((updatedStage: StageIterativeData) => {
      this.updateStage(index, updatedStage);
      updatedSubscription.unsubscribe();
    });
  }

  private updateStage(index: number, updatedStage: StageIterativeData): void {
    const stagesArray = this.stagesFormArray;
    stagesArray.at(index).patchValue({
      name: updatedStage.name,
      weight: updatedStage.weight
    });

    this.cdr.detectChanges();
  }

  private addNewStage(newStage: StageIterativeData): void {
    this.stagesFormArray.push(
      this.fb.group({
        name: [newStage.name, Validators.required],
        weight: [newStage.weight, [Validators.required, Validators.min(0)]]
      })
    );

    this.cdr.detectChanges();
  }

  removeStage(index: number): void {
    if (this.stagesFormArray.length > 1) {
      this.stagesFormArray.removeAt(index);
      this.cdr.detectChanges();
    }
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
    }
  }

  continueToNextPanel(currentPanelKey: PanelKey): void {
    const currentIndex = this.PANEL_ORDER.indexOf(currentPanelKey);

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

  private isPanelValid(panelKey: PanelKey): boolean {
    switch (panelKey) {
      case 'project': {
        const requiredControls = ['template', 'name', 'startDate', 'deadline', 'iterationDuration'];
        const allRequiredValid = requiredControls.every(controlName => this.projectForm.get(controlName)?.valid);
        const groupErrors = this.projectForm.errors;
        const hasRelevantGroupError = groupErrors?.['dateRange'];
        const iterationCount = Number(this.projectForm.get('iterationCount')?.value) || 0;
        return allRequiredValid && !hasRelevantGroupError && iterationCount > 0;
      }

      case 'stages': {
        const stagesArray = this.projectForm.get('stages') as FormArray;
        return Boolean(stagesArray && stagesArray.valid && stagesArray.length > 0);
      }

      case 'questionnaires': {
        const questionnairesArray = this.projectForm.get('questionnaires') as FormArray;
        return Boolean(questionnairesArray && questionnairesArray.length > 0);
      }

      case 'representatives': {
        const repsArray = this.projectForm.get('representatives') as FormArray;
        return Boolean(repsArray && repsArray.valid && repsArray.length > 0);
      }

      default:
        return false;
    }
  }

  canOpenPanel(panelKey: PanelKey): boolean {
    const panelIndex = this.PANEL_ORDER.indexOf(panelKey);
    return panelIndex <= this.lastValidPanelIndex;
  }

  onPanelToggled(panelKey: PanelKey, newState: boolean): void {
    this.panelStates[panelKey] = newState;
    this.cdr.detectChanges();
  }

  onAttemptedToggle(panelKey: PanelKey): void {
    this.notificationService.showWarning(`Complete os painéis anteriores antes de acessar "${panelKey}".`);
  }

  private loadPanelData(panelKey: PanelKey): void {
    if (!this.selectedTemplateData) {
      return;
    }

    switch (panelKey) {
      case 'stages': {
        this.generateIterations();

        if (
          this.selectedTemplateData.stages?.length &&
          !this.hasUserModifiedStages()
        ) {
          const stagesData = this.selectedTemplateData.stages.map((stage: TemplateStageDTO) =>
            this.mapTemplateStageToForm(stage)
          );
          this.projectForm.setControl('stages', this.buildStagesForm(stagesData));
        }
        break;
      }

      case 'questionnaires': {
        this.generateQuestionnaires();
        break;
      }

      case 'representatives': {
        if (this.selectedTemplateData.representatives?.length) {
          const repsArray = this.projectForm.get('representatives') as FormArray;
          if (repsArray.length === 0) {
            const repsData = this.selectedTemplateData.representatives.map((rep: TemplateRepresentativeDTO) =>
              this.mapTemplateRepresentativeToForm(rep)
            );
            this.projectForm.setControl('representatives', this.buildRepresentativesForm(repsData));
          }
        }
        break;
      }
    }

    this.cdr.detectChanges();
  }

  private generateIterations(): void {
    const iterationCount = Number(this.projectForm.get('iterationCount')?.value) || 0;
    const iterationDuration = Number(this.projectForm.get('iterationDuration')?.value) || 0;
    const startDateValue = this.projectForm.get('startDate')?.value;

    if (iterationCount <= 0 || iterationDuration <= 0 || !startDateValue) {
      this.iterationsValueChangesSub?.unsubscribe();
      this.iterationsValueChangesSub = undefined;
  this.projectForm.setControl('iterations', this.buildIterationsForm([]));
  this.projectForm.setControl('questionnaires', this.buildQuestionnairesForm([]));
      this.cdr.markForCheck();
      return;
    }

    const iterationsData: Iteration[] = [];
    let currentStartDate = new Date(startDateValue);

    for (let i = 1; i <= iterationCount; i++) {
      const iterationEndDate = FormUtils.addBusinessDays(new Date(currentStartDate), Math.max(iterationDuration - 1, 0));
      iterationsData.push({
        name: `Iteração ${i}`,
        weight: 0,
        applicationStartDate: FormUtils.formatDateISO(currentStartDate),
        applicationEndDate: FormUtils.formatDateISO(iterationEndDate)
      });

      currentStartDate = FormUtils.addBusinessDays(new Date(iterationEndDate), 1);
    }

    this.suppressIterationValueChanges = true;
    this.projectForm.setControl('iterations', this.buildIterationsForm(iterationsData));
    this.setupIterationsValueChangeListener();
    this.suppressIterationValueChanges = false;

    this.generateQuestionnaires();
  }

  private setupIterationsValueChangeListener(): void {
    const iterationsArray = this.iterationsFormArray;

    this.iterationsValueChangesSub?.unsubscribe();
    this.iterationsValueChangesSub = iterationsArray.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.suppressIterationValueChanges) {
          return;
        }
        this.generateQuestionnaires();
      });
  }

  private generateQuestionnaires(): void {
    const iterationsArray = this.iterationsFormArray;
    const questionnairesArray = this.projectForm.get('questionnaires') as FormArray;

    const previousValues = questionnairesArray.getRawValue() as Questionnaire[];
    const cacheByIteration = new Map(
      (previousValues || []).map((item) => [item.iteration || item.name, item])
    );

    questionnairesArray.clear();

    if (iterationsArray.length === 0) {
      this.cdr.markForCheck();
      return;
    }

    for (const [index, control] of iterationsArray.controls.entries()) {
      const iterationControl = control as FormGroup;
      const iterationName = iterationControl.get('name')?.value || `Iteração ${index + 1}`;
      const iterationStartValue = iterationControl.get('applicationStartDate')?.value;
      const iterationEndValue = iterationControl.get('applicationEndDate')?.value;

      if (!iterationStartValue || !iterationEndValue) {
        continue;
      }

      const iterationStart = new Date(iterationStartValue);
      const iterationEnd = new Date(iterationEndValue);
      const iterationDurationDays = Math.max(FormUtils.calculateBusinessDays(iterationStart, iterationEnd), 1);

      const openingOffset = Math.max(Math.round(iterationDurationDays * 0.1), 0);
      const closingOffset = Math.max(Math.round(iterationDurationDays * 0.9), openingOffset);

      const questionnaireStartDate = FormUtils.addBusinessDays(new Date(iterationStart), openingOffset);
      const questionnaireEndDate = FormUtils.addBusinessDays(new Date(iterationStart), closingOffset);

      const dateRange = `${FormUtils.formatDateBR(FormUtils.formatDateISO(questionnaireStartDate))} - ${FormUtils.formatDateBR(FormUtils.formatDateISO(questionnaireEndDate))}`;
      const cached = cacheByIteration.get(iterationName);

      questionnairesArray.push(
        this.fb.group({
          name: [iterationName, [Validators.required]],
          iteration: [iterationName, [Validators.required]],
          weight: [cached?.weight ?? 1, [Validators.required, Validators.min(0)]],
          dateRange: [cached?.dateRange || dateRange],
          questions: [cached?.questions || []],
        })
      );
    }

    this.cdr.markForCheck();
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }

  importQuestionnaires(): void {
    this.notificationService.showWarning('Funcionalidade em desenvolvimento');
  }

  private applyRestoreFormValue(formValue?: IterativoProjectFormValue): void {
    if (!formValue) {
      return;
    }

    this.projectForm.setControl('iterations', this.buildIterationsForm(formValue.iterations || []));
    this.projectForm.setControl('stages', this.buildStagesForm(formValue.stages || []));
    this.projectForm.setControl('representatives', this.buildRepresentativesForm(formValue.representatives || []));
    this.projectForm.setControl('questionnaires', this.buildQuestionnairesForm(formValue.questionnaires || []));

    this.projectForm.patchValue(formValue);
  }

  private restorePanelMetadata(restoreParameter: RestoreParams<IterativoProjectRouteParams>): void {
    if (restoreParameter['panelStates']) {
      this.panelStates = restoreParameter['panelStates'] as PanelStates;
    }

    if (typeof restoreParameter['lastValidPanelIndex'] === 'number') {
      this.lastValidPanelIndex = restoreParameter['lastValidPanelIndex'];
    }
  }

  private getModalInstance<T>(): T | null {
    const modalAccessor = this.modalService as unknown as { modalRef?: { instance: T } };
    return modalAccessor.modalRef?.instance ?? null;
  }

  private mapTemplateStageToForm(stage: TemplateStageDTO): Stage {
    return {
      name: stage.name,
      weight: stage.weight,
    };
  }

  private mapTemplateRepresentativeToForm(rep: TemplateRepresentativeDTO): Representative {
    return {
      firstName: capitalizeWords(rep.firstName),
      lastName: capitalizeWords(rep.lastName),
      email: rep.email,
      weight: rep.weight,
      roles: rep.roleNames ?? [],
    };
  }
}
