import { Component, inject, OnInit, OnDestroy, signal, WritableSignal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';

import { BasePageComponent, RestoreParams } from '../../../../core/abstractions/base-page.component';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ActionType } from '../../../../shared/enums/action-type.enum';
import { QuestionModalComponent, QuestionData } from '../../components/question-modal/question-modal.component';
import { AccordionPanelComponent } from '../../../../shared/components/accordion-panel/accordion-panel.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { SelectComponent, SelectOption } from '../../../../shared/components/select/select.component';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { GenericParams, RouteParams } from '../../../../core/services/router.service';
import { RoleService } from '../../../../core/services/role.service';
import { QuestionnaireQueryStore } from '../../../../shared/stores/questionnaire-query.store';
import { QuestionnaireQuestionResponse, QuestionnaireRawResponse } from '../../../../shared/interfaces/questionnaire/questionnaire-query.interface';
import { Page } from '../../../../shared/interfaces/pageable.interface';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';

interface CascataQuestionnaireRouteParams extends GenericParams {
  questionnaireIndex?: number;
  projectId?: string;
  questionnaireId?: number;
  mode?: ActionType;
  stageName?: string;
  sequence?: number | string;
  name?: string;
  applicationStartDate?: string;
  applicationEndDate?: string;
  questions?: QuestionData[];
}

type CascataQuestionnaireRestoreParams = RestoreParams<CascataQuestionnaireRouteParams>;

@Component({
  selector: 'app-cascata-questionnaire-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, AccordionPanelComponent, InputComponent, SelectComponent, PaginationComponent],
  templateUrl: './cascata-questionnaire-form.component.html',
  styleUrls: ['./cascata-questionnaire-form.component.scss']
})
export class CascataQuestionnaireFormComponent extends BasePageComponent<CascataQuestionnaireRouteParams> implements OnInit, OnDestroy {
  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private notificationService = inject(NotificationService);
  private roleService = inject(RoleService);
  private questionnaireQueryStore = inject(QuestionnaireQueryStore);
  private questionnaireIndex: number | null = null;
  private questionnaireMetadata: CascataQuestionnaireRouteParams | null = null;
  private skipStatePersistence = false;
  private currentStageName: string | null = null;

