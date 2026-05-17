import {
  Component,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  DestroyRef,
  OnInit,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  FormArray,
  Validators,
  AbstractControl,
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, take } from 'rxjs/operators';

import { LoggerService } from '../../../../core/services/logger.service';

import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { RouterService } from '../../../../core/services/router.service';
import { RoleService } from '../../../../core/services/role.service';
import { RoleSummary } from '../../../../shared/interfaces/role/role-summary.interface';
import { AccordionPanelComponent } from '../../../../shared/components/accordion-panel/accordion-panel.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';

import { CustomValidators } from '../../../../shared/validators/custom.validator';
import { FormUtils } from '../../../../shared/utils/form-utils';
import { BusinessDaysUtils } from '../../../../core/utils/business-days-utils';
import { ActionType } from '../../../../shared/enums/action-type.enum';

import {
  ProjectEditData,
  ProjectStageDetail,
  ProjectIterationDetail,
  ProjectQuestionnaireDetail,
  ProjectRepresentativeDetail,
  UpdateProjectRequest,
  UpdateStagePayload,
  UpdateIterationPayload,
  UpdateQuestionnairePayload,
  UpdateQuestionPayload,
  UpdateRepresentativePayload,
  UpdateProjectConflictError,
} from '../../../../shared/interfaces/project/project-update.interface';

import {
  StageIterativeModalComponent,
  StageIterativeData,
} from '../stage-iterative-modal/stage-iterative-modal.component';
import {
  RepresentativeModalComponent,
  RepresentativeData,
} from '../representative-modal/representative-modal.component';
import { QuestionData } from '../question-modal/question-modal.component';


type PanelKey = 'project' | 'stages' | 'representatives' | 'questionnaires';
type PanelStates = Record<PanelKey, boolean>;

