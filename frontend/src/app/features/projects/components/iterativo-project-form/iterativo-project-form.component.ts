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
import {
  IterationPayload,
  ProjectCreationPayload,
  QuestionPayload,
  QuestionnairePayload,
  RepresentativePayload,
  StagePayload,
} from '../../../../shared/interfaces/project/project-creation.interface';
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
import { ProjectStore } from '../../../../shared/stores/project.store';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { GenericParams, RouteParams, RouterService } from '../../../../core/services/router.service';
import { StageIterativeModalComponent, StageIterativeData } from '../stage-iterative-modal/stage-iterative-modal.component';
import { RepresentativeModalComponent, RepresentativeData } from '../representative-modal/representative-modal.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { FormUtils } from '../../../../shared/utils/form-utils';
import { Subscription } from 'rxjs';
import { QuestionData } from '../question-modal/question-modal.component';
import { ProjectCreationConfirmModalComponent } from '../project-creation-confirm-modal/project-creation-confirm-modal.component';
import {
  ProjectTemplate,
  TemplateIterationDTO,
  TemplateQuestionDTO,
  TemplateQuestionnaireDTO,
  TemplateRepresentativeDTO,
  TemplateStageDTO,
} from '../../../../shared/interfaces/template/template.interface';
import { finalize, switchMap, take } from 'rxjs/operators';
import { RoleService } from '../../../../core/services/role.service';
import { RoleSummary } from '../../../../shared/interfaces/role/role-summary.interface';

export interface Representative {
  id?: number | string | null;
  firstName: string;
  lastName: string;
  email: string;
  weight: number;
  roleIds: number[];
  roleNames?: string[];
  roles?: RoleSummary[];
  userId?: number | null;
  projectId?: number | null;
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
  private projectStore = inject(ProjectStore);
  private modalService = inject(ModalService);
  private notificationService = inject(NotificationService);
  private roleService = inject(RoleService);
  public override routerService = inject(RouterService);

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public isSubmitting = false;
  public showQuestionnaireQuestionErrors = false;
  public panelStates: PanelStates = {
    project: true,
    stages: false,
    representatives: false,
    questionnaires: false,
  };
  private questionnaireQuestionErrors = new Set<number>();