  readonly mode = signal<ActionType>(ActionType.EDIT);
  readonly isViewMode = signal(false);
  readonly isLoadingQuestions = signal(false);
  readonly viewProjectId = signal<string | null>(null);
  readonly viewQuestionnaireId = signal<number | null>(null);
  readonly pagination = signal<Page<unknown> | null>(null);
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);

  form!: FormGroup;
  questions: WritableSignal<QuestionData[]> = signal([]);

  questionnaireDataAccordionOpen = true;
  questionsAccordionOpen = true;

  searchTerm = '';
  selectedRole = '';

  roleFilterOptions: SelectOption[] = [];

  private questionModalSubscription?: Subscription;

  constructor() {
    super();
  }

  protected override onInit(): void {
    this.initializeForm();
    this.loadRoleFilterOptions();
  }

  private initializeForm(): void {
    this.form = this.fb.group({
      sequence: [{ value: '', disabled: true }],
      name: ['', Validators.required],
      applicationStartDate: [{ value: '', disabled: true }],
      applicationEndDate: [{ value: '', disabled: true }],
    });
  }

  private loadRoleFilterOptions(): void {
    this.roleService
      .getRoles()
      .pipe(take(1))
      .subscribe({
        next: (roles) => {
          this.roleFilterOptions = (roles ?? []).map((role) => ({
            value: role.name,
            label: role.name,
          }));
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Falha ao carregar lista de papéis para filtro de questionário', error);
        }
      });
  }

  protected override loadParams(params: RouteParams<CascataQuestionnaireRouteParams>): void {
    if (!params) return;

    const data = (params.p ?? params) as CascataQuestionnaireRouteParams;

    const incomingMode = data.mode ?? ActionType.EDIT;
    this.mode.set(incomingMode);
    this.isViewMode.set(incomingMode === ActionType.VIEW);
    this.viewProjectId.set(typeof data.projectId === 'string' ? data.projectId : null);
    this.viewQuestionnaireId.set(typeof data.questionnaireId === 'number' ? data.questionnaireId : null);

    this.questionnaireIndex = typeof data.questionnaireIndex === 'number' ? data.questionnaireIndex : null;
    this.questionnaireMetadata = data;
    this.currentStageName = data.stageName ?? data.name ?? null;

    if (data.questions && Array.isArray(data.questions)) {
      this.questions.set(this.annotateQuestionsWithStage(data.questions));
    }

    this.form.patchValue({
      sequence: data.sequence,
      name: data.name,
      applicationStartDate: data.applicationStartDate,
      applicationEndDate: data.applicationEndDate
    });

    if (this.isViewMode()) {
      this.applyReadOnlyState();
      this.loadViewData();
    }
    this.cdr.detectChanges();
  }

  private applyReadOnlyState(): void {
    this.form.disable({ emitEvent: false });
  }

  private loadViewData(): void {
    const projectId = this.viewProjectId();
    const questionnaireId = this.viewQuestionnaireId();

    if (!projectId || questionnaireId === null) {
      return;
    }

    this.isLoadingQuestions.set(true);
    this.questionnaireQueryStore
      .getQuestionnaireRaw(projectId, questionnaireId)
      .pipe(take(1))
      .subscribe({
        next: (raw: QuestionnaireRawResponse) => {
          this.form.patchValue(
            {
              sequence: raw.id,
              name: raw.name,
              applicationStartDate: raw.applicationStartDate,
              applicationEndDate: raw.applicationEndDate,
            },
            { emitEvent: false }
          );
          this.cdr.markForCheck();
        },
        error: () => {
          this.notificationService.showError('Não foi possível carregar os dados do questionário.');
        },
      });

    this.fetchQuestionsPage(0);
  }

  private fetchQuestionsPage(page: number): void {
    const projectId = this.viewProjectId();
    const questionnaireId = this.viewQuestionnaireId();

    if (!projectId || questionnaireId === null) {
      this.isLoadingQuestions.set(false);
      return;
    }

    this.currentPage.set(page);
    this.isLoadingQuestions.set(true);

    this.questionnaireQueryStore
      .searchQuestions(projectId, questionnaireId, null, page, this.pageSize())
      .pipe(take(1))
      .subscribe({
        next: (result) => {
          this.pagination.set(result as unknown as Page<unknown>);
          const questions = (result.content ?? []).map((q: QuestionnaireQuestionResponse) => {
            return this.assignStageMetadata({
              id: String(q.id),
              value: q.text,
              roleIds: q.roleIds ?? [],
              roleNames: [],
              stageNames: q.stageNames ?? [],
            } as unknown as QuestionData);
          });
          this.questions.set(questions);
          this.isLoadingQuestions.set(false);
          this.cdr.markForCheck();
        },
        error: () => {
          this.isLoadingQuestions.set(false);
          this.notificationService.showError('Não foi possível carregar as perguntas do questionário.');
        },
      });
  }

  onPageChange(page: number): void {
    const targetPage = Math.max(0, page - 1);
    this.fetchQuestionsPage(targetPage);
  }

  getControl(controlName: string) {
    return this.form.get(controlName);
  }

  get filteredQuestions(): QuestionData[] {
    let filtered = this.questions();
    const term = this.searchTerm.toLowerCase().trim();
    const role = this.selectedRole;

    if (term) {
      filtered = filtered.filter(q => q.value.toLowerCase().includes(term));
    }

    if (role) {
      filtered = filtered.filter(q => q.roleNames.includes(role));
    }

    return filtered;
  }

  getStageLabels(question: QuestionData): string[] {
    return this.resolveQuestionStageNames(question);
  }

  toggleQuestionnaireDataAccordion(): void {
    this.questionnaireDataAccordionOpen = !this.questionnaireDataAccordionOpen;
  }

  toggleQuestionsAccordion(): void {
    this.questionsAccordionOpen = !this.questionsAccordionOpen;
  }

  openAddQuestionModal(): void {
    if (this.isViewMode()) {
      return;
    }
    this.modalService.open(QuestionModalComponent, 'medium-card', {
      mode: ActionType.CREATE
    });

    const modalInstance = this.modalService.getActiveInstance<QuestionModalComponent>();
    if (modalInstance) {
      this.questionModalSubscription = modalInstance.questionCreated.subscribe((newQuestion: QuestionData) => {
        if (!newQuestion.id) newQuestion.id = Date.now().toString();
        const enriched = this.assignStageMetadata({ ...newQuestion });
        this.questions.update(qs => [...qs, enriched]);
        this.cdr.detectChanges();
      });
    }
  }

  openEditQuestionModal(question: QuestionData): void {
    if (this.isViewMode()) {
      return;
    }
    this.modalService.open(QuestionModalComponent, 'medium-card', {
      mode: ActionType.EDIT,
      editData: question
    });

    const modalInstance = this.modalService.getActiveInstance<QuestionModalComponent>();
    if (modalInstance) {
      this.questionModalSubscription = modalInstance.questionUpdated.subscribe((updatedQuestion: QuestionData) => {
        this.replaceQuestion(updatedQuestion);
        this.cdr.detectChanges();
      });
    }
  }

  deleteQuestion(question: QuestionData): void {
    if (this.isViewMode()) {
        return;
    }
    this.questions.update(qs => qs.filter(q => q.id !== question.id));
  }

  onSave(): void {
    if (this.isViewMode()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.questions().length) {
      this.notificationService.showWarning('Adicione ao menos uma pergunta antes de salvar o questionário.');
      return;
    }

    const hasStageMismatch = this.questions().some((question) => !this.hasStageMetadata(question));
    if (hasStageMismatch) {
      this.notificationService.showWarning('Todas as perguntas precisam estar vinculadas à etapa atual.');
      return;
    }

    const formData = this.form.getRawValue();

    const finalData = {
      ...formData,
      stageName: this.questionnaireMetadata?.stageName || formData.name,
      questionnaireIndex: this.questionnaireIndex,
      questions: this.questions()
    };

    this.navigateBack({ questionnaireUpdate: finalData });
  }

  onCancel(): void {
    this.navigateBack();
  }

  private navigateBack(updatedParams?: GenericParams): void {
    this.skipStatePersistence = true;
    this.routerService.backToPrevious(0, true, updatedParams);
  }

  private annotateQuestionsWithStage(questions?: QuestionData[]): QuestionData[] {
    if (!questions?.length) {
      return [];
    }

    return questions.map((question) => this.assignStageMetadata({ ...question }));
  }

  private assignStageMetadata(question: QuestionData): QuestionData {
    const stageName = this.getEffectiveStageName();
    const normalizedStageName = stageName?.trim();

    if (normalizedStageName?.length) {
      question.stageNames = [normalizedStageName];
      question.stageName = normalizedStageName;
      question.categoryStageName = normalizedStageName;
      return question;
    }

    const stageNames = this.resolveQuestionStageNames(question);
    question.stageNames = stageNames;
    question.stageName = stageNames[0] ?? null;
    question.categoryStageName = stageNames.length === 1 ? stageNames[0] : question.categoryStageName ?? null;
    return question;
  }

  private getEffectiveStageName(): string | null {
    if (this.questionnaireMetadata?.stageName) {
      return this.questionnaireMetadata.stageName;
    }

    if (this.currentStageName) {
      return this.currentStageName;
    }

    const nameControlValue = this.form.get('name')?.value;
    return typeof nameControlValue === 'string' ? nameControlValue : null;
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

    const fallbackStage = this.getEffectiveStageName();
    return fallbackStage ? [fallbackStage.trim()] : [];
  }

  private hasStageMetadata(question: QuestionData | undefined): boolean {
    return this.resolveQuestionStageNames(question).length > 0;
  }

  protected override save(): RouteParams<CascataQuestionnaireRouteParams> {
    return {
      formValue: this.form.getRawValue(),
      questions: this.questions()
    } satisfies CascataQuestionnaireRouteParams;
  }

  protected override restore(restoreParameter: CascataQuestionnaireRestoreParams): void {
    if (!restoreParameter?.hasParams) {
      return;
    }

    const formValue = restoreParameter['formValue'];
    if (formValue) {
      this.form.patchValue(formValue);
      const castFormValue = formValue as { stageName?: string; name?: string };
      const restoredName = castFormValue.stageName ?? castFormValue.name;
      if (typeof restoredName === 'string') {
        this.currentStageName = restoredName;
      }
    }

    const savedQuestions = (restoreParameter['questions'] ?? restoreParameter.p?.['questions']) as QuestionData[] | undefined;
    if (savedQuestions) {
      this.questions.set(this.annotateQuestionsWithStage(savedQuestions));
    }
  }

  private replaceQuestion(updatedQuestion: QuestionData): void {
    const annotated = this.assignStageMetadata({ ...updatedQuestion });
    this.questions.update((qs) => qs.map((q) => (q.id === annotated.id ? annotated : q)));
  }

  override ngOnDestroy(): void {
    if (!this.skipStatePersistence) {
      super.ngOnDestroy();
    }
    this.questionModalSubscription?.unsubscribe();
  }
}