@Component({
  selector: 'app-edit-iterativo-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AccordionPanelComponent,
    InputComponent,
    SelectComponent,
  ],
  templateUrl: './edit-iterativo-project-form.component.html',
  styleUrls: ['./edit-iterativo-project-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditIterativoProjectFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private projectStore = inject(ProjectStore);
  private modalService = inject(ModalService);
  private notificationService = inject(NotificationService);
  private routerService = inject(RouterService);
  private roleService = inject(RoleService);

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public isSubmitting = false;
  public isLoadingProject = true;
  public loadError: string | null = null;
  public showQuestionnaireQuestionErrors = false;

  public panelStates: PanelStates = {
    project: true,
    stages: true,
    representatives: true,
    questionnaires: true,
  };

  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Iterativo, label: 'Iterativo Incremental' },
  ];

  public availableRoles: RoleSummary[] = [];
  private roleNameById = new Map<number, string>();
  private questionnaireQuestionErrors = new Set<number>();

  private projectId!: string;
  private projectData: ProjectEditData | null = null;
  private static readonly FORM_STATE_CACHE_KEY = 'editIterativoFormStateCache';

  ngOnInit(): void {
    this.route.params.pipe(take(1)).subscribe((params) => {
      this.projectId = params['projectId'];
      if (!this.projectId) {
        this.routerService.navigateTo('/projects');
        return;
      }
      this.initForm();
      this.loadRoles();

      if (!this.restoreFormFromCache()) {
        this.loadProjectForEdit();
      }
    });
  }

  private initForm(): void {
    this.projectForm = this.fb.group(
      {
        name: ['', [Validators.required]],
        type: [{ value: ProjectType.Iterativo, disabled: true }, [Validators.required]],
        startDate: [null, [Validators.required]],
        deadline: [null],
        iterationDuration: [10, [Validators.required, Validators.min(1)]],
        iterationCount: [null],
        stages: this.fb.array([]),
        iterations: this.fb.array([]),
        representatives: this.fb.array([]),
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

    this.setupIterationCountCalculation();
    this.setupIterationRegeneration();
  }

  private setupIterationCountCalculation(): void {
    const startDateControl = this.projectForm.get('startDate');
    const deadlineControl = this.projectForm.get('deadline');
    const iterationDurationControl = this.projectForm.get('iterationDuration');
    const iterationCountControl = this.projectForm.get('iterationCount');

    const calculate = (): void => {
      const startDate = startDateControl?.value;
      const deadline = deadlineControl?.value;
      const iterationDuration = iterationDurationControl?.value;

      if (startDate && deadline && iterationDuration && iterationDuration > 0) {
        const start = BusinessDaysUtils.parseISODate(startDate);
        const end = BusinessDaysUtils.parseISODate(deadline);
        const businessDays = FormUtils.calculateBusinessDays(start, end);
        const count = Math.floor(businessDays / iterationDuration);
        iterationCountControl?.setValue(count > 0 ? count : 1, { emitEvent: false });
      } else {
        iterationCountControl?.setValue(null, { emitEvent: false });
      }
      this.cdr.markForCheck();
    };

    startDateControl?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(calculate);
    deadlineControl?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(calculate);
    iterationDurationControl?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(calculate);
  }

  private setupIterationRegeneration(): void {
    const regenerate = (): void => {
      const startDate = this.projectForm.get('startDate')?.value;
      const deadline = this.projectForm.get('deadline')?.value;
      const iterationDuration = this.projectForm.get('iterationDuration')?.value;

      if (startDate && deadline && iterationDuration && iterationDuration > 0) {
        this.regenerateIterationsAndQuestionnaires();
      }
    };

    this.projectForm.get('startDate')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(regenerate);
    this.projectForm.get('deadline')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(regenerate);
    this.projectForm.get('iterationDuration')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(regenerate);
  }

  private loadRoles(): void {
    this.roleService
      .getRoles(true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (roles) => {
          this.availableRoles = roles ?? [];
          this.roleNameById = new Map(this.availableRoles.map((r) => [r.id, r.name]));
          this.refreshQuestionnaireRoleNames();
          this.cdr.markForCheck();
        },
        error: (err) => console.error('Erro ao carregar roles:', err),
      });
  }

  private refreshQuestionnaireRoleNames(): void {
    if (!this.roleNameById.size) return;
    this.questionnairesFormArray.controls.forEach((control) => {
      const questionsCtrl = control.get('questions');
      const questions = questionsCtrl?.value as QuestionData[] | undefined;
      if (!questions?.length) return;
      const updated = questions.map((q) => ({
        ...q,
        roleNames: this.resolveRoleNames(q.roleIds, q.roleNames),
      }));
      questionsCtrl?.setValue(updated, { emitEvent: false });
    });
  }

  private resolveRoleNames(roleIds?: number[], fallbackNames?: string[]): string[] {
    const ids = Array.isArray(roleIds) ? roleIds : [];
    if (ids.length && this.roleNameById.size) {
      const names = ids
        .map((id) => this.roleNameById.get(Number(id)))
        .filter((name): name is string => Boolean(name));
      if (names.length) return Array.from(new Set(names));
    }
    return Array.isArray(fallbackNames) ? [...fallbackNames] : [];
  }

  private loadProjectForEdit(): void {
    this.isLoadingProject = true;
    this.loadError = null;
    this.cdr.markForCheck();

    this.projectStore
      .getProjectForEdit(this.projectId)
      .pipe(
        take(1),
        finalize(() => {
          this.isLoadingProject = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (data) => {
          this.projectData = data;
          this.populateFormFromProjectData(data);
        },
        error: (err) => {
          const message =
            err && typeof err === 'object' && 'message' in err
              ? (err as { message?: string }).message ?? 'Falha ao carregar dados do projeto.'
              : 'Falha ao carregar dados do projeto.';
          this.loadError = message;
          this.notificationService.showError(message);
        },
      });
  }

  retryLoad(): void {
    this.loadProjectForEdit();
  }

  private populateFormFromProjectData(data: ProjectEditData): void {
    this.projectForm.patchValue({
      name: data.name,
      type: data.type,
      startDate: data.startDate,
      deadline: data.deadline,
      iterationDuration: data.iterationDuration ?? 10,
      iterationCount: data.configuredIterationCount ?? data.iterations?.length ?? null,
    });

    const stagesArray = this.projectForm.get('stages') as FormArray;
    while (stagesArray.length > 0) stagesArray.removeAt(0);
    for (const stage of data.stages || []) {
      stagesArray.push(this.buildStageFormGroup(stage));
    }

    const iterationsArray = this.projectForm.get('iterations') as FormArray;
    while (iterationsArray.length > 0) iterationsArray.removeAt(0);
    for (const iteration of (data.iterations || []).sort((a, b) => (a.order ?? 0) - (b.order ?? 0))) {
      iterationsArray.push(this.buildIterationFormGroup(iteration));
    }

    const repsArray = this.projectForm.get('representatives') as FormArray;
    while (repsArray.length > 0) repsArray.removeAt(0);
    for (const rep of data.representatives || []) {
      repsArray.push(this.buildRepresentativeFormGroup(rep));
    }

    const qArray = this.projectForm.get('questionnaires') as FormArray;
    while (qArray.length > 0) qArray.removeAt(0);
    const sortedQuestionnaires = [...(data.questionnaires || [])].sort((a, b) => {
      if (a.sequence != null && b.sequence != null) return a.sequence - b.sequence;
      if (a.sequence != null) return -1;
      if (b.sequence != null) return 1;
      const dateA = a.applicationStartDate ?? '';
      const dateB = b.applicationStartDate ?? '';
      return dateA.localeCompare(dateB);
    });
    for (const q of sortedQuestionnaires) {
      qArray.push(this.buildQuestionnaireFormGroup(q));
    }

    this.applyPendingQuestionnaireUpdate();
    this.cdr.markForCheck();
  }

  private buildStageFormGroup(stage: ProjectStageDetail): FormGroup {
    return this.fb.group({
      _entityId: [stage.id],
      name: [stage.name, Validators.required],
      weight: [stage.weight, [Validators.required, Validators.min(0)]],
    });
  }

  private buildIterationFormGroup(iteration: ProjectIterationDetail): FormGroup {
    const start = iteration.applicationStartDate || '';
    const end = iteration.applicationEndDate || '';
    const dateRange = start && end
      ? `${FormUtils.formatDateBR(start)} - ${FormUtils.formatDateBR(end)}`
      : '';

    return this.fb.group({
      _entityId: [iteration.id],
      name: [iteration.name, Validators.required],
      weight: [iteration.weight ?? 0],
      order: [iteration.order ?? 1],
      applicationStartDate: [start, Validators.required],
      applicationEndDate: [end, Validators.required],
      dateRange: [dateRange],
    });
  }

  private buildRepresentativeFormGroup(rep: ProjectRepresentativeDetail): FormGroup {
    return this.fb.group({
      _entityId: [rep.id],
      firstName: [rep.firstName, Validators.required],
      lastName: [rep.lastName, Validators.required],
      email: [rep.email, [Validators.required, Validators.email]],
      weight: [rep.weight, [Validators.required, Validators.min(1)]],
      roleIds: [rep.roleIds ?? [], [Validators.required, Validators.minLength(1)]],
      roleNames: [rep.roleNames ?? []],
      userId: [rep.userId ?? null],
    });
  }

  private buildQuestionnaireFormGroup(q: ProjectQuestionnaireDetail): FormGroup {
    const questions: QuestionData[] = (q.questions || []).map((question) => ({
      id: String(question.id),
      _entityId: question.id,
      value: question.text,
      roleIds: question.roleIds ?? [],
      roleNames: question.roleNames ?? [],
      stageNames: question.stageNames ?? [],
      stageName: question.stageNames?.[0] ?? null,
      categoryStageName: question.stageNames?.[0] ?? null,
    }));

    const dateRange = q.applicationStartDate && q.applicationEndDate
      ? `${FormUtils.formatDateBR(q.applicationStartDate)} - ${FormUtils.formatDateBR(q.applicationEndDate)}`
      : '';

    return this.fb.group({
      _entityId: [q.id],
      name: [q.name, [Validators.required]],
      iterationName: [q.iterationName ?? q.name],
      weight: [q.weight, [Validators.required, Validators.min(0)]],
      dateRange: [dateRange],
      applicationStartDate: [q.applicationStartDate || ''],
      applicationEndDate: [q.applicationEndDate || ''],
      questions: [questions],
    });
  }

  getControl(name: string): AbstractControl | null {
    return this.projectForm.get(name);
  }

  get stagesFormArray(): FormArray {
    return this.projectForm.get('stages') as FormArray;
  }

  get iterationsFormArray(): FormArray {
    return this.projectForm.get('iterations') as FormArray;
  }

  get representativesFormArray(): FormArray {
    return this.projectForm.get('representatives') as FormArray;
  }

  get questionnairesFormArray(): FormArray {
    return this.projectForm.get('questionnaires') as FormArray;
  }

  public getRepresentativeRoleNames(repControl: AbstractControl | null): string {
    if (!(repControl instanceof FormGroup)) return '-';
    const roleIds = repControl.get('roleIds')?.value as number[] | undefined;
    const mapped = this.getRoleNamesFromIds(roleIds);
    if (mapped.length) return mapped.join(', ');
    const stored = repControl.get('roleNames')?.value as string[] | undefined;
    return stored?.length ? stored.join(', ') : '-';
  }

  private getRoleNamesFromIds(roleIds: number[] | undefined): string[] {
    if (!roleIds?.length || this.roleNameById.size === 0) return [];
    return roleIds
      .map((id) => this.roleNameById.get(Number(id)))
      .filter((name): name is string => Boolean(name));
  }

  addStage(): void {
    const existingNames = this.stagesFormArray.controls
      .map((c) => (c.get('name')?.value ?? '').toString().trim())
      .filter((n) => n.length > 0);

    this.modalService.open(StageIterativeModalComponent, 'small-card', {
      existingStageNames: existingNames,
    });

    const modalRef = this.modalService.getActiveInstance<StageIterativeModalComponent>();
    if (!modalRef) return;

    modalRef.stageCreated.pipe(take(1)).subscribe((newStage: StageIterativeData) => {
      this.stagesFormArray.push(
        this.fb.group({
          _entityId: [null],
          name: [newStage.name, Validators.required],
          weight: [newStage.weight, [Validators.required, Validators.min(0)]],
        })
      );
      this.cdr.detectChanges();
    });
  }

  editStage(index: number): void {
    const stageGroup = this.stagesFormArray.at(index) as FormGroup;
    const existingNames = this.stagesFormArray.controls
      .map((c, i) => ({ i, name: (c.get('name')?.value ?? '').toString().trim() }))
      .filter((item) => item.i !== index && item.name.length > 0)
      .map((item) => item.name);

    this.modalService.open(StageIterativeModalComponent, 'small-card', {
      mode: ActionType.EDIT,
      existingStageNames: existingNames,
      editData: {
        id: String(index),
        name: stageGroup.get('name')?.value,
        weight: stageGroup.get('weight')?.value,
      },
    });

    const modalRef = this.modalService.getActiveInstance<StageIterativeModalComponent>();
    if (!modalRef) return;

    modalRef.stageUpdated.pipe(take(1)).subscribe((updated: StageIterativeData) => {
      stageGroup.patchValue({
        name: updated.name,
        weight: updated.weight,
      });
      this.cdr.detectChanges();
    });
  }

  removeStage(index: number): void {
    if (this.stagesFormArray.length > 1) {
      this.stagesFormArray.removeAt(index);
      this.cdr.detectChanges();
    }
  }

  addRepresentative(): void {
    this.modalService.open(RepresentativeModalComponent, 'medium-card', {
      mode: ActionType.CREATE,
    });

    const modalRef = this.modalService.getActiveInstance<RepresentativeModalComponent>();
    if (!modalRef) return;

    modalRef.representativeCreated.pipe(take(1)).subscribe((newRep: RepresentativeData) => {
      const group = this.fb.group({
        _entityId: [null],
        firstName: [newRep.firstName, Validators.required],
        lastName: [newRep.lastName, Validators.required],
        email: [newRep.email, [Validators.required, Validators.email]],
        weight: [newRep.weight, [Validators.required, Validators.min(1)]],
        roleIds: [newRep.roleIds ?? [], [Validators.required, Validators.minLength(1)]],
        roleNames: [newRep.roleNames ?? []],
        userId: [null],
      });
      this.representativesFormArray.push(group);
      this.cdr.detectChanges();
    });
  }

  editRepresentative(index: number): void {
    const repGroup = this.representativesFormArray.at(index) as FormGroup;

    this.modalService.open(RepresentativeModalComponent, 'medium-card', {
      mode: ActionType.EDIT,
      editData: {
        id: repGroup.get('_entityId')?.value ?? null,
        firstName: repGroup.get('firstName')?.value,
        lastName: repGroup.get('lastName')?.value,
        email: repGroup.get('email')?.value,
        weight: repGroup.get('weight')?.value,
        roleIds: repGroup.get('roleIds')?.value || [],
        roleNames: this.getRoleNamesFromIds(repGroup.get('roleIds')?.value),
        userId: repGroup.get('userId')?.value ?? null,
      },
    });

    const modalRef = this.modalService.getActiveInstance<RepresentativeModalComponent>();
    if (!modalRef) return;

    modalRef.representativeUpdated.pipe(take(1)).subscribe((updated: RepresentativeData) => {
      repGroup.patchValue({
        firstName: updated.firstName,
        lastName: updated.lastName,
        email: updated.email,
        weight: updated.weight,
        roleIds: updated.roleIds ?? [],
        roleNames: updated.roleNames ?? [],
      });
      this.cdr.detectChanges();
    });
  }

  removeRepresentative(index: number): void {
    this.representativesFormArray.removeAt(index);
    this.cdr.detectChanges();
  }

  editQuestionnaire(index: number): void {
    const qGroup = this.questionnairesFormArray.at(index) as FormGroup | null;
    const questionnaire = qGroup?.getRawValue();
    if (!questionnaire) {
      this.notificationService.showWarning('Não foi possível carregar o questionário selecionado.');
      return;
    }

    const projectName = this.projectForm.get('name')?.value || 'Projeto';
    const questions = questionnaire.questions || [];
    const stages = this.getAvailableStageNames();

    this.saveFormStateToCache();

    this.routerService.navigateTo('/projects/questionnaire/iterativo', {
      params: {
        p: {
          projectName,
          questionnaireIndex: index,
          name: questionnaire.name,
          weight: questionnaire.weight,
          iteration: questionnaire.iterationName ?? questionnaire.name,
          questions,
          stages,
          returnToEdit: true,
          editProjectId: this.projectId,
        },
      },
    });
  }

  private getAvailableStageNames(): string[] {
    return Array.from(
      new Set(
        this.stagesFormArray.controls
          .map((c) => (c as FormGroup).get('name')?.value)
          .filter((n): n is string => typeof n === 'string' && n.trim().length > 0)
          .map((n) => n.trim())
      )
    );
  }

  private applyPendingQuestionnaireUpdate(): void {
    const PENDING_KEY = 'pendingQuestionnaireUpdate';
    try {
      const raw = sessionStorage.getItem(PENDING_KEY);
      if (!raw) return;
      sessionStorage.removeItem(PENDING_KEY);

      const update = JSON.parse(raw);
      if (!update || typeof update.questionnaireIndex !== 'number') return;

      const questionnaireGroup = this.questionnairesFormArray.at(update.questionnaireIndex) as FormGroup | null;
      if (!questionnaireGroup) return;

      questionnaireGroup.patchValue(
        {
          name: update.name,
          weight: update.weight,
          iterationName: update.iteration || questionnaireGroup.get('iterationName')?.value,
        },
        { emitEvent: false }
      );

      const updatedQuestions = update.questions || [];
      const questionsControl = questionnaireGroup.get('questions');
      if (questionsControl) {
        questionsControl.setValue(updatedQuestions);
      } else {
        questionnaireGroup.addControl('questions', this.fb.control(updatedQuestions));
      }

      questionnaireGroup.markAsDirty();
      this.cdr.detectChanges();
    } catch (error) {
      LoggerService.error('EditIterativoProjectForm: Erro ao aplicar atualização pendente do questionário', error);
    }
  }

  shouldDisplayQuestionnaireQuestionError(index: number): boolean {
    return this.showQuestionnaireQuestionErrors && this.questionnaireQuestionErrors.has(index);
  }

  getQuestionnaireQuestionErrorMessage(index: number): string {
    const control = this.questionnairesFormArray.at(index) as FormGroup | null;
    const name = (control?.get('name')?.value ?? '').toString().trim();
    return name
      ? `Adicione pelo menos uma pergunta ao questionário "${name}".`
      : 'Adicione pelo menos uma pergunta a este questionário.';
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
      this.notificationService.showWarning('Adicione pelo menos uma pergunta para cada questionário.');
    }
    this.cdr.markForCheck();
    return !hasErrors;
  }

  private regenerateIterationsAndQuestionnaires(): void {
    const iterationCount = Number(this.projectForm.get('iterationCount')?.value) || 0;
    const iterationDuration = Number(this.projectForm.get('iterationDuration')?.value) || 0;
    const startDateValue = this.projectForm.get('startDate')?.value;

    if (iterationCount <= 0 || iterationDuration <= 0 || !startDateValue) {
      return;
    }

    const existingQuestionnaires = this.questionnairesFormArray.getRawValue() || [];
    const qCacheByIteration = new Map<string, Record<string, unknown>>();
    for (const q of existingQuestionnaires) {
      const key = q['iterationName'] || q['name'];
      if (key) qCacheByIteration.set(key, q);
    }

    let lastQuestionsTemplate: unknown[] = [];
    for (const q of existingQuestionnaires) {
      const questions = q['questions'];
      if (Array.isArray(questions) && questions.length > 0) {
        lastQuestionsTemplate = questions;
      }
    }

    const existingIterations = this.iterationsFormArray.getRawValue() || [];
    const iterationIdByName = new Map<string, number | null>();
    for (const it of existingIterations) {
      if (it['name']) iterationIdByName.set(it['name'], it['_entityId'] ?? null);
    }

    const iterationsArray = this.projectForm.get('iterations') as FormArray;
    while (iterationsArray.length > 0) iterationsArray.removeAt(0);

    let currentStartDate = BusinessDaysUtils.parseISODate(startDateValue);

    for (let index = 0; index < iterationCount; index++) {
      const iterationEndDate = FormUtils.addBusinessDays(
        new Date(currentStartDate),
        Math.max(iterationDuration - 1, 0)
      );
      const iterationName = `Iteração ${index + 1}`;
      const startStr = FormUtils.formatDateISO(currentStartDate);
      const endStr = FormUtils.formatDateISO(iterationEndDate);
      const dateRange = `${FormUtils.formatDateBR(startStr)} - ${FormUtils.formatDateBR(endStr)}`;

      const existingId = iterationIdByName.get(iterationName) ?? null;

      iterationsArray.push(
        this.fb.group({
          _entityId: [existingId],
          name: [iterationName, Validators.required],
          weight: [0],
          order: [index + 1],
          applicationStartDate: [startStr, Validators.required],
          applicationEndDate: [endStr, Validators.required],
          dateRange: [dateRange],
        })
      );

      currentStartDate = FormUtils.addBusinessDays(new Date(iterationEndDate), 1);
    }

    const qArray = this.projectForm.get('questionnaires') as FormArray;
    while (qArray.length > 0) qArray.removeAt(0);

    for (let index = 0; index < iterationsArray.length; index++) {
      const itControl = iterationsArray.at(index) as FormGroup;
      const iterationName = itControl.get('name')?.value || `Iteração ${index + 1}`;
      const itStart = itControl.get('applicationStartDate')?.value || '';
      const itEnd = itControl.get('applicationEndDate')?.value || '';

      if (!itStart || !itEnd) continue;

      const itStartDate = BusinessDaysUtils.parseISODate(itStart);
      const itEndDate = BusinessDaysUtils.parseISODate(itEnd);
      const durationDays = Math.max(FormUtils.calculateBusinessDays(itStartDate, itEndDate), 1);

      const openingOffset = Math.max(Math.round(durationDays * 0.1), 0);
      const closingOffset = Math.max(Math.round(durationDays * 0.9), openingOffset);
      const qStartDate = FormUtils.addBusinessDays(new Date(itStartDate), openingOffset);
      const qEndDate = FormUtils.addBusinessDays(new Date(itStartDate), closingOffset);
      const qDateRange = `${FormUtils.formatDateBR(FormUtils.formatDateISO(qStartDate))} - ${FormUtils.formatDateBR(FormUtils.formatDateISO(qEndDate))}`;

      const cached = qCacheByIteration.get(iterationName);

      const fallbackQuestions = !cached && lastQuestionsTemplate.length > 0
        ? JSON.parse(JSON.stringify(lastQuestionsTemplate))
        : [];

      qArray.push(
        this.fb.group({
          _entityId: [cached?.['_entityId'] ?? null],
          name: [iterationName, [Validators.required]],
          iterationName: [iterationName],
          weight: [cached?.['weight'] ?? 1, [Validators.required, Validators.min(0)]],
          dateRange: [cached?.['dateRange'] || qDateRange],
          applicationStartDate: [FormUtils.formatDateISO(qStartDate)],
          applicationEndDate: [FormUtils.formatDateISO(qEndDate)],
          questions: [cached?.['questions'] ?? fallbackQuestions],
        })
      );
    }

    this.cdr.markForCheck();
  }

  canOpenPanel(): boolean {
    return true;
  }

  private saveFormStateToCache(): void {
    try {
      const state = {
        formValue: this.projectForm.getRawValue(),
        projectData: this.projectData,
      };
      sessionStorage.setItem(
        EditIterativoProjectFormComponent.FORM_STATE_CACHE_KEY,
        JSON.stringify(state)
      );
    } catch (e) {
      LoggerService.error('EditIterativoProjectForm: Erro ao salvar cache do formulário', e);
    }
  }

  private restoreFormFromCache(): boolean {
    const raw = sessionStorage.getItem(EditIterativoProjectFormComponent.FORM_STATE_CACHE_KEY);
    if (!raw) return false;
    sessionStorage.removeItem(EditIterativoProjectFormComponent.FORM_STATE_CACHE_KEY);

    try {
      const cached = JSON.parse(raw);
      if (!cached?.formValue) return false;

      this.projectData = cached.projectData || null;
      const fv = cached.formValue;

      this.projectForm.patchValue({
        name: fv.name,
        type: fv.type,
        startDate: fv.startDate,
        deadline: fv.deadline,
        iterationDuration: fv.iterationDuration,
        iterationCount: fv.iterationCount,
      }, { emitEvent: false });

      const stagesArray = this.projectForm.get('stages') as FormArray;
      while (stagesArray.length > 0) stagesArray.removeAt(0);
      for (const s of fv.stages || []) {
        stagesArray.push(this.fb.group({
          _entityId: [s._entityId ?? null],
          name: [s.name, Validators.required],
          weight: [s.weight, [Validators.required, Validators.min(0)]],
        }));
      }

      const iterationsArray = this.projectForm.get('iterations') as FormArray;
      while (iterationsArray.length > 0) iterationsArray.removeAt(0);
      for (const it of fv.iterations || []) {
        iterationsArray.push(this.fb.group({
          _entityId: [it._entityId ?? null],
          name: [it.name, Validators.required],
          weight: [it.weight ?? 0],
          order: [it.order ?? 1],
          applicationStartDate: [it.applicationStartDate, Validators.required],
          applicationEndDate: [it.applicationEndDate, Validators.required],
          dateRange: [it.dateRange || ''],
        }));
      }

      const repsArray = this.projectForm.get('representatives') as FormArray;
      while (repsArray.length > 0) repsArray.removeAt(0);
      for (const r of fv.representatives || []) {
        repsArray.push(this.fb.group({
          _entityId: [r._entityId ?? null],
          firstName: [r.firstName, Validators.required],
          lastName: [r.lastName, Validators.required],
          email: [r.email, [Validators.required, Validators.email]],
          weight: [r.weight, [Validators.required, Validators.min(1)]],
          roleIds: [r.roleIds ?? [], [Validators.required, Validators.minLength(1)]],
          roleNames: [r.roleNames ?? []],
          userId: [r.userId ?? null],
        }));
      }

      const qArray = this.projectForm.get('questionnaires') as FormArray;
      while (qArray.length > 0) qArray.removeAt(0);
      for (const q of fv.questionnaires || []) {
        qArray.push(this.fb.group({
          _entityId: [q._entityId ?? null],
          name: [q.name, [Validators.required]],
          iterationName: [q.iterationName ?? q.name],
          weight: [q.weight, [Validators.required, Validators.min(0)]],
          dateRange: [q.dateRange || ''],
          applicationStartDate: [q.applicationStartDate || ''],
          applicationEndDate: [q.applicationEndDate || ''],
          questions: [q.questions ?? []],
        }));
      }

      this.applyPendingQuestionnaireUpdate();
      this.isLoadingProject = false;
      this.cdr.markForCheck();
      return true;
    } catch (e) {
      LoggerService.error('EditIterativoProjectForm: Erro ao restaurar cache do formulário', e);
      return false;
    }
  }

  onPanelToggled(panelKey: PanelKey, newState: boolean): void {
    this.panelStates[panelKey] = newState;
    this.cdr.detectChanges();
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      this.notificationService.showWarning('Revise os campos obrigatórios antes de salvar.');
      return;
    }

    if (!this.validateQuestionnairesHaveQuestions()) return;
    if (this.isSubmitting) return;

    this.applyUpdate();
  }

  private applyUpdate(): void {
    let payload: UpdateProjectRequest;
    try {
      payload = this.buildUpdatePayload();
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Erro ao preparar os dados.';
      this.notificationService.showError(message);
      return;
    }

    this.isSubmitting = true;
    this.cdr.markForCheck();

    this.projectStore
      .updateProject(this.projectId, payload)
      .pipe(
        take(1),
        finalize(() => {
          this.isSubmitting = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (response) => {
          try {
            if (!response?.changesSummary) {
              this.notificationService.showSuccess('Projeto atualizado com sucesso.');
              this.routerService.navigateTo(`/projects/${this.projectId}`);
              return;
            }

            const summary = response.changesSummary;
            const totalChanges =
              summary.stagesAdded + summary.stagesUpdated + summary.stagesRemoved +
              summary.iterationsAdded + summary.iterationsUpdated + summary.iterationsRemoved +
              summary.questionnairesAdded + summary.questionnairesUpdated + summary.questionnairesRemoved +
              summary.questionsAdded + summary.questionsUpdated + summary.questionsRemoved +
              summary.representativesAdded + summary.representativesUpdated + summary.representativesRemoved;

            if (summary.blockedReasons?.length) {
              const reasons = summary.blockedReasons.join('\n• ');
              this.notificationService.showError(`Atualização bloqueada:\n• ${reasons}`);
              return;
            }

            if (summary.warnings?.length) {
              const warns = summary.warnings.join('\n• ');
              this.notificationService.showWarning(`Projeto atualizado com avisos:\n• ${warns}`);
            } else {
              this.notificationService.showSuccess(
                totalChanges > 0
                  ? `Projeto atualizado com sucesso. ${totalChanges} alteração(ões) aplicada(s).`
                  : 'Projeto atualizado com sucesso.'
              );
            }

            this.routerService.navigateTo(`/projects/${this.projectId}`);
          } catch (error_) {
            LoggerService.error('Erro inesperado ao processar resposta de atualização', error_);
            this.notificationService.showError('Erro inesperado ao processar a resposta da atualização.');
          }
        },
        error: (err) => {
          this.handleUpdateError(err);
        },
      });
  }

  private handleUpdateError(err: unknown): void {
    if (this.isConflictError(err)) {
      const conflictErr = err as { status: number; message: string };
      try {
        const parsed = JSON.parse(conflictErr.message) as UpdateProjectConflictError;
        if (parsed.errors?.length) {
          const errorList = parsed.errors.join('\n• ');
          this.notificationService.showError(`Operação bloqueada:\n• ${errorList}`);
          return;
        }
      } catch {
        // fallback
      }
      this.notificationService.showError(
        'Conflito: Algumas alterações não podem ser aplicadas. Verifique os dados e tente novamente.'
      );
      return;
    }

    this.notificationService.showError(err);
  }

  private isConflictError(err: unknown): boolean {
    return !!err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 409;
  }

  goBack(): void {
    this.routerService.navigateTo(`/projects/${this.projectId}`);
  }

  private buildUpdatePayload(): UpdateProjectRequest {
    const formValue = this.projectForm.getRawValue();
    const name = (formValue.name || '').trim();
    if (!name) throw new Error('Informe o nome do projeto.');
    if (!formValue.startDate) throw new Error('Informe a data de início do projeto.');

    const iterationDuration = Number(formValue.iterationDuration) || undefined;
    const iterationCount = Number(formValue.iterationCount) || undefined;

    return {
      name,
      startDate: formValue.startDate,
      deadline: formValue.deadline || null,
      iterationDuration,
      iterationCount,
      dryRun: false,
      stages: this.buildStagePayloads(),
      iterations: this.buildIterationPayloads(),
      questionnaires: this.buildQuestionnairePayloads(),
      representatives: this.buildRepresentativePayloads(),
    };
  }

  private buildStagePayloads(): UpdateStagePayload[] {
    return this.stagesFormArray.controls
      .map((control, index) => ({
        id: this.normalizeEntityId(control.get('_entityId')?.value),
        name: control.get('name')?.value,
        weight: Number(control.get('weight')?.value) || 0,
        sequence: index + 1,
      }))
      .filter((s) => Boolean(s.name));
  }

  private buildIterationPayloads(): UpdateIterationPayload[] {
    return this.iterationsFormArray.controls
      .map((control, index) => ({
        id: this.normalizeEntityId(control.get('_entityId')?.value),
        name: control.get('name')?.value || `Iteração ${index + 1}`,
        weight: Number(control.get('weight')?.value) || 0,
        order: Number(control.get('order')?.value) || index + 1,
        applicationStartDate: control.get('applicationStartDate')?.value || '',
        applicationEndDate: control.get('applicationEndDate')?.value || '',
      }))
      .filter((it) => Boolean(it.applicationStartDate && it.applicationEndDate));
  }

  private buildQuestionnairePayloads(): UpdateQuestionnairePayload[] {
    return this.questionnairesFormArray.controls
      .map((control, index) => {
        const questions = control.get('questions')?.value as QuestionData[] | undefined;
        return {
          id: this.normalizeEntityId(control.get('_entityId')?.value),
          name: control.get('name')?.value,
          weight: Number(control.get('weight')?.value) || 0,
          sequence: index + 1,
          iterationName: control.get('iterationName')?.value || null,
          applicationStartDate: control.get('applicationStartDate')?.value || null,
          applicationEndDate: control.get('applicationEndDate')?.value || null,
          questions: this.buildQuestionPayloads(questions),
        };
      })
      .filter((q) => Boolean(q.name));
  }

  private buildQuestionPayloads(questions: QuestionData[] | undefined): UpdateQuestionPayload[] {
    if (!questions?.length) return [];

    return questions.map((q) => ({
      id: this.normalizeEntityId((q as QuestionData & { _entityId?: number })._entityId ?? q.id),
      value: q.value,
      roleIds: this.normalizeRoleIds(q.roleIds).sort((a, b) => a - b),
      stageNames: q.stageNames?.length ? [...q.stageNames].sort((a, b) => a.localeCompare(b)) : [],
    }));
  }

  private buildRepresentativePayloads(): UpdateRepresentativePayload[] {
    return this.representativesFormArray.controls
      .map((control) => ({
        id: this.normalizeEntityId(control.get('_entityId')?.value),
        firstName: (control.get('firstName')?.value || '').trim(),
        lastName: (control.get('lastName')?.value || '').trim(),
        email: (control.get('email')?.value || '').trim(),
        weight: Number(control.get('weight')?.value) || 0,
        roleIds: this.normalizeRoleIds(control.get('roleIds')?.value),
      }))
      .filter((r) => r.roleIds.length > 0 && Boolean(r.email));
  }

  private normalizeEntityId(value: unknown): number | null {
    if (value === null || value === undefined || value === '') return null;
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private normalizeRoleIds(value: unknown): number[] {
    if (!Array.isArray(value)) return [];
    return Array.from(new Set(value.map(Number).filter((id) => Number.isFinite(id) && id > 0)));
  }
}
