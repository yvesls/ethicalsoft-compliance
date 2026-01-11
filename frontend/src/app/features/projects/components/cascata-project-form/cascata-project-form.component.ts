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
import { ProjectDatesValidators } from '../../../../shared/validators/project-dates.validator';
import { StagesDeadlineValidator } from '../../../../shared/validators/stages-deadline.validator';
import { FormUtils } from '../../../../shared/utils/form-utils';
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
import { StageCascataModalComponent, StageCascataData } from '../stage-cascata-modal/stage-cascata-modal.component';
import { RepresentativeModalComponent, RepresentativeData } from '../representative-modal/representative-modal.component';
import { QuestionData } from '../question-modal/question-modal.component';
import { ProjectCreationConfirmModalComponent } from '../project-creation-confirm-modal/project-creation-confirm-modal.component';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import {
  ProjectTemplate,
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
  sequence: number;
  applicationStartDate: string;
  applicationEndDate: string;
  stageName?: string;
  questions?: QuestionData[];
}

interface QuestionnaireUpdatePayload extends Questionnaire {
  questionnaireIndex: number | null;
  questions: QuestionData[];
}

interface CascataStageFormValue {
  name: string;
  weight: number;
  sequence: number;
  dateRange: string;
  durationDays: number;
  applicationStartDate: string;
  applicationEndDate: string;
}

type PanelKey = 'project' | 'steps' | 'representatives' | 'questionnaires';
type PanelStates = Record<PanelKey, boolean>;

interface CascataProjectRouteParams extends GenericParams {
  questionnaireUpdate?: QuestionnaireUpdatePayload;
}

interface CascataProjectFormValue {
  template: string | null;
  name: string;
  type: ProjectType;
  startDate: string | null;
  deadline: string | null;
  steps?: CascataStageFormValue[];
  representatives?: Representative[];
  questionnaires?: Questionnaire[];
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
export class CascataProjectFormComponent extends BasePageComponent<CascataProjectRouteParams> implements OnInit {
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
    steps: false,
    representatives: false,
    questionnaires: false,
  };
  private questionnaireQuestionErrors = new Set<number>();

  public templateOptions: SelectOption[] = [];
  public projectTypeOptions: SelectOption[] = [
    { value: ProjectType.Cascata, label: 'Cascata' },
  ];

  public availableRoles: RoleSummary[] = [];
  private roleNameById = new Map<number, string>();

  private hasRestoredSteps = false;
  private lastValidPanelIndex = 0;
  private selectedTemplateData: ProjectTemplate | null = null;

