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
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { InfoExplainerComponent } from '../../../../shared/components/info-explainer/info-explainer.component';

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
import { MultiSelectComponent, MultiSelectOption } from '../../../../shared/components/multi-select/multi-select.component';

import { CustomValidators } from '../../../../shared/validators/custom.validator';
import { ProjectDatesValidators } from '../../../../shared/validators/project-dates.validator';
import { StagesDeadlineValidator } from '../../../../shared/validators/stages-deadline.validator';
import { FormUtils } from '../../../../shared/utils/form-utils';
import { BusinessDaysUtils } from '../../../../core/utils/business-days-utils';
import { ActionType } from '../../../../shared/enums/action-type.enum';

import {
  ProjectEditData,
  ProjectStageDetail,
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
  StageCascataModalComponent,
  StageCascataData,
} from '../stage-cascata-modal/stage-cascata-modal.component';
import {
  RepresentativeModalComponent,
  RepresentativeData,
} from '../representative-modal/representative-modal.component';
import { QuestionData } from '../question-modal/question-modal.component';

type PanelKey = 'project' | 'steps' | 'representatives' | 'questionnaires';
type PanelStates = Record<PanelKey, boolean>;

@Component({
  selector: 'app-edit-cascata-project-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AccordionPanelComponent,
    InputComponent,
    SelectComponent,
    MultiSelectComponent,
    TranslateModule,
    InfoExplainerComponent,
  ],
  templateUrl: './edit-cascata-project-form.component.html',
  styleUrls: ['./edit-cascata-project-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditCascataProjectFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private projectStore = inject(ProjectStore);
  private modalService = inject(ModalService);
  private notificationService = inject(NotificationService);
  private routerService = inject(RouterService);
  private roleService = inject(RoleService);
  private readonly translate = inject(TranslateService);

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public isSubmitting = false;
  public isLoadingProject = true;
  public loadError: string | null = null;
  public showQuestionnaireQuestionErrors = false;

  public panelStates: PanelStates = {
    project: true,
    steps: true,
    representatives: true,
    questionnaires: true,
  };

  public projectTypeOptions: SelectOption[] = [];
  public aiUsageScopeOptions: MultiSelectOption[] = [];

  public availableRoles: RoleSummary[] = [];
  private roleNameById = new Map<number, string>();
  private questionnaireQuestionErrors = new Set<number>();

  private projectId!: string;
  private projectData: ProjectEditData | null = null;

  private readonly PANEL_ORDER: PanelKey[] = ['project', 'steps', 'representatives', 'questionnaires'];
  private static readonly FORM_STATE_CACHE_KEY = 'editCascataFormStateCache';

  ngOnInit(): void {
    this.projectTypeOptions = [
      { value: ProjectType.Cascata, label: this.translate.instant('projects.type.cascata') },
    ];
    this.aiUsageScopeOptions = [
      { value: 'NAO_UTILIZA', label: this.translate.instant('project.ai_usage.options.NAO_UTILIZA') },
      { value: 'REQUISITOS', label: this.translate.instant('project.ai_usage.options.REQUISITOS') },
      { value: 'DESIGN', label: this.translate.instant('project.ai_usage.options.DESIGN') },
      { value: 'CODIFICACAO', label: this.translate.instant('project.ai_usage.options.CODIFICACAO') },
      { value: 'TESTES', label: this.translate.instant('project.ai_usage.options.TESTES') },
      { value: 'DOCUMENTACAO', label: this.translate.instant('project.ai_usage.options.DOCUMENTACAO') },
      { value: 'AI_GOVERNANCE', label: this.translate.instant('project.ai_usage.options.AI_GOVERNANCE') },
    ];
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
        type: [{ value: ProjectType.Cascata, disabled: true }, [Validators.required]],
        startDate: [null, [Validators.required]],
        deadline: [null, [Validators.required, CustomValidators.minDateToday()]],
        aiUsageScopes: [[]],
        steps: this.fb.array([]),
        representatives: this.fb.array([]),
        questionnaires: this.fb.array([]),
      },
      {
        validators: [
          CustomValidators.dateRange('startDate', 'deadline'),
          ProjectDatesValidators.stageApplicationRangeWithinDeadline(),
          StagesDeadlineValidator.stagesFitWithinDeadline(),
        ],
      }
    );

    this.setupDateListeners();
  }

  private setupDateListeners(): void {
    this.projectForm.get('startDate')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.stepsFormArray.length > 0) {
          this.recalculateAllStageRanges();
        }
      });

    this.projectForm.get('deadline')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.stepsFormArray.length > 0) {
          this.recalculateAllStageRanges();
        }
        this.projectForm.get('startDate')?.updateValueAndValidity({ emitEvent: false });
        for (const stepControl of this.stepsFormArray.controls) {
          stepControl.updateValueAndValidity({ emitEvent: false });
        }
      });
  }

  private loadRoles(): void {
    this.roleService
      .getRoles(true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (roles) => {
          this.availableRoles = roles ?? [];
          this.roleNameById = new Map(this.availableRoles.map((r) => [r.id, r.name]));
          this.cdr.markForCheck();
        },
        error: (err) => console.error('Erro ao carregar roles:', err),
      });
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
              ? (err as { message?: string }).message ?? this.translate.instant('projects.form.error_loading')
              : this.translate.instant('projects.form.error_loading');
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
      aiUsageScopes: data.aiUsageScopes ?? [],
    });

    const stepsArray = this.projectForm.get('steps') as FormArray;
    while (stepsArray.length > 0) stepsArray.removeAt(0);
    const sortedStages = (data.stages || []).sort((a, b) => a.sequence - b.sequence);
    const stagesWithDuration = this.ensureStageDurationDays(sortedStages, data.startDate);
    for (const stage of stagesWithDuration) {
      stepsArray.push(this.buildStageFormGroup(stage));
    }

    const repsArray = this.projectForm.get('representatives') as FormArray;
    while (repsArray.length > 0) repsArray.removeAt(0);
    for (const rep of data.representatives || []) {
      repsArray.push(this.buildRepresentativeFormGroup(rep));
    }

    const qArray = this.projectForm.get('questionnaires') as FormArray;
    while (qArray.length > 0) qArray.removeAt(0);
    for (const q of (data.questionnaires || []).sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0))) {
      qArray.push(this.buildQuestionnaireFormGroup(q));
    }

    this.applyPendingQuestionnaireUpdate();
    this.cdr.markForCheck();
  }

  private buildStageFormGroup(stage: ProjectStageDetail): FormGroup {
    const startStr = stage.applicationStartDate || '';
    const endStr = stage.applicationEndDate || '';
    const dateRange = startStr && endStr
      ? `${FormUtils.formatDateBR(startStr)} - ${FormUtils.formatDateBR(endStr)}`
      : '';

    return this.fb.group({
      _entityId: [stage.id],
      name: [stage.name, Validators.required],
      weight: [stage.weight, [Validators.required, Validators.min(0)]],
      sequence: [stage.sequence || 1, [Validators.required, Validators.min(1)]],
      dateRange: [dateRange],
      durationDays: [stage.durationDays || 0],
      applicationStartDate: [startStr],
      applicationEndDate: [endStr],
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
      stageName: question.stageNames?.[0] ?? q.stageName ?? null,
      categoryStageName: question.stageNames?.[0] ?? q.stageName ?? null,
    }));

    return this.fb.group({
      _entityId: [q.id],
      name: [q.name, [Validators.required]],
      sequence: [q.sequence ?? 1],
      stageName: [q.stageName ?? q.name],
      iterationName: [q.iterationName ?? null],
      domain: [q.domain ?? null],
      description: [q.description ?? null],
      applicationStartDate: [q.applicationStartDate || ''],
      applicationEndDate: [q.applicationEndDate || ''],
      weight: [q.weight],
      questions: [questions],
    });
  }

  getControl(name: string): AbstractControl | null {
    return this.projectForm.get(name);
  }

  get stepsFormArray(): FormArray {
    return this.projectForm.get('steps') as FormArray;
  }

  get representativesFormArray(): FormArray {
    return this.projectForm.get('representatives') as FormArray;
  }

  get questionnairesFormArray(): FormArray {
    return this.projectForm.get('questionnaires') as FormArray;
  }

  trackByIndex(index: number): number {
    return index;
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

  addStep(): void {
    const projectStart = this.projectForm.get('startDate')?.value;
    const projectDeadline = this.projectForm.get('deadline')?.value;
    const existingStages = this.getExistingStagesForModal();
    const existingSequences = this.getExistingSequences();

    this.modalService.open(StageCascataModalComponent, 'small-card', {
      projectStartDate: projectStart,
      projectDeadline: projectDeadline,
      projectDurationDays: this.calculateProjectDuration(),
      existingStages: existingStages,
      existingSequences,
    });

    const modalRef = this.modalService.getActiveInstance<StageCascataModalComponent>();
    if (!modalRef) return;

    modalRef.stageCreated.pipe(take(1)).subscribe((newStage: StageCascataData) => {
      this.handleSequenceOnCreate(newStage.sequence);
      this.stepsFormArray.push(
        this.fb.group({
          _entityId: [null],
          name: [newStage.name, Validators.required],
          weight: [newStage.weight, [Validators.required, Validators.min(0)]],
          sequence: [newStage.sequence, [Validators.required, Validators.min(1)]],
          dateRange: [`${FormUtils.formatDateBR(newStage.applicationStartDate)} - ${FormUtils.formatDateBR(newStage.applicationEndDate)}`],
          durationDays: [newStage.durationDays],
          applicationStartDate: [newStage.applicationStartDate],
          applicationEndDate: [newStage.applicationEndDate],
        })
      );
      this.recalculateAllStageRanges();
      this.syncQuestionnairesWithSteps();
      this.cdr.detectChanges();
    });
  }

  editStep(index: number): void {
    const stepFormGroup = this.stepsFormArray.at(index) as FormGroup;
    const projectStart = this.projectForm.get('startDate')?.value;
    const projectDeadline = this.projectForm.get('deadline')?.value;

    this.modalService.open(StageCascataModalComponent, 'medium-card', {
      mode: ActionType.EDIT,
      editData: {
        id: String(index),
        name: stepFormGroup.get('name')?.value,
        weight: stepFormGroup.get('weight')?.value,
        sequence: stepFormGroup.get('sequence')?.value,
        durationDays: stepFormGroup.get('durationDays')?.value,
        applicationStartDate: stepFormGroup.get('applicationStartDate')?.value,
        applicationEndDate: stepFormGroup.get('applicationEndDate')?.value,
      },
      projectStartDate: projectStart,
      projectDeadline: projectDeadline,
      projectDurationDays: this.calculateProjectDuration(),
      existingStages: this.getExistingStagesForModal(),
      currentStageIndex: index,
      existingSequences: this.getExistingSequencesExcluding(index),
    });

    const modalRef = this.modalService.getActiveInstance<StageCascataModalComponent>();
    if (!modalRef) return;

    modalRef.stageUpdated.pipe(take(1)).subscribe((updated: StageCascataData) => {
      this.handleSequenceOnEdit(index, updated.sequence);
      (this.stepsFormArray.at(index) as FormGroup).patchValue({
        name: updated.name,
        weight: updated.weight,
        sequence: updated.sequence,
        durationDays: updated.durationDays,
        dateRange: `${FormUtils.formatDateBR(updated.applicationStartDate)} - ${FormUtils.formatDateBR(updated.applicationEndDate)}`,
        applicationStartDate: updated.applicationStartDate,
        applicationEndDate: updated.applicationEndDate,
      });
      this.recalculateAllStageRanges();
      this.syncQuestionnairesWithSteps();
      this.cdr.detectChanges();
    });
  }

  removeStep(index: number): void {
    if (this.stepsFormArray.length > 1) {
      this.stepsFormArray.removeAt(index);
      this.recalculateAllStageRanges();
      this.syncQuestionnairesWithSteps();
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
      this.notificationService.showWarning(this.translate.instant('notifications.project_form.load_questionnaire_error'));
      return;
    }

    const projectName = this.projectForm.get('name')?.value || this.translate.instant('common.project');
    const questions = questionnaire.questions || [];

    this.saveFormStateToCache();

    this.routerService.navigateTo('/projects/questionnaire/cascata', {
      params: {
        p: {
          projectName,
          questionnaireIndex: index,
          sequence: questionnaire.sequence,
          name: questionnaire.name,
          applicationStartDate: questionnaire.applicationStartDate,
          applicationEndDate: questionnaire.applicationEndDate,
          stageName: questionnaire.stageName,
          questions,
          returnToEdit: true,
          editProjectId: this.projectId,
        },
      },
    });
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
          sequence: update.sequence,
          applicationStartDate: update.applicationStartDate,
          applicationEndDate: update.applicationEndDate,
          stageName: update.stageName || questionnaireGroup.get('stageName')?.value,
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
      LoggerService.error('EditCascataProjectForm: Erro ao aplicar atualização pendente do questionário', error);
    }
  }

  shouldDisplayQuestionnaireQuestionError(index: number): boolean {
    return this.showQuestionnaireQuestionErrors && this.questionnaireQuestionErrors.has(index);
  }

  getQuestionnaireQuestionErrorMessage(index: number): string {
    const control = this.questionnairesFormArray.at(index) as FormGroup | null;
    const name = (control?.get('name')?.value ?? '').toString().trim();
    return name
      ? this.translate.instant('projects.form.validation.add_question_to_questionnaire', { name })
      : this.translate.instant('projects.form.validation.add_question_to_this');
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
      this.notificationService.showWarning(this.translate.instant('notifications.project_form.add_questions'));
    }
    this.cdr.markForCheck();
    return !hasErrors;
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
        EditCascataProjectFormComponent.FORM_STATE_CACHE_KEY,
        JSON.stringify(state)
      );
    } catch (e) {
      LoggerService.error('EditCascataProjectForm: Erro ao salvar cache do formulário', e);
    }
  }

  private restoreFormFromCache(): boolean {
    const raw = sessionStorage.getItem(EditCascataProjectFormComponent.FORM_STATE_CACHE_KEY);
    if (!raw) return false;
    sessionStorage.removeItem(EditCascataProjectFormComponent.FORM_STATE_CACHE_KEY);

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
        aiUsageScopes: fv.aiUsageScopes ?? [],
      }, { emitEvent: false });

      const stepsArray = this.projectForm.get('steps') as FormArray;
      while (stepsArray.length > 0) stepsArray.removeAt(0);
      for (const s of fv.steps || []) {
        stepsArray.push(this.fb.group({
          _entityId: [s._entityId ?? null],
          name: [s.name, Validators.required],
          weight: [s.weight, [Validators.required, Validators.min(0)]],
          sequence: [s.sequence, [Validators.required, Validators.min(1)]],
          dateRange: [s.dateRange || ''],
          durationDays: [s.durationDays],
          applicationStartDate: [s.applicationStartDate],
          applicationEndDate: [s.applicationEndDate],
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
          sequence: [q.sequence],
          stageName: [q.stageName],
          iterationName: [q.iterationName ?? null],
          domain: [q.domain ?? null],
          description: [q.description ?? null],
          applicationStartDate: [q.applicationStartDate],
          applicationEndDate: [q.applicationEndDate],
          weight: [q.weight ?? 0],
          questions: [q.questions ?? []],
        }));
      }

      this.applyPendingQuestionnaireUpdate();
      this.isLoadingProject = false;
      this.cdr.markForCheck();
      return true;
    } catch (e) {
      LoggerService.error('EditCascataProjectForm: Erro ao restaurar cache do formulário', e);
      return false;
    }
  }

  onPanelToggled(panelKey: PanelKey, newState: boolean): void {
    this.panelStates[panelKey] = newState;
    this.cdr.detectChanges();
  }

  onAttemptedToggle(): void {  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      const issues = this.collectValidationIssues();
      this.notificationService.showWarning(issues);
      return;
    }

    if (!this.validateQuestionnairesHaveQuestions()) return;
    if (this.isSubmitting) return;

    this.applyUpdate();
  }

  private collectValidationIssues(): string {
    const issues: string[] = [];

    const fieldLabels: Record<string, string> = {
      name: this.translate.instant('projects.form.validation.field_project_name'),
      startDate: this.translate.instant('projects.form.validation.field_start_date'),
      deadline: this.translate.instant('projects.form.validation.field_deadline'),
    };

    for (const [key, label] of Object.entries(fieldLabels)) {
      if (this.projectForm.get(key)?.invalid) {
        issues.push(label);
      }
    }

    const groupErrors = this.projectForm.errors;
    if (groupErrors?.['dateRange']) {
      issues.push(this.translate.instant('projects.form.validation.date_range_error'));
    }
    if (groupErrors?.['stagesExceedDeadline']) {
      issues.push(this.translate.instant('projects.form.validation.stages_exceed_deadline'));
    }
    if (groupErrors?.['stageApplicationRangeExceedsDeadline']) {
      issues.push(this.translate.instant('projects.form.validation.stage_application_range'));
    }

    const stepsArray = this.projectForm.get('steps') as FormArray;
    if (!stepsArray || stepsArray.length === 0) {
      issues.push(this.translate.instant('projects.form.validation.add_at_least_one_step'));
    } else if (stepsArray.invalid) {
      issues.push(this.translate.instant('projects.form.validation.invalid_steps'));
    }

    const repsArray = this.projectForm.get('representatives') as FormArray;
    if (!repsArray || repsArray.length === 0) {
      issues.push(this.translate.instant('projects.form.validation.add_at_least_one_rep'));
    } else if (repsArray.invalid) {
      issues.push(this.translate.instant('projects.form.validation.invalid_reps'));
    }

    if (issues.length === 0) {
      return this.translate.instant('projects.form.validation.review_required');
    }

    return `${this.translate.instant('projects.form.validation.cannot_save_prefix')}<br>• ${issues.join('<br>• ')}`;
  }

  private applyUpdate(): void {
    let payload: UpdateProjectRequest;
    try {
      payload = this.buildUpdatePayload();
    } catch (error) {
      LoggerService.error('EditCascataProjectForm: Erro ao construir payload de atualização.', error);
      const message = error instanceof Error ? error.message : this.translate.instant('projects.form.validation.prepare_data_error');
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
              this.notificationService.showSuccess(this.translate.instant('projects.messages.updated'));
              this.routerService.navigateTo(`/projects/${this.projectId}`);
              return;
            }

            const summary = response.changesSummary;
            const totalChanges =
              summary.stagesAdded + summary.stagesUpdated + summary.stagesRemoved +
              summary.questionnairesAdded + summary.questionnairesUpdated + summary.questionnairesRemoved +
              summary.representativesAdded + summary.representativesUpdated + summary.representativesRemoved +
              summary.questionsAdded + summary.questionsUpdated + summary.questionsRemoved;

            if (summary.blockedReasons?.length) {
              const reasons = summary.blockedReasons.join('\n• ');
              this.notificationService.showError(this.translate.instant('notifications.project_form.update_blocked', { reasons }));
              return;
            }

            if (summary.warnings?.length) {
              const warns = summary.warnings.join('\n• ');
              this.notificationService.showWarning(this.translate.instant('notifications.project_form.updated_with_warnings', { warns }));
            } else {
              this.notificationService.showSuccess(
                totalChanges > 0
                  ? `${this.translate.instant('projects.messages.updated')} ${totalChanges} alteração(ões) aplicada(s).`
                  : this.translate.instant('projects.messages.updated')
              );
            }

            this.routerService.navigateTo(`/projects/${this.projectId}`);
          } catch (error_) {
            LoggerService.error('Erro inesperado ao processar resposta de atualização', error_);
            this.notificationService.showError(this.translate.instant('notifications.project_form.save_error'));
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
          this.notificationService.showError(this.translate.instant('notifications.project_form.operation_blocked', { errorList }));
          return;
        }
      } catch (parseError) {
        LoggerService.warn('EditCascataProjectForm: Erro ao parsear detalhes do conflito.', parseError);
      }
      this.notificationService.showError(this.translate.instant('notifications.project_form.conflict_retry'));
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
    if (!name) throw new Error(this.translate.instant('projects.form.validation.name_required'));
    if (!formValue.startDate) throw new Error(this.translate.instant('projects.form.validation.start_date_required'));

    const aiUsageScopes: string[] = formValue.aiUsageScopes ?? [];
    return {
      name,
      startDate: formValue.startDate,
      deadline: formValue.deadline || null,
      aiUsageScopes: aiUsageScopes.length ? aiUsageScopes : undefined,
      dryRun: false,
      stages: this.buildStagePayloads(),
      iterations: this.buildIterationPayloads(),
      questionnaires: this.buildQuestionnairePayloads(),
      representatives: this.buildRepresentativePayloads(),
    };
  }

  private buildStagePayloads(): UpdateStagePayload[] {
    return this.stepsFormArray.controls
      .map((control, index) => ({
        id: this.normalizeEntityId(control.get('_entityId')?.value),
        name: control.get('name')?.value,
        weight: Number(control.get('weight')?.value) || 0,
        sequence: Number(control.get('sequence')?.value) || index + 1,
        durationDays: Number(control.get('durationDays')?.value) || undefined,
        applicationStartDate: control.get('applicationStartDate')?.value || null,
        applicationEndDate: control.get('applicationEndDate')?.value || null,
      }))
      .filter((s) => Boolean(s.name))
      .sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0));
  }

  private buildIterationPayloads(): UpdateIterationPayload[] {
    return [];
  }

  private buildQuestionnairePayloads(): UpdateQuestionnairePayload[] {
    return this.questionnairesFormArray.controls
      .map((control, index) => {
        const questions = control.get('questions')?.value as QuestionData[] | undefined;
        const stageName = control.get('stageName')?.value || control.get('name')?.value;
        return {
          id: this.normalizeEntityId(control.get('_entityId')?.value),
          name: control.get('name')?.value,
          weight: Number(control.get('weight')?.value) || 0,
          sequence: Number(control.get('sequence')?.value) || index + 1,
          stageName,
          iterationName: control.get('iterationName')?.value || null,
          domain: control.get('domain')?.value || null,
          description: control.get('description')?.value || null,
          applicationStartDate: control.get('applicationStartDate')?.value || null,
          applicationEndDate: control.get('applicationEndDate')?.value || null,
          questions: this.buildQuestionPayloads(questions),
        };
      })
      .filter((q) => Boolean(q.name))
      .sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0));
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

  private calculateProjectDuration(): number {
    const startDate = this.projectForm.get('startDate')?.value;
    const deadline = this.projectForm.get('deadline')?.value;
    if (!startDate || !deadline) return 0;
    return FormUtils.calculateBusinessDays(
      BusinessDaysUtils.parseISODate(startDate),
      BusinessDaysUtils.parseISODate(deadline)
    );
  }

  private getExistingStagesForModal(): { index: number; weight: number; durationDays: number; sequence: number }[] {
    return this.stepsFormArray.controls.map((control, index) => ({
      index,
      weight: Number(control.get('weight')?.value) || 0,
      durationDays: Number(control.get('durationDays')?.value) || 0,
      sequence: Number(control.get('sequence')?.value) || 1,
    }));
  }

  private ensureStageDurationDays(
    stages: ProjectStageDetail[],
    projectStartDate: string
  ): ProjectStageDetail[] {
    if (!projectStartDate || !stages.length) return stages;

    let stageStart = BusinessDaysUtils.parseISODate(projectStartDate);
    const result: ProjectStageDetail[] = [];

    for (const stage of stages) {
      if (stage.durationDays && stage.durationDays > 0) {
        result.push(stage);
        stageStart = FormUtils.addBusinessDays(stageStart, stage.durationDays);
        continue;
      }

      const estimated = this.estimateDurationDays(stageStart, stage.applicationStartDate, stage.applicationEndDate);
      result.push({ ...stage, durationDays: estimated });
      stageStart = FormUtils.addBusinessDays(stageStart, estimated);
    }

    return result;
  }

  private estimateDurationDays(
    stageStart: Date,
    applicationStartDate?: string | null,
    applicationEndDate?: string | null
  ): number {
    if (!applicationStartDate || !applicationEndDate) return 0;

    const appStart = BusinessDaysUtils.parseISODate(applicationStartDate);
    const appEnd = BusinessDaysUtils.parseISODate(applicationEndDate);

    if (Number.isNaN(appStart.getTime()) || Number.isNaN(appEnd.getTime()) || Number.isNaN(stageStart.getTime())) {
      return 0;
    }

    const openingOffset = FormUtils.calculateBusinessDays(stageStart, appStart);
    const closingOffset = FormUtils.calculateBusinessDays(stageStart, appEnd);

    if (closingOffset <= 0) return 0;

    const estimate = Math.round(closingOffset / 0.9);

    for (let candidate = Math.max(1, estimate - 2); candidate <= estimate + 2; candidate++) {
      const expectedOpen = Math.max(Math.round(candidate * 0.1), 0);
      const expectedClose = Math.max(Math.round(candidate * 0.9), expectedOpen);
      if (expectedOpen === openingOffset && expectedClose === closingOffset) {
        return candidate;
      }
    }

    return Math.max(estimate, 1);
  }

  private getExistingSequences(): number[] {
    return this.stepsFormArray.controls
      .map((c) => Number(c.get('sequence')?.value))
      .filter((s) => Number.isFinite(s) && s > 0);
  }

  private getExistingSequencesExcluding(excludeIndex: number): number[] {
    return this.stepsFormArray.controls
      .map((c, i) => ({ i, s: Number(c.get('sequence')?.value) }))
      .filter((x) => x.i !== excludeIndex)
      .map((x) => x.s)
      .filter((s) => Number.isFinite(s) && s > 0);
  }

  private handleSequenceOnCreate(newSequence: number): void {
    for (const control of this.stepsFormArray.controls) {
      const cur = control.get('sequence')?.value;
      if (cur >= newSequence) {
        control.patchValue({ sequence: cur + 1 }, { emitEvent: false });
      }
    }
  }

  private handleSequenceOnEdit(editIndex: number, newSequence: number): void {
    const edited = this.stepsFormArray.at(editIndex);
    const old = edited.get('sequence')?.value;
    if (old === newSequence) return;

    const conflict = this.stepsFormArray.controls.findIndex(
      (c, i) => i !== editIndex && c.get('sequence')?.value === newSequence
    );
    if (conflict !== -1) {
      this.stepsFormArray.at(conflict).patchValue({ sequence: old }, { emitEvent: false });
    }
    edited.patchValue({ sequence: newSequence }, { emitEvent: false });
  }

  private recalculateAllStageRanges(): void {
    const startDate = this.projectForm.get('startDate')?.value;
    if (!startDate) return;

    const sorted = this.stepsFormArray.controls
      .map((c, i) => ({ i, seq: c.get('sequence')?.value || 999 }))
      .sort((a, b) => a.seq - b.seq || a.i - b.i);

    let prev: Date = BusinessDaysUtils.parseISODate(startDate);
    for (const item of sorted) {
      const step = this.stepsFormArray.at(item.i);
      const dur = Number(step.get('durationDays')?.value) || 0;
      if (dur > 0) {
        const stageStart = new Date(prev);
        const openOff = Math.max(Math.round(dur * 0.1), 0);
        const closeOff = Math.max(Math.round(dur * 0.9), openOff);
        const appStart = FormUtils.addBusinessDays(stageStart, openOff);
        const appEnd = FormUtils.addBusinessDays(stageStart, closeOff);
        step.patchValue({
          applicationStartDate: FormUtils.formatDateISO(appStart),
          applicationEndDate: FormUtils.formatDateISO(appEnd),
          dateRange: `${FormUtils.formatDateBR(FormUtils.formatDateISO(appStart))} - ${FormUtils.formatDateBR(FormUtils.formatDateISO(appEnd))}`,
        }, { emitEvent: false });
        prev = FormUtils.addBusinessDays(stageStart, dur);
      }
    }
    this.syncQuestionnairesWithSteps();
    this.cdr.detectChanges();
  }

  private syncQuestionnairesWithSteps(): void {
    const existingQuestionnaires = this.questionnairesFormArray.getRawValue() || [];

    const queueByName = new Map<string, Record<string, unknown>>();
    for (const q of existingQuestionnaires) {
      const key = (q['stageName'] || q['name']) as string;
      if (key) queueByName.set(key, q);
    }

    while (this.questionnairesFormArray.length > 0) {
      this.questionnairesFormArray.removeAt(0);
    }

    const sorted = this.stepsFormArray.controls
      .map((c, i) => ({ i, seq: c.get('sequence')?.value || 999 }))
      .sort((a, b) => a.seq - b.seq || a.i - b.i);

    let lastQuestionsTemplate: unknown[] = [];
    for (const q of existingQuestionnaires) {
      const questions = q['questions'];
      if (Array.isArray(questions) && questions.length > 0) {
        lastQuestionsTemplate = questions;
      }
    }

    const usedNames = new Set<string>();

    for (let pos = 0; pos < sorted.length; pos++) {
      const item = sorted[pos];
      const step = this.stepsFormArray.at(item.i);
      const stepName = step.get('name')?.value;
      const startDate = step.get('applicationStartDate')?.value || '';
      const endDate = step.get('applicationEndDate')?.value || '';

      let existing = queueByName.get(stepName);
      if (existing) {
        usedNames.add(stepName);
      }

      if (!existing && pos < existingQuestionnaires.length) {
        const posCandidate = existingQuestionnaires[pos];
        const posName = (posCandidate['stageName'] || posCandidate['name']) as string;
        if (posName && !usedNames.has(posName)) {
          existing = posCandidate;
          usedNames.add(posName);
        }
      }

      if (existing) {
        this.questionnairesFormArray.push(this.fb.group({
          _entityId: [existing['_entityId'] ?? null],
          name: [stepName, [Validators.required]],
          sequence: [item.seq],
          stageName: [stepName],
          iterationName: [existing['iterationName'] ?? null],
          domain: [existing['domain'] ?? null],
          description: [existing['description'] ?? null],
          applicationStartDate: [startDate],
          applicationEndDate: [endDate],
          weight: [existing['weight'] ?? 0],
          questions: [existing['questions'] ?? []],
        }));
      } else {
        const fallbackQuestions = lastQuestionsTemplate.length > 0
          ? JSON.parse(JSON.stringify(lastQuestionsTemplate))
          : [];
        this.questionnairesFormArray.push(this.fb.group({
          _entityId: [null],
          name: [stepName, [Validators.required]],
          sequence: [item.seq],
          stageName: [stepName],
          iterationName: [null],
          domain: [null],
          description: [null],
          applicationStartDate: [startDate],
          applicationEndDate: [endDate],
          weight: [0],
          questions: [fallbackQuestions],
        }));
      }
    }
    this.cdr.detectChanges();
  }

  hasStagesExceedDeadlineError(): boolean {
    return !!this.projectForm.errors?.['stageExceedsDeadline'];
  }

  getStagesExceedDeadlineMessage(): string {
    return this.projectForm.errors?.['stageExceedsDeadline'] ?? '';
  }

  getDeadlineErrorMessage(): string {
    const error = this.projectForm.errors?.['deadlineTooEarly'];
    if (!error) return '';
    return this.translate.instant('projects.form.validation.deadline_too_early', {
      ...error,
      stageName: error.stageName ?? this.translate.instant('common.stage'),
    });
  }

  getStartDateErrorMessage(): string {
    const error = this.projectForm.errors?.['startDateTooLate'];
    if (!error) return '';
    return this.translate.instant('projects.form.validation.start_date_too_late', error);
  }

  getStageExceedsDeadlineMessage(): string {
    const error = this.projectForm.errors?.['stageExceedsDeadline'];
    if (!error) return '';
    return this.translate.instant('projects.form.validation.stage_exceeds_deadline', error);
  }
}
