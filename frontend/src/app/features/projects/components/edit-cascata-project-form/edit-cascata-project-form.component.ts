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
  ChangesSummary,
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
import {
  ProjectUpdatePreviewModalComponent,
} from '../project-update-preview-modal/project-update-preview-modal.component';

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

  public ProjectType = ProjectType;
  public projectForm!: FormGroup;
  public isSubmitting = false;
  public isLoadingPreview = false;
  public isLoadingProject = true;
  public loadError: string | null = null;
  public showQuestionnaireQuestionErrors = false;

  public panelStates: PanelStates = {
    project: true,
    steps: true,
    representatives: true,
    questionnaires: true,
  };

  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Cascata, label: 'Cascata' },
  ];

  public availableRoles: RoleSummary[] = [];
  private roleNameById = new Map<number, string>();
  private questionnaireQuestionErrors = new Set<number>();

  private projectId!: string;
  private projectData: ProjectEditData | null = null;

  private readonly PANEL_ORDER: PanelKey[] = ['project', 'steps', 'representatives', 'questionnaires'];

  ngOnInit(): void {
    this.route.params.pipe(take(1)).subscribe((params) => {
      this.projectId = params['projectId'];
      if (!this.projectId) {
        this.routerService.navigateTo('/projects');
        return;
      }
      this.initForm();
      this.loadRoles();
      this.loadProjectForEdit();
    });
  }

  private initForm(): void {
    this.projectForm = this.fb.group(
      {
        name: ['', [Validators.required]],
        type: [{ value: ProjectType.Cascata, disabled: true }, [Validators.required]],
        startDate: [null, [Validators.required]],
        deadline: [null, [Validators.required, CustomValidators.minDateToday()]],
        steps: this.fb.array([]),
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
    });

    const stepsArray = this.projectForm.get('steps') as FormArray;
    while (stepsArray.length > 0) stepsArray.removeAt(0);
    for (const stage of (data.stages || []).sort((a, b) => a.sequence - b.sequence)) {
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
      value: question.value,
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
      this.notificationService.showWarning('Não foi possível carregar o questionário selecionado.');
      return;
    }

    const projectName = this.projectForm.get('name')?.value || 'Projeto';
    const questions = questionnaire.questions || [];

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

  canOpenPanel(_panelKey: PanelKey): boolean {
    return true;
  }

  onPanelToggled(panelKey: PanelKey, newState: boolean): void {
    this.panelStates[panelKey] = newState;
    this.cdr.detectChanges();
  }

  onAttemptedToggle(_panelKey: PanelKey): void {
  }

  onSubmit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      this.notificationService.showWarning('Revise os campos obrigatórios antes de salvar.');
      return;
    }

    if (!this.validateQuestionnairesHaveQuestions()) return;
    if (this.isSubmitting || this.isLoadingPreview) return;

    this.runDryRunPreview();
  }

  private runDryRunPreview(): void {
    let payload: UpdateProjectRequest;
    try {
      payload = this.buildUpdatePayload(true);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Erro ao preparar os dados.';
      this.notificationService.showError(message);
      return;
    }

    this.isLoadingPreview = true;
    this.cdr.markForCheck();

    this.projectStore
      .updateProject(this.projectId, payload)
      .pipe(
        take(1),
        finalize(() => {
          this.isLoadingPreview = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (response) => {
          this.openPreviewModal(response.changesSummary);
        },
        error: (err) => {
          this.handleUpdateError(err);
        },
      });
  }

  private openPreviewModal(summary: ChangesSummary): void {
    const isBlocked = summary.blockedReasons.length > 0;

    this.modalService.open(ProjectUpdatePreviewModalComponent, 'medium-card', {
      summary,
      isBlocked,
    });

    const modalInstance = this.modalService.getActiveInstance<ProjectUpdatePreviewModalComponent>();
    if (!modalInstance) return;

    modalInstance.confirmed.pipe(take(1)).subscribe(() => {
      this.modalService.close();
      this.applyUpdate();
    });

    modalInstance.canceled.pipe(take(1)).subscribe(() => {
      this.modalService.close();
    });
  }

  private applyUpdate(): void {
    let payload: UpdateProjectRequest;
    try {
      payload = this.buildUpdatePayload(false);
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
          const summary = response.changesSummary;
          const totalChanges =
            summary.stagesAdded + summary.stagesUpdated + summary.stagesRemoved +
            summary.questionnairesAdded + summary.questionnairesUpdated + summary.questionnairesRemoved +
            summary.representativesAdded + summary.representativesUpdated + summary.representativesRemoved +
            summary.questionsAdded + summary.questionsUpdated + summary.questionsRemoved;

          this.notificationService.showSuccess(
            totalChanges > 0
              ? `Projeto atualizado com sucesso. ${totalChanges} alteração(ões) aplicada(s).`
              : 'Projeto atualizado com sucesso.'
          );
          this.routerService.navigateTo(`/projects/${this.projectId}`);
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

  private buildUpdatePayload(dryRun: boolean): UpdateProjectRequest {
    const formValue = this.projectForm.getRawValue();
    const name = (formValue.name || '').trim();
    if (!name) throw new Error('Informe o nome do projeto.');
    if (!formValue.startDate) throw new Error('Informe a data de início do projeto.');

    return {
      name,
      startDate: formValue.startDate,
      deadline: formValue.deadline || null,
      dryRun,
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
      roleIds: this.normalizeRoleIds(q.roleIds),
      stageNames: q.stageNames?.length ? q.stageNames : undefined,
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
    const queueByStage = new Map<string, Record<string, unknown>>();
    for (const q of existingQuestionnaires) {
      const key = q['stageName'] || q['name'];
      if (key) queueByStage.set(key, q);
    }

    while (this.questionnairesFormArray.length > 0) {
      this.questionnairesFormArray.removeAt(0);
    }

    const sorted = this.stepsFormArray.controls
      .map((c, i) => ({ i, seq: c.get('sequence')?.value || 999 }))
      .sort((a, b) => a.seq - b.seq || a.i - b.i);

    for (const item of sorted) {
      const step = this.stepsFormArray.at(item.i);
      const stepName = step.get('name')?.value;
      const startDate = step.get('applicationStartDate')?.value || '';
      const endDate = step.get('applicationEndDate')?.value || '';
      const existing = queueByStage.get(stepName);

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
          questions: [[]],
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
    return this.projectForm.errors?.['deadlineTooEarly'] ?? 'A data de início não pode ser maior que o prazo limite.';
  }

  getStartDateErrorMessage(): string {
    return this.projectForm.errors?.['startDateTooLate'] ?? '';
  }

  getStageExceedsDeadlineMessage(): string {
    return this.projectForm.errors?.['stageExceedsDeadline'] ?? '';
  }
}