  public templateOptions: SelectOption[] = [];
  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Iterativo, label: 'Iterativo Incremental' },
  ];

  public availableRoles: RoleSummary[] = [];
  private roleNameById = new Map<number, string>();

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
    this.loadRoles();
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

  private loadRoles(): void {
    this.roleService
      .getRoles()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (roles) => {
          this.availableRoles = roles ?? [];
          this.roleNameById = new Map(this.availableRoles.map((role) => [role.id, role.name]));
          this.syncRepresentativesRoleIdsFromNames();
          this.updateRepresentativeRoleDisplay();
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Erro ao carregar roles:', error);
        }
      });
  }

  private syncRepresentativesRoleIdsFromNames(): void {
    if (!this.availableRoles.length || this.representativesFormArray.length === 0) {
      return;
    }

    for (const control of this.representativesFormArray.controls) {
      const group = control as FormGroup;
      const roleIds = group.get('roleIds')?.value as number[] | undefined;
      if (roleIds?.length) {
        continue;
      }

      const roleNames = group.get('roleNames')?.value as string[] | undefined;
      if (roleNames?.length) {
        const mappedIds = this.mapRoleNamesToIds(roleNames);
        if (mappedIds.length) {
          group.patchValue({ roleIds: mappedIds }, { emitEvent: false });
        }
      }
    }
  }

  private updateRepresentativeRoleDisplay(targetGroup?: FormGroup): void {
    const applyDisplayNames = (group?: FormGroup | AbstractControl | null): void => {
      if (!(group instanceof FormGroup)) {
        return;
      }

      const roleIds = group.get('roleIds')?.value as number[] | undefined;
      const namesFromIds = this.getRoleNamesFromIds(roleIds);
      const storedNames = group.get('roleNames')?.value as string[] | undefined;
      const finalNames = namesFromIds.length ? namesFromIds : storedNames ?? [];
      group.patchValue({ roleNames: finalNames }, { emitEvent: false });
    };

    if (targetGroup) {
      applyDisplayNames(targetGroup);
      return;
    }

    for (const control of this.representativesFormArray.controls) {
      applyDisplayNames(control);
    }
  }

  private mapRoleNamesToIds(roleNames: string[] | undefined): number[] {
    if (!roleNames?.length || !this.availableRoles.length) {
      return [];
    }

    const normalized = roleNames
      .map((name) => name?.trim().toLowerCase())
      .filter(Boolean);

    if (!normalized.length) {
      return [];
    }

    const ids: number[] = [];
    for (const role of this.availableRoles) {
      if (normalized.includes(role.name.trim().toLowerCase())) {
        ids.push(role.id);
      }
    }

    return Array.from(new Set(ids));
  }

  private mapRepresentativeRoleIds(rep: Representative): number[] {
    if (Array.isArray(rep.roles) && rep.roles.length) {
      return rep.roles
        .map((role) => Number(role.id))
        .filter((id) => Number.isFinite(id) && id > 0)
    }

    if (rep.roleIds?.length) {
      return rep.roleIds.map(Number).filter(Number.isFinite);
    }

    if (rep.roleNames?.length) {
      return this.mapRoleNamesToIds(rep.roleNames);
    }

    return [];
  }

  private getRoleNamesFromIds(roleIds: number[] | undefined): string[] {
    if (!roleIds?.length || this.roleNameById.size === 0) {
      return [];
    }

    return roleIds
      .map((id) => this.roleNameById.get(Number(id)))
      .filter(Boolean) as string[];
  }

  private getRoleNamesFromFormGroup(repFormGroup: FormGroup): string[] {
    const ids = repFormGroup.get('roleIds')?.value as number[] | undefined;
    const mapped = this.getRoleNamesFromIds(ids);
    if (mapped.length) {
      return mapped;
    }

    const storedNames = repFormGroup.get('roleNames')?.value as string[] | undefined;
    return storedNames ?? [];
  }

  public getRepresentativeRoleNames(repControl: AbstractControl | null): string {
    if (!(repControl instanceof FormGroup)) {
      return '-';
    }

    const names = this.getRoleNamesFromFormGroup(repControl);
    return names.length ? names.join(', ') : '-';
  }

  private normalizeRoleIds(value: unknown): number[] {
    if (!Array.isArray(value)) {
      return [];
    }

    return Array.from(
      new Set(
        value
          .map(Number)
          .filter((id) => Number.isFinite(id) && id > 0)
      )
    );
  }

  private normalizeNullableNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
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
          id: [rep.id ?? null],
          userId: [rep.userId ?? null],
          projectId: [rep.projectId ?? null],
          firstName: [rep.firstName, [Validators.required]],
          lastName: [rep.lastName, [Validators.required]],
          email: [rep.email, [Validators.required, Validators.email]],
          weight: [rep.weight, [Validators.required, Validators.min(1)]],
          roleIds: [this.mapRepresentativeRoleIds(rep), [Validators.required, Validators.minLength(1)]],
          roleNames: [rep.roleNames ?? []],
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

  get questionnairesFormArray(): FormArray {
    return this.getControl('questionnaires') as FormArray;
  }

  get stagesFormArray(): FormArray {
    return this.getControl('stages') as FormArray;
  }

  get representativesFormArray(): FormArray {
    return this.getControl('representatives') as FormArray;
  }

  shouldDisplayQuestionnaireQuestionError(index: number): boolean {
    return this.showQuestionnaireQuestionErrors && this.questionnaireQuestionErrors.has(index);
  }

  getQuestionnaireQuestionErrorMessage(index: number): string {
    const control = this.questionnairesFormArray.at(index) as FormGroup | null;
    const name = (control?.get('name')?.value ?? '').toString().trim();
    if (name) {
      return `Adicione pelo menos uma pergunta ao questionário "${name}".`;
    }
    return 'Adicione pelo menos uma pergunta a este questionário.';
  }

  private validateQuestionnairesHaveQuestions(): boolean {
    this.questionnaireQuestionErrors.clear();

    this.questionnairesFormArray.controls.forEach((control, index) => {
      const questions = control.get('questions')?.value as QuestionData[] | undefined;
      if (!questions || questions.length === 0) {
        this.questionnaireQuestionErrors.add(index);
      }
    });

    const hasErrors = this.questionnaireQuestionErrors.size > 0;
    this.showQuestionnaireQuestionErrors = hasErrors;

    if (hasErrors) {
      this.notificationService.showWarning('Adicione pelo menos uma pergunta para cada questionário antes de salvar.');
    }

    this.cdr.markForCheck();
    return !hasErrors;
  }

  private handleQuestionnaireQuestionUpdate(index: number, questions: QuestionData[] | undefined): void {
    if (!this.showQuestionnaireQuestionErrors && this.questionnaireQuestionErrors.size === 0) {
      return;
    }

    if (questions?.length) {
      const removed = this.questionnaireQuestionErrors.delete(index);
      if (removed && this.questionnaireQuestionErrors.size === 0) {
        this.showQuestionnaireQuestionErrors = false;
      }
      if (removed) {
        this.cdr.markForCheck();
      }
      return;
    }

    if (this.showQuestionnaireQuestionErrors) {
      this.questionnaireQuestionErrors.add(index);
      this.cdr.markForCheck();
    }
  }

  addRepresentative(): void {
    this.modalService.open(RepresentativeModalComponent, 'medium-card', {
      mode: ActionType.CREATE
    });

    const modalRef = this.modalService.getActiveInstance<RepresentativeModalComponent>();
    if (!modalRef) {
      console.warn('Representative modal instance not available after opening.');
      return;
    }

    const createdSubscription = modalRef.representativeCreated.subscribe((newRepresentative: RepresentativeData) => {
      const newRep = this.fb.group({
        id: [newRepresentative.id ?? null],
        userId: [newRepresentative.userId ?? null],
        projectId: [newRepresentative.projectId ?? null],
        firstName: [newRepresentative.firstName, [Validators.required]],
        lastName: [newRepresentative.lastName, [Validators.required]],
        email: [newRepresentative.email, [Validators.required, Validators.email]],
        weight: [newRepresentative.weight, [Validators.required, Validators.min(1)]],
        roleIds: [newRepresentative.roleIds ?? [], [Validators.required, Validators.minLength(1)]],
        roleNames: [newRepresentative.roleNames ?? []],
      });
      this.representativesFormArray.push(newRep);
      this.updateRepresentativeRoleDisplay(newRep);
      createdSubscription.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  editRepresentative(index: number): void {
    const repFormGroup = this.representativesFormArray.at(index) as FormGroup;

    this.modalService.open(RepresentativeModalComponent, 'medium-card', {
      mode: ActionType.EDIT,
      editData: {
        id: repFormGroup.get('id')?.value ?? null,
        firstName: repFormGroup.get('firstName')?.value,
        lastName: repFormGroup.get('lastName')?.value,
        email: repFormGroup.get('email')?.value,
        weight: repFormGroup.get('weight')?.value,
        roleIds: repFormGroup.get('roleIds')?.value || [],
        roleNames: this.getRoleNamesFromFormGroup(repFormGroup),
        userId: repFormGroup.get('userId')?.value ?? null,
        projectId: repFormGroup.get('projectId')?.value ?? null,
      }
    });

    const modalRef = this.modalService.getActiveInstance<RepresentativeModalComponent>();
    if (!modalRef) {
      console.warn('Representative modal instance not available after opening for edit.');
      return;
    }

    const updatedSubscription = modalRef.representativeUpdated.subscribe((updatedRepresentative: RepresentativeData) => {
      repFormGroup.patchValue({
        id: updatedRepresentative.id ?? repFormGroup.get('id')?.value ?? null,
        firstName: updatedRepresentative.firstName,
        lastName: updatedRepresentative.lastName,
        email: updatedRepresentative.email,
        weight: updatedRepresentative.weight,
        roleIds: updatedRepresentative.roleIds ?? [],
        roleNames: updatedRepresentative.roleNames ?? [],
        userId: updatedRepresentative.userId ?? null,
        projectId: updatedRepresentative.projectId ?? null,
      });
      this.updateRepresentativeRoleDisplay(repFormGroup);
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
          questions: questions,
          stages: this.getAvailableStageNames(),
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

    return templateQuestionnaire.questions.map((question: TemplateQuestionDTO, index: number) => {
      const stageNames = this.getTemplateQuestionStageNames(question);
      return {
        id: `${Date.now()}-${index}`,
        value: question.value,
        roleIds: this.extractRoleIds(question.roles, question.roleNames),
        roleNames: this.extractRoleNames(question.roles, question.roleNames),
        stageNames,
        stageName: stageNames[0] ?? null,
        categoryStageName: stageNames[0] ?? null,
      } satisfies QuestionData;
    });
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

    const updatedQuestions = update.questions ?? [];
    const questionsControl = questionnaireGroup.get('questions');
    if (questionsControl) {
      questionsControl.setValue(updatedQuestions);
    } else {
      questionnaireGroup.addControl('questions', this.fb.control(updatedQuestions));
    }

    this.handleQuestionnaireQuestionUpdate(update.questionnaireIndex, updatedQuestions);

    questionnaireGroup.markAsDirty();
    this.cdr.markForCheck();
  }

  addStage(): void {
  this.openCreateStageModal();
  }

  private openCreateStageModal(): void {
    this.modalService.open(StageIterativeModalComponent, 'small-card');
    const modalRef = this.modalService.getActiveInstance<StageIterativeModalComponent>();
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

    const modalRef = this.modalService.getActiveInstance<StageIterativeModalComponent>();
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
      this.notificationService.showWarning('Revise os campos obrigatórios antes de salvar.');
      return;
    }

    if (!this.validateQuestionnairesHaveQuestions()) {
      return;
    }

    if (this.isSubmitting) {
      return;
    }

    this.openProjectCreationConfirmModal();
  }

  private openProjectCreationConfirmModal(): void {
    this.modalService.open(ProjectCreationConfirmModalComponent, 'small-card');
    const modalInstance = this.modalService.getActiveInstance<ProjectCreationConfirmModalComponent>();
    if (!modalInstance) {
      return;
    }

    modalInstance.confirmed.pipe(take(1)).subscribe(() => {
      this.modalService.close();
      this.createProjectWithTemplate();
    });

    modalInstance.canceled.pipe(take(1)).subscribe(() => {
      this.modalService.close();
    });
  }

  private createProjectWithTemplate(): void {
    let payload: ProjectCreationPayload;
    try {
      payload = this.buildProjectCreationPayload();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Erro ao preparar os dados do projeto.';
      this.notificationService.showError(message);
      return;
    }

    this.isSubmitting = true;
    this.cdr.markForCheck();

    this.projectStore
      .createProject(payload)
      .pipe(
        switchMap((project) => {
          if (!project?.id) {
            throw new Error('Não foi possível identificar o projeto criado.');
          }

          return this.templateStore.createTemplateFromProject(
            project.id,
            this.buildTemplateCloneRequest(payload.name)
          );
        }),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isSubmitting = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.notificationService.showSuccess('Projeto criado e template gerado com sucesso.');
          this.routerService.navigateTo('/projects');
        },
        error: (error) => {
          this.notificationService.showError(error);
        },
      });
  }

  private buildProjectCreationPayload(): ProjectCreationPayload {
    const formValue = this.projectForm.getRawValue() as IterativoProjectFormValue;
    const templateId = formValue.template;
    const startDate = formValue.startDate;
    const projectName = (formValue.name || '').trim();
    const iterationDuration = Number(formValue.iterationDuration);
    const iterationCount = Number(formValue.iterationCount ?? 0);

    if (!templateId) {
      throw new Error('Selecione um template antes de salvar o projeto.');
    }

    if (!startDate) {
      throw new Error('Informe a data de início do projeto.');
    }

    if (!projectName) {
      throw new Error('Informe o nome do projeto.');
    }

    if (!iterationDuration || iterationDuration <= 0) {
      throw new Error('Informe uma duração válida para as iterações.');
    }

    if (!iterationCount || iterationCount <= 0) {
      throw new Error('Não foi possível calcular a quantidade de iterações.');
    }

    const stages = this.buildStagePayload();
    const iterations = this.buildIterationPayload();
    const questionnaires = this.buildQuestionnairePayload();
    const representatives = this.buildRepresentativePayload();

    return {
      name: projectName,
      templateId,
      type: ProjectType.Iterativo,
      startDate,
      deadline: formValue.deadline || null,
      status: 'ABERTO',
      iterationDuration,
      iterationCount,
      stages: stages.length ? stages : undefined,
      iterations: iterations.length ? iterations : undefined,
      questionnaires: questionnaires.length ? questionnaires : undefined,
      representatives: representatives.length ? representatives : undefined,
    };
  }

  private buildStagePayload(): StagePayload[] {
    return this.stagesFormArray.controls
      .map((control, index) => ({
        name: control.get('name')?.value,
        weight: Number(control.get('weight')?.value) || 0,
        sequence: index + 1,
      }))
      .filter((stage) => Boolean(stage.name));
  }

  private getAvailableStageNames(): string[] {
    return Array.from(
      new Set(
        this.stagesFormArray.controls
          .map((control) => (control as FormGroup).get('name')?.value)
          .filter((name): name is string => typeof name === 'string' && name.trim().length > 0)
          .map((name) => name.trim())
      )
    );
  }

  private buildIterationPayload(): IterationPayload[] {
    const questionnaireWeights = this.getQuestionnaireWeightMap();

    return this.iterationsFormArray.controls
      .map((control, index) => {
        const iterationName = control.get('name')?.value || `Iteração ${index + 1}`;
        const weight = this.resolveIterationWeight(iterationName, questionnaireWeights);
        const applicationStartDate = control.get('applicationStartDate')?.value;
        const applicationEndDate = control.get('applicationEndDate')?.value;

        return {
          name: iterationName,
          weight,
          order: index + 1,
          applicationStartDate,
          applicationEndDate,
        } satisfies IterationPayload;
      })
      .filter((iteration) =>
        Boolean(iteration.applicationStartDate && iteration.applicationEndDate && Number.isFinite(iteration.weight))
      );
  }

  private getQuestionnaireWeightMap(): Map<string, number> {
    const map = new Map<string, number>();

    for (const control of this.questionnairesFormArray.controls) {
      const iterationKey = control.get('iteration')?.value || control.get('name')?.value;
      if (!iterationKey) {
        continue;
      }

      const weightValue = Number(control.get('weight')?.value);
      if (Number.isFinite(weightValue)) {
        map.set(iterationKey, weightValue);
      }
    }

    return map;
  }

  private resolveIterationWeight(iterationName: string | undefined, weightMap: Map<string, number>): number {
    if (iterationName && weightMap.has(iterationName)) {
      return weightMap.get(iterationName) as number;
    }

    const questionnaireControl = this.questionnairesFormArray.controls.find((control) => {
      const iterationRef = control.get('iteration')?.value || control.get('name')?.value;
      return iterationRef === iterationName;
    });

    const fallbackWeight = Number(questionnaireControl?.get('weight')?.value);
    if (Number.isFinite(fallbackWeight) && fallbackWeight > 0) {
      weightMap.set(iterationName ?? '', fallbackWeight);
      return fallbackWeight;
    }

    const iterationIndex = this.iterationsFormArray.controls.findIndex((control) => {
      const name = control.get('name')?.value;
      return name === iterationName;
    });

    if (iterationIndex >= 0) {
      const defaultWeight = iterationIndex + 1;
      weightMap.set(iterationName ?? '', defaultWeight);
      return defaultWeight;
    }

    return 1;
  }

  private buildQuestionnairePayload(): QuestionnairePayload[] {
    return this.questionnairesFormArray.controls
      .map((control, index) => {
        const questions = control.get('questions')?.value as QuestionData[] | undefined;
        const iterationName = control.get('iteration')?.value || control.get('name')?.value;
        const iterationControl = this.iterationsFormArray.controls.find(
          (iterControl) => (iterControl as FormGroup).get('name')?.value === iterationName
        ) as FormGroup | undefined;

        return {
          name: control.get('name')?.value,
          sequence: index + 1,
          iterationName: iterationName || null,
          weight: Number(control.get('weight')?.value) || 0,
          applicationStartDate: iterationControl?.get('applicationStartDate')?.value || null,
          applicationEndDate: iterationControl?.get('applicationEndDate')?.value || null,
          questions: this.mapQuestions(questions),
        };
      })
      .filter((questionnaire) => Boolean(questionnaire.name));
  }

  private buildRepresentativePayload(): RepresentativePayload[] {
    return this.representativesFormArray.controls
      .map((control) => {
        const roleIds = this.normalizeRoleIds(control.get('roleIds')?.value);
        return {
          ...(this.extractRepresentativeId(control.get('id'))),
          firstName: (control.get('firstName')?.value || '').trim(),
          lastName: (control.get('lastName')?.value || '').trim(),
          email: (control.get('email')?.value || '').trim(),
          userId: this.normalizeNullableNumber(control.get('userId')?.value),
          projectId: this.normalizeNullableNumber(control.get('projectId')?.value),
          weight: Number(control.get('weight')?.value) || 0,
          roleIds,
        } satisfies RepresentativePayload;
      })
      .filter((representative) => representative.roleIds.length > 0 && Boolean(representative.email));
  }

  private extractRepresentativeId(control: AbstractControl | null): { id?: number | string | null } {
    const rawValue = control?.value;

    if (rawValue === null || rawValue === undefined || rawValue === '') {
      return {};
    }

    return { id: rawValue };
  }

  private mapQuestions(questions: QuestionData[] | undefined): QuestionPayload[] {
    if (!questions?.length) {
      return [];
    }

    return questions.map((question) => {
      const roleIds = this.resolveQuestionRoleIds(question);
      const roleNames = this.resolveQuestionRoleNames(question, roleIds);
      const stageNames = this.resolveQuestionStageNames(question);
      return {
        value: question.value,
        roleIds,
        roleNames,
        stageNames: stageNames.length ? stageNames : undefined,
        stageName: stageNames[0] ?? null,
        categoryStageName: this.resolveCategoryStageName(question, stageNames),
      } satisfies QuestionPayload;
    });
  }

  private resolveQuestionRoleIds(question: QuestionData | undefined): number[] {
    if (!question) {
      return [];
    }

    const normalized = this.normalizeRoleIds(question.roleIds);
    if (normalized.length) {
      return normalized;
    }

    if (Array.isArray(question.roleNames) && question.roleNames.length) {
      return this.mapRoleNamesToIds(question.roleNames);
    }

    return [];
  }

  private resolveQuestionRoleNames(question: QuestionData | undefined, resolvedRoleIds: number[]): string[] {
    if (!question) {
      return [];
    }

    if (Array.isArray(question.roleNames) && question.roleNames.length) {
      return [...question.roleNames];
    }

    if (resolvedRoleIds.length) {
      return this.getRoleNamesFromIds(resolvedRoleIds);
    }

    return [];
  }

  private resolveQuestionStageNames(question: QuestionData | undefined): string[] {
    if (!question) {
      return [];
    }

    const normalized = Array.isArray(question.stageNames)
      ? question.stageNames
          .map((stage) => (typeof stage === 'string' ? stage.trim() : ''))
          .filter((stage) => stage.length > 0)
      : [];

    if (normalized.length) {
      return Array.from(new Set(normalized));
    }

    if (typeof question.stageName === 'string' && question.stageName.trim().length) {
      return [question.stageName.trim()];
    }

    return [];
  }

  private resolveCategoryStageName(question: QuestionData | undefined, stageNames: string[]): string | null {
    if (typeof question?.categoryStageName === 'string') {
      const normalized = question.categoryStageName.trim();
      if (normalized.length) {
        return normalized;
      }
    }

    if (stageNames.length === 1) {
      return stageNames[0];
    }

    if (typeof question?.stageName === 'string' && question.stageName.trim().length) {
      return question.stageName.trim();
    }

    return null;
  }

  private buildTemplateCloneRequest(projectName: string): {
    name: string;
    description: string;
    visibility: 'PUBLIC' | 'PRIVATE';
  } {
    const defaultDescription = `Template gerado automaticamente a partir do projeto ${projectName}.`;
    return {
      name: `${projectName} - Template`,
      description: this.selectedTemplateData?.description || defaultDescription,
      visibility: 'PRIVATE',
    };
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
            this.syncRepresentativesRoleIdsFromNames();
            this.updateRepresentativeRoleDisplay();
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

    for (let index = 0; index < iterationCount; index++) {
      const iterationEndDate = FormUtils.addBusinessDays(new Date(currentStartDate), Math.max(iterationDuration - 1, 0));
      const iterationName = this.getTemplateIterationName(index) ?? `Iteração ${index + 1}`;

      iterationsData.push({
        name: iterationName,
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
  const questionnairesArray = this.questionnairesFormArray;

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
      const defaultIterationName = this.getTemplateIterationName(index) ?? `Iteração ${index + 1}`;
      const iterationName = iterationControl.get('name')?.value || defaultIterationName;
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
      const initialQuestions = cached?.questions?.length
        ? this.cloneQuestions(cached.questions, iterationName)
        : this.getTemplateQuestionsForIteration(iterationName);

      questionnairesArray.push(
        this.fb.group({
          name: [iterationName, [Validators.required]],
          iteration: [iterationName, [Validators.required]],
          weight: [cached?.weight ?? 1, [Validators.required, Validators.min(0)]],
          dateRange: [cached?.dateRange || dateRange],
          questions: [initialQuestions],
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

  private mapTemplateStageToForm(stage: TemplateStageDTO): Stage {
    return {
      name: stage.name,
      weight: stage.weight,
    };
  }

  private mapTemplateRepresentativeToForm(rep: TemplateRepresentativeDTO): Representative {
    const roleIds = this.extractRoleIds(rep.roles, rep.roleNames);
    const roleNames = this.extractRoleNames(rep.roles, rep.roleNames);

    return {
      id: rep.id ?? null,
      firstName: capitalizeWords(rep.firstName),
      lastName: capitalizeWords(rep.lastName),
      email: rep.email,
      weight: rep.weight,
      roleIds,
      roleNames,
      userId: null,
      projectId: null,
    };
  }

  private getTemplateIterationName(index: number): string | null {
    if (!this.selectedTemplateData?.iterations?.length) {
      return null;
    }

    const templateIteration = this.selectedTemplateData.iterations[index] as TemplateIterationDTO | undefined;
    const trimmedName = templateIteration?.name?.trim();
    return trimmedName?.length ? trimmedName : null;
  }

  private getTemplateQuestionsForIteration(iterationName?: string | null): QuestionData[] {
    if (!iterationName || !this.selectedTemplateData?.questionnaires?.length) {
      return [];
    }

    const templateQuestionnaire = this.selectedTemplateData.questionnaires.find(
      (questionnaire: TemplateQuestionnaireDTO) =>
        questionnaire.iterationRefName === iterationName || questionnaire.name === iterationName
    );

    if (!templateQuestionnaire?.questions?.length) {
      return [];
    }

    return templateQuestionnaire.questions.map((question: TemplateQuestionDTO, index: number) => ({
      id: this.generateQuestionId(iterationName, index),
      value: question.value,
      roleIds: this.extractRoleIds(question.roles, question.roleNames),
      roleNames: this.extractRoleNames(question.roles, question.roleNames),
      stageNames: this.getTemplateQuestionStageNames(question),
      stageName: question.stageName ?? null,
      categoryStageName: question.stageName ?? null,
    }));
  }

  private cloneQuestions(questions: QuestionData[] | undefined, source?: string): QuestionData[] {
    if (!questions?.length) {
      return [];
    }

    return questions.map((question, index) => ({
      ...question,
      id: question.id ?? this.generateQuestionId(source, index),
      roleIds: Array.isArray(question.roleIds) ? [...question.roleIds] : [],
      roleNames: Array.isArray(question.roleNames) ? [...question.roleNames] : [],
      stageNames: Array.isArray(question.stageNames)
        ? [...question.stageNames]
        : question.stageName
          ? [question.stageName]
          : [],
      stageName: question.stageNames?.[0] ?? question.stageName ?? null,
      categoryStageName: question.categoryStageName ?? question.stageNames?.[0] ?? question.stageName ?? null,
    }));
  }

  private getTemplateQuestionStageNames(question: TemplateQuestionDTO): string[] {
    const stageName = question.stageName?.trim();
    return stageName?.length ? [stageName] : [];
  }

  private generateQuestionId(prefix: string | undefined, index: number): string {
    const randomPart = Math.random().toString(36).slice(2, 8);
    return `${prefix ?? 'question'}-${index}-${Date.now().toString(36)}-${randomPart}`;
  }

  private extractRoleIds(roles?: RoleSummary[], fallbackNames?: string[]): number[] {
    const idsFromRoles = Array.isArray(roles)
      ? roles
          .map((role) => Number(role.id))
          .filter((id) => Number.isFinite(id) && id > 0)
      : [];

    if (idsFromRoles.length) {
      return Array.from(new Set(idsFromRoles));
    }

    return this.mapRoleNamesToIds(fallbackNames ?? []);
  }

  private extractRoleNames(roles?: RoleSummary[], fallbackNames?: string[]): string[] {
    if (roles?.length) {
      return roles
        .map((role) => role.name?.trim())
        .filter((name): name is string => Boolean(name));
    }

    return Array.isArray(fallbackNames) ? [...fallbackNames] : [];
  }
}