  private readonly PANEL_ORDER: PanelKey[] = ['project', 'steps', 'representatives', 'questionnaires'];

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
    this.loadRoles();
  }

  protected override loadParams(
    params: RouteParams<CascataProjectRouteParams>
  ): void {
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
        for (const stepControl of this.stepsFormArray.controls) {
          stepControl.updateValueAndValidity({ emitEvent: false });
        }
      });
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
      const ids = group.get('roleIds')?.value as number[] | undefined;
      if (ids?.length) {
        continue;
      }

      const storedNames = group.get('roleNames')?.value as string[] | undefined;
      if (storedNames?.length) {
        const mappedIds = this.mapRoleNamesToIds(storedNames);
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

    const matchedIds: number[] = [];
    for (const role of this.availableRoles) {
      if (normalized.includes(role.name.trim().toLowerCase())) {
        matchedIds.push(role.id);
      }
    }

    return Array.from(new Set(matchedIds));
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
    const roleIds = repFormGroup.get('roleIds')?.value as number[] | undefined;
    const mapped = this.getRoleNamesFromIds(roleIds);
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

    const roleNames = this.getRoleNamesFromFormGroup(repControl);
    return roleNames.length ? roleNames.join(', ') : '-';
  }

  private recalculateAllStageRanges(): void {
    const startDate = this.projectForm.get('startDate')?.value;
    if (!startDate) return;

    const sortedSteps = this.getSortedStepsBySequence();

    let previousStageEndDate: Date = new Date(startDate);

    for (const stepInfo of sortedSteps) {
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
  }

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

  private buildCascataStepsForm(data?: CascataStageFormValue[]): FormArray {
    const stepsData = data ?? [];
    const stepGroups = stepsData.map((step) => {
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
    const representativesData = data ?? [];
    const repGroups = representativesData.map((rep) => {
      return this.fb.group({
        id: [rep.id ?? null],
        userId: [rep.userId ?? null],
        projectId: [rep.projectId ?? null],
        firstName: [rep.firstName, Validators.required],
        lastName: [rep.lastName, Validators.required],
        email: [rep.email, [Validators.required, Validators.email]],
        weight: [rep.weight, [Validators.required, Validators.min(1)]],
        roleIds: [this.mapRepresentativeRoleIds(rep), [Validators.required, Validators.minLength(1)]],
        roleNames: [rep.roleNames ?? []],
      });
    });
    return this.fb.array(repGroups);
  }

  addStep(): void {
  this.openCreateStageModal();
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

    const modalRef = this.modalService.getActiveInstance<StageCascataModalComponent>();
    if (!modalRef) {
      return;
    }

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

    const modalRef = this.modalService.getActiveInstance<StageCascataModalComponent>();
    if (!modalRef) {
      return;
    }

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
    for (const control of this.stepsFormArray.controls) {
      const currentSequence = control.get('sequence')?.value;

      if (currentSequence >= newSequence) {
        control.patchValue({ sequence: currentSequence + 1 }, { emitEvent: false });
      }
    }
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
      this.stepsFormArray.removeAt(index);
      this.recalculateAllStageRanges();
      this.cdr.detectChanges();
    }
  }

  private syncQuestionnairesWithSteps(): void {
    const previousValues = (this.questionnairesFormArray.getRawValue() || []) as Questionnaire[];
    const questionnaireByStage = new Map<string, Questionnaire | undefined>(
      previousValues.map((item) => [item.stageName || item.name, item])
    );
    while (this.questionnairesFormArray.length > 0) {
      this.questionnairesFormArray.removeAt(0);
    }

    const sortedSteps = this.getSortedStepsBySequence();

    for (const stepInfo of sortedSteps) {
      const stepControl = this.stepsFormArray.at(stepInfo.index);
      const stepName = stepControl.get('name')?.value;
      const startDate = stepControl.get('applicationStartDate')?.value || '';
      const endDate = stepControl.get('applicationEndDate')?.value || '';
      const cached = questionnaireByStage.get(stepName);
      const initialQuestions = cached?.questions?.length
        ? this.cloneQuestions(cached.questions, stepName)
        : this.getTemplateQuestionsForStage(stepName);

      this.questionnairesFormArray.push(
        this.fb.group({
          name: [stepName, [Validators.required]],
          stageName: [stepName],
          sequence: [stepInfo.sequence],
          applicationStartDate: [startDate],
          applicationEndDate: [endDate],
          questions: [initialQuestions],
        })
      );
  }

    this.cdr.detectChanges();
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
        firstName: [newRepresentative.firstName, Validators.required],
        lastName: [newRepresentative.lastName, Validators.required],
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

  private getQuestionsForQuestionnaire(questionnaire: Questionnaire | undefined): QuestionData[] {
    if (!questionnaire) {
      return [];
    }

    if (questionnaire.questions?.length) {
      return questionnaire.questions;
    }

    if (!this.selectedTemplateData?.questionnaires) {
      return [];
    }

    // Encontrar o questionário correspondente no template pela etapa
    const templateQuestionnaire = this.selectedTemplateData.questionnaires.find(
      (tq: TemplateQuestionnaireDTO) => tq.stageName === questionnaire.stageName || tq.name === questionnaire.name
    );

    if (!templateQuestionnaire?.questions) {
      return [];
    }

    // Mapear as perguntas do template para o formato esperado pelo componente
    return templateQuestionnaire.questions.map((question: TemplateQuestionDTO, index: number) => {
      const stageNames = this.getTemplateQuestionStageNames(question);
      return {
        id: `${Date.now()}-${index}`,
        value: question.value,
        roleIds: this.extractRoleIds(question.roles, question.roleNames),
        roleNames: this.extractRoleNames(question.roles, question.roleNames),
        stageNames,
        stageName: stageNames[0] ?? questionnaire.stageName ?? null,
        categoryStageName: stageNames[0] ?? questionnaire.stageName ?? null,
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

    this.handleQuestionnaireQuestionUpdate(update.questionnaireIndex, updatedQuestions);

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

  protected override save(): RouteParams<CascataProjectRouteParams> {
    return {
      formValue: this.projectForm.getRawValue(),
      panelStates: this.panelStates,
      lastValidPanelIndex: this.lastValidPanelIndex,
    };
  }

  protected override restore(restoreParameter: RestoreParams<CascataProjectRouteParams>): void {
    if (restoreParameter.hasParams) {
      const formValue = restoreParameter['formValue'] as CascataProjectFormValue | undefined;
      this.applyRestoreFormValue(formValue);
      this.restorePanelMetadata(restoreParameter);
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

    for (const [index, control] of this.stepsFormArray.controls.entries()) {
      const extraDay = index < remainder ? 1 : 0;
      const assignedDays = daysPerStage + extraDay;
      control.get('durationDays')?.setValue(assignedDays);
    }
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

  private loadPanelData(panelKey: PanelKey): void {
    if (!this.selectedTemplateData) return;

    switch (panelKey) {
      case 'steps': {
        if (
          this.selectedTemplateData.stages?.length &&
          !this.hasUserModifiedSteps()
        ) {
          const stepsData = this.selectedTemplateData.stages.map((stage: TemplateStageDTO) =>
            this.mapStageTemplateToForm(stage)
          );
          this.projectForm.setControl('steps', this.buildCascataStepsForm(stepsData));

          this.distributeEqualDurationDays();
          this.recalculateAllStageRanges();
        }
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

      case 'questionnaires': {
        this.syncQuestionnairesWithSteps();
        break;
      }
    }
  }

  private isPanelValid(panelKey: PanelKey): boolean {
    switch (panelKey) {
      case 'project': {
        const requiredControls = ['template', 'name', 'startDate', 'deadline'];
        const allRequiredValid = requiredControls.every(controlName => this.projectForm.get(controlName)?.valid);
        const groupErrors = this.projectForm.errors;
        const hasRelevantGroupError =
          groupErrors?.['dateRange'] ||
          groupErrors?.['deadlineTooEarly'] ||
          groupErrors?.['startDateTooLate'];
        return allRequiredValid && !hasRelevantGroupError;
      }

      case 'steps': {
        const stepsArray = this.projectForm.get('steps') as FormArray;
        return Boolean(stepsArray && stepsArray.valid && stepsArray.length > 0 && !this.hasStagesExceedDeadlineError());
      }

      case 'representatives': {
        const repsArray = this.projectForm.get('representatives') as FormArray;
        return Boolean(repsArray && repsArray.valid && repsArray.length > 0);
      }

      case 'questionnaires':
        return true;

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
            this.buildTemplateCloneRequest(payload.name),
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
    const formValue = this.projectForm.getRawValue() as CascataProjectFormValue;
    const templateId = formValue.template;
    const startDate = formValue.startDate;
    const projectName = (formValue.name || '').trim();

    if (!templateId) {
      throw new Error('Selecione um template antes de salvar o projeto.');
    }

    if (!startDate) {
      throw new Error('Informe a data de início do projeto.');
    }

    if (!projectName) {
      throw new Error('Informe o nome do projeto.');
    }

    const stages = this.buildStagePayload();
    const questionnaires = this.buildQuestionnairePayload();
    const representatives = this.buildRepresentativePayload();

    return {
      name: projectName,
      templateId,
      type: ProjectType.Cascata,
      startDate,
      deadline: formValue.deadline || null,
      status: 'ABERTO',
      stages: stages.length ? stages : undefined,
      questionnaires: questionnaires.length ? questionnaires : undefined,
      representatives: representatives.length ? representatives : undefined,
    };
  }

  private buildStagePayload(): StagePayload[] {
    return this.stepsFormArray.controls
      .map((control, index) => {
        const duration = Number(control.get('durationDays')?.value ?? 0);
        return {
          name: control.get('name')?.value,
          weight: Number(control.get('weight')?.value) || 0,
          sequence: Number(control.get('sequence')?.value) || index + 1,
          durationDays: duration > 0 ? duration : undefined,
          applicationStartDate: control.get('applicationStartDate')?.value || null,
          applicationEndDate: control.get('applicationEndDate')?.value || null,
        };
      })
      .filter((stage) => Boolean(stage.name))
      .sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0));
  }

  private buildQuestionnairePayload(): QuestionnairePayload[] {
    return this.questionnairesFormArray.controls
      .map((control, index) => {
        const questions = control.get('questions')?.value as QuestionData[] | undefined;
        const stageName = control.get('stageName')?.value || control.get('name')?.value;
        const stageWeight = this.getStageWeightByName(stageName);
        return {
          name: control.get('name')?.value,
          sequence: Number(control.get('sequence')?.value) || index + 1,
          stageName,
          applicationStartDate: control.get('applicationStartDate')?.value || null,
          applicationEndDate: control.get('applicationEndDate')?.value || null,
          weight: stageWeight,
          questions: this.mapQuestions(questions),
        };
      })
      .filter((questionnaire) => Boolean(questionnaire.name))
      .sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0));
  }

  private getStageWeightByName(stageName?: string | null): number {
    if (!stageName) {
      return 0;
    }

    for (const control of this.stepsFormArray.controls) {
      const group = control as FormGroup;
      if (group.get('name')?.value === stageName) {
        const parsedWeight = Number(group.get('weight')?.value);
        return Number.isFinite(parsedWeight) ? parsedWeight : 0;
      }
    }

    return 0;
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

  private getTemplateQuestionsForStage(stageName?: string | null): QuestionData[] {
    if (!stageName || !this.selectedTemplateData?.questionnaires?.length) {
      return [];
    }

    const templateQuestionnaire = this.selectedTemplateData.questionnaires.find(
      (questionnaire: TemplateQuestionnaireDTO) =>
        questionnaire.stageName === stageName || questionnaire.name === stageName
    );

    if (!templateQuestionnaire?.questions?.length) {
      return [];
    }

    return templateQuestionnaire.questions.map((question: TemplateQuestionDTO, index: number) => ({
      id: this.generateQuestionId(stageName, index),
      value: question.value,
      roleIds: this.extractRoleIds(question.roles, question.roleNames),
      roleNames: this.extractRoleNames(question.roles, question.roleNames),
      stageNames: this.getTemplateQuestionStageNames(question),
      stageName: question.stageName ?? stageName ?? null,
      categoryStageName: question.stageName ?? stageName ?? null,
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

  private mapStageTemplateToForm(stage: TemplateStageDTO): CascataStageFormValue {
    return {
      name: stage.name,
      weight: stage.weight,
      sequence: stage.sequence ?? 1,
      dateRange: '',
      durationDays: stage.durationDays ?? 0,
      applicationStartDate: '',
      applicationEndDate: '',
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

  private applyRestoreFormValue(formValue?: CascataProjectFormValue): void {
    if (!formValue) {
      return;
    }

    const stepsData = formValue.steps || [];
    if (stepsData.length > 0) {
      this.hasRestoredSteps = true;
    }

    this.projectForm.setControl('steps', this.buildCascataStepsForm(stepsData));
    this.projectForm.setControl(
      'representatives',
      this.buildRepresentativesForm(formValue.representatives || [])
    );

    if (formValue.questionnaires?.length) {
      this.projectForm.setControl('questionnaires', this.buildQuestionnairesForm(formValue.questionnaires));
    } else {
      this.syncQuestionnairesWithSteps();
    }

    this.projectForm.patchValue(formValue);
  }

  private restorePanelMetadata(restoreParameter: RestoreParams<CascataProjectRouteParams>): void {
    if (restoreParameter['panelStates']) {
      this.panelStates = restoreParameter['panelStates'] as PanelStates;
    }

    if (typeof restoreParameter['lastValidPanelIndex'] === 'number') {
      this.lastValidPanelIndex = restoreParameter['lastValidPanelIndex'];
    }
  }
}
