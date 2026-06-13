import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  distinctUntilChanged,
  filter,
  map,
  tap,
} from 'rxjs/operators';
import { forkJoin, take } from 'rxjs';

import { FilterBarComponent } from '../../../../shared/components/filter-bar/filter-bar.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { ListComponent } from '../../../../shared/components/list/list.component';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { Project } from '../../../../shared/interfaces/project/project.interface';
import { RoleService } from '../../../../core/services/role.service';
import {
  ProjectQuestionnaireFilters,
  ProjectQuestionnaireSummary,
  QuestionnaireRespondentStatus,
  RescheduleQuestionnairePayload,
} from '../../../../shared/interfaces/project/project-questionnaire.interface';
import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { ProjectStatus } from '../../../../shared/enums/project-status.enum';
import { TimelineStatus } from '../../../../shared/enums/timeline-status.enum';
import { Page } from '../../../../shared/interfaces/pageable.interface';
import { AuthenticationService, UserInterface } from '../../../../core/services/authentication.service';
import { RoleEnum } from '../../../../shared/enums/role.enum';
import { ProjectContextService } from '../../../../core/services/project-context.service';
import { QuestionnaireResponseStatus } from '../../../../shared/enums/questionnaire-response-status.enum';
import { NotificationService } from '../../../../core/services/notification.service';
import { environment } from '../../../../enviroments/environments';
import { BusinessDaysUtils } from '../../../../core/utils/business-days-utils';
import { DashboardService } from '../../../dashboard/services/dashboard.service';
import { ModalService } from '../../../../core/services/modal.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core'
import { RescheduleQuestionnaireModalComponent } from '../../components/reschedule-questionnaire-modal/reschedule-questionnaire-modal.component';

interface ProjectState {
  data: Project | null;
  status: 'loading' | 'loaded' | 'error';
  error: string | null;
}

interface QuestionnaireListState {
  items: ProjectQuestionnaireSummary[];
  status: 'loading' | 'loaded' | 'error';
  error: string | null;
  pagination: {
    currentPage: number;
    totalItems: number;
    pageSize: number;
  };
}

type NullableDateLike = string | Date | null | undefined;

type QuestionnaireActionMode = 'respond' | 'view';

@Component({
  selector: 'app-project-detail-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FilterBarComponent,
    InputComponent,
    ListComponent,
    PaginationComponent,
    TranslateModule,
  ],
  templateUrl: './project-detail-page.component.html',
  styleUrls: ['./project-detail-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly projectStore = inject(ProjectStore);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly authService = inject(AuthenticationService);
  private readonly projectContext = inject(ProjectContextService);
  private readonly notification = inject(NotificationService);
  private readonly dashboardService = inject(DashboardService);
  private readonly modalService = inject(ModalService);
  private readonly roleService = inject(RoleService);
  private readonly translate = inject(TranslateService);

  private readonly questionnairesPageSize = 5;
  private currentProjectId: string | null = null;
  private readonly sendingReminderIds = signal<Set<number>>(new Set());
  private readonly forceClosingIds = signal<Set<number>>(new Set());
  private readonly reschedulingIds = signal<Set<number>>(new Set());
  private readonly expandedRespondentLists = signal<Set<number>>(new Set());

  private readonly userRoles = signal<string[]>([]);
  private readonly currentUser = signal<UserInterface | null>(null);
  readonly currentUserProjectRoles = signal<string[]>([]);
  readonly isAdmin = computed(() =>
    this.userRoles().includes(RoleEnum.ADMIN)
  );

  readonly isDraft = computed(() => {
    const project = this.projectState().data;
    return project?.status === ProjectStatus.Rascunho;
  });

  readonly isPublishing = signal(false);
  readonly isDeleting = signal(false);

  readonly canDeleteProject = computed(() => {
    const project = this.projectState().data;
    return this.isAdmin() && !!project && project.status !== ProjectStatus.Concluido;
  });

  readonly projectState = signal<ProjectState>({
    data: null,
    status: 'loading',
    error: null,
  });

  readonly questionnairesState = signal<QuestionnaireListState>({
    items: [],
    status: 'loading',
    error: null,
    pagination: {
      currentPage: 1,
      totalItems: 0,
      pageSize: this.questionnairesPageSize,
    },
  });

  readonly filterForm: FormGroup = this.fb.group({
    name: [''],
    stage: [''],
    iteration: [''],
  });

  get projectTypeLabelMap(): Record<ProjectType, string> {
    return {
      [ProjectType.Cascata]: this.translate.instant('projects.type.cascata'),
      [ProjectType.Iterativo]: this.translate.instant('projects.type.iterativo'),
    };
  }

  private get projectStatusLabelMap(): Record<string, string> {
    return {
      ABERTO: this.translate.instant('projects.status.aberto'),
      RASCUNHO: this.translate.instant('projects.status.rascunho'),
      CONCLUIDO: this.translate.instant('projects.status.concluido'),
      ARQUIVADO: this.translate.instant('projects.status.arquivado'),
      EXCLUIDO: this.translate.instant('projects.status.excluido'),
    };
  }

  private get timelineStatusLabelMap(): Record<TimelineStatus, string> {
    return {
      [TimelineStatus.Pendente]: this.translate.instant('questionnaire.timeline_status.pendente'),
      [TimelineStatus.EmAndamento]: this.translate.instant('questionnaire.timeline_status.em_andamento'),
      [TimelineStatus.Concluido]: this.translate.instant('questionnaire.timeline_status.concluido'),
      [TimelineStatus.Atrasado]: this.translate.instant('questionnaire.timeline_status.atrasado'),
    };
  }

  private get respondentStatusLabelMap(): Record<QuestionnaireResponseStatus, string> {
    return {
      [QuestionnaireResponseStatus.Pending]: this.translate.instant('questionnaire.respondent_status.pending'),
      [QuestionnaireResponseStatus.InProgress]: this.translate.instant('questionnaire.respondent_status.in_progress'),
      [QuestionnaireResponseStatus.Completed]: this.translate.instant('questionnaire.respondent_status.completed'),
    };
  }

  ngOnInit(): void {
    this.listenToUserRoles();
    this.listenToRouteChanges();
  }

  onSearch(): void {
    this.loadQuestionnaires(1);
  }

  onResetFilters(): void {
    this.filterForm.reset({ name: '', stage: '', iteration: '' });
    this.loadQuestionnaires(1);
  }

  onPageChange(page: number): void {
    this.loadQuestionnaires(page);
  }

  onEditProject(): void {
    const project = this.projectState().data;
    if (!project) {
      return;
    }

    if (this.isDraft()) {
      this.router.navigate(['/projects/create'], {
        queryParams: { type: project.type, projectId: project.id },
      });
    } else {
      this.router.navigate(['/projects', project.id, 'edit'], {
        queryParams: { type: project.type },
      });
    }
  }

  publishProject(): void {
    const project = this.projectState().data;
    if (!project || !this.isDraft() || this.isPublishing()) {
      return;
    }

    this.notification.showConfirm(
      this.translate.instant('projects.messages.publish_confirm'),
      () => {
        this.isPublishing.set(true);

        this.projectStore
          .publishProject(project.id)
          .pipe(
            take(1),
          )
          .subscribe({
            next: () => {
              this.isPublishing.set(false);
              this.notification.showSuccess(this.translate.instant('projects.messages.published'));
              this.loadProject(project.id);
            },
            error: (error) => {
              this.isPublishing.set(false);
              this.notification.showError(error);
            },
          });
      },
      () => {  }
    );
  }

  deleteProject(): void {
    const project = this.projectState().data;
    if (!project || this.isDeleting()) {
      return;
    }

    this.notification.showConfirm(
      this.translate.instant('projects.messages.delete_confirm', { name: project.name }),
      () => {
        this.isDeleting.set(true);

        this.projectStore
          .deleteProject(project.id)
          .pipe(take(1))
          .subscribe({
            next: () => {
              this.isDeleting.set(false);
              this.notification.showSuccess(this.translate.instant('projects.messages.deleted'));
              this.router.navigate(['/projects']);
            },
            error: (error) => {
              this.isDeleting.set(false);
              this.notification.showError(error);
            },
          });
      }
    );
  }

  onRetryLoadProject(): void {
    if (!this.currentProjectId) {
      return;
    }

    this.loadProject(this.currentProjectId);
    this.loadQuestionnaires(this.questionnairesState().pagination.currentPage || 1);
  }

  getProjectStatusLabel(status: string | undefined | null): string {
    if (!status) {
      return '---';
    }

    if (this.projectStatusLabelMap[status]) {
      return this.projectStatusLabelMap[status];
    }

    return status;
  }

  getFormattedProjectCode(project: Project | null): string {
    if (!project?.id) {
      return '';
    }

    return `${this.translate.instant('projects.detail.code_prefix')} ${String(project.id).padStart(3, '0')}`;
  }

  getQuestionnaireProgress(questionnaire: ProjectQuestionnaireSummary): number {
    if (!questionnaire.totalRespondents) {
      return 0;
    }

    return Math.round(
      (questionnaire.respondedRespondents / questionnaire.totalRespondents) * 100
    );
  }

  isQuestionnairePinned(questionnaire: ProjectQuestionnaireSummary): boolean {
    const isProgressInProgress =
      questionnaire.progressStatus === QuestionnaireResponseStatus.InProgress;

    const normalizedStatus = questionnaire.status ?? '';
    const isTimelineInProgress =
      normalizedStatus === TimelineStatus.EmAndamento || normalizedStatus === 'IN_PROGRESS';

    return isProgressInProgress || isTimelineInProgress;
  }

  isQuestionnaireInProgress(questionnaire: ProjectQuestionnaireSummary): boolean {
    return this.isQuestionnairePinned(questionnaire);
  }

  getQuestionnaireReferenceLabel(
    questionnaire: ProjectQuestionnaireSummary,
    projectType: ProjectType | undefined
  ): string {
    if (projectType === ProjectType.Cascata) {
      return questionnaire.stageName || this.translate.instant('projects.detail.not_defined');
    }

    if (projectType === ProjectType.Iterativo) {
      return questionnaire.iterationName || this.translate.instant('projects.detail.not_defined');
    }

    return questionnaire.stageName || questionnaire.iterationName || this.translate.instant('projects.detail.no_reference');
  }

  getReferenceLabelTitle(projectType: ProjectType | undefined): string {
    if (projectType === ProjectType.Cascata) {
      return this.translate.instant('projects.detail.reference_stage');
    }

    if (projectType === ProjectType.Iterativo) {
      return this.translate.instant('projects.detail.reference_iteration');
    }

    return this.translate.instant('projects.detail.reference_other');
  }

  getApplicationRange(questionnaire: ProjectQuestionnaireSummary): string {
    const start = this.formatDate(questionnaire.applicationStartDate);
    const end = this.formatDate(questionnaire.applicationEndDate);

    if (!start && !end) {
      return this.translate.instant('projects.detail.no_period');
    }

    if (start && end) {
      return this.translate.instant('projects.detail.date_range', { start, end });
    }

    return start
      ? this.translate.instant('projects.detail.from_date', { date: start })
      : this.translate.instant('projects.detail.until_date', { date: end });
  }

  isQuestionnaireCompleted(questionnaire: ProjectQuestionnaireSummary): boolean {
    const status = (questionnaire.status ?? '').toString().toUpperCase();
    return status === TimelineStatus.Concluido || status === 'COMPLETED';
  }

  navigateToProjectDashboard(): void {
    const projectId = this.currentProjectId ?? this.projectState().data?.id;
    if (!projectId) {
      return;
    }
    void this.router.navigate(['/projects', projectId, 'dashboard']);
  }

  navigateToQuestionnaireDashboard(questionnaire: ProjectQuestionnaireSummary): void {
    const projectId = this.currentProjectId ?? this.projectState().data?.id;
    if (!projectId) {
      return;
    }
    void this.router.navigate([
      '/projects',
      projectId,
      'questionnaires',
      questionnaire.id,
      'dashboard',
    ]);
  }

  navigateToIndividualDashboard(questionnaire: ProjectQuestionnaireSummary): void {
    const projectId = this.currentProjectId ?? this.projectState().data?.id;
    if (!projectId) {
      return;
    }
    void this.router.navigate([
      '/projects',
      projectId,
      'questionnaires',
      questionnaire.id,
      'dashboard',
      'individual',
    ]);
  }

  canForceCloseQuestionnaire(questionnaire: ProjectQuestionnaireSummary): boolean {
    return (
      this.isAdmin() &&
      !this.isQuestionnaireCompleted(questionnaire) &&
      questionnaire.pendingRespondents === 0 &&
      questionnaire.totalRespondents > 0
    );
  }

  canRescheduleQuestionnaire(questionnaire: ProjectQuestionnaireSummary): boolean {
    if (!this.isAdmin()) {
      return false;
    }

    if (this.normalizeStatus(questionnaire.status) !== TimelineStatus.Pendente) {
      return false;
    }

    const items = this.questionnairesState().items;
    if (!items.some((q) => this.isQuestionnaireCompleted(q))) {
      return false;
    }

    const firstPending = this.getFirstPendingQuestionnaire(items);
    return firstPending?.id === questionnaire.id;
  }

  isRescheduling(questionnaireId: number): boolean {
    return this.reschedulingIds().has(questionnaireId);
  }

  onRescheduleQuestionnaire(questionnaire: ProjectQuestionnaireSummary): void {
    if (!this.currentProjectId || !this.isAdmin()) {
      return;
    }

    const project = this.projectState().data;
    const startDate = questionnaire.applicationStartDate
      ? this.toISODate(questionnaire.applicationStartDate)
      : null;
    const endDate = questionnaire.applicationEndDate
      ? this.toISODate(questionnaire.applicationEndDate)
      : null;
    const projectStartDate = project?.startDate
      ? this.toISODate(project.startDate)
      : null;

    this.modalService.open(RescheduleQuestionnaireModalComponent, 'small-card', {
      questionnaireName: questionnaire.name,
      currentStartDate: startDate,
      currentEndDate: endDate,
      projectStartDate: projectStartDate,
    });

    const modalInstance = this.modalService.getActiveInstance<RescheduleQuestionnaireModalComponent>();
    if (!modalInstance) {
      return;
    }

    modalInstance.confirmed.pipe(take(1)).subscribe((payload: RescheduleQuestionnairePayload) => {
      this.modalService.close();
      this.executeReschedule(questionnaire, payload);
    });
  }

  private executeReschedule(
    questionnaire: ProjectQuestionnaireSummary,
    payload: RescheduleQuestionnairePayload
  ): void {
    if (!this.currentProjectId) {
      return;
    }

    this.reschedulingIds.update((ids) => {
      const next = new Set(ids);
      next.add(questionnaire.id);
      return next;
    });

    this.projectStore
      .rescheduleQuestionnaire(this.currentProjectId, questionnaire.id, payload)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          this.removeReschedulingId(questionnaire.id);

          const startFormatted = this.formatDate(response.newStartDate);
          const endFormatted = this.formatDate(response.newEndDate);
          this.notification.showSuccess(
            this.translate.instant('projects.messages.reschedule_success', { name: response.questionnaireName, start: startFormatted, end: endFormatted })
          );

          if (response.projectDeadlineExceeded && response.projectDeadlineWarning) {
            setTimeout(() => {
              this.notification.showWarning(response.projectDeadlineWarning!);
            }, 500);
          }

          this.loadQuestionnaires(
            this.questionnairesState().pagination.currentPage || 1
          );
          this.loadProject(this.currentProjectId!);
        },
        error: (error: unknown) => {
          this.removeReschedulingId(questionnaire.id);
          this.notification.showError(error ?? this.translate.instant('projects.messages.reschedule_error'));
        },
      });
  }

  private removeReschedulingId(questionnaireId: number): void {
    this.reschedulingIds.update((ids) => {
      const next = new Set(ids);
      next.delete(questionnaireId);
      return next;
    });
  }

  private toISODate(date: NullableDateLike): string | null {
    if (!date) {
      return null;
    }
    const parsed = BusinessDaysUtils.parseISODate(date);
    return Number.isNaN(parsed.getTime()) ? null : BusinessDaysUtils.formatDateISO(parsed);
  }

  private normalizeStatus(status: string | null | undefined): string {
    return (status ?? '').toString().toUpperCase();
  }

  private getFirstPendingQuestionnaire(
    items: ProjectQuestionnaireSummary[]
  ): ProjectQuestionnaireSummary | undefined {
    return items
      .filter((q) => this.normalizeStatus(q.status) === TimelineStatus.Pendente)
      .sort((a, b) => {
        const aStart = a.applicationStartDate ? new Date(a.applicationStartDate).getTime() : Infinity;
        const bStart = b.applicationStartDate ? new Date(b.applicationStartDate).getTime() : Infinity;
        return aStart - bStart;
      })[0];
  }

  isForceClosing(questionnaireId: number): boolean {
    return this.forceClosingIds().has(questionnaireId);
  }

  onForceCloseQuestionnaire(questionnaire: ProjectQuestionnaireSummary): void {
    if (!this.currentProjectId) return;

    this.notification.showConfirm(
      `Deseja encerrar o questionário "${questionnaire.name}" e calcular o ISEP com as respostas existentes?`,
      () => this.executeForceClose(questionnaire)
    );
  }

  private executeForceClose(questionnaire: ProjectQuestionnaireSummary): void {
    if (!this.currentProjectId) return;

    this.forceClosingIds.update((ids) => {
      const next = new Set(ids);
      next.add(questionnaire.id);
      return next;
    });

    this.dashboardService
      .forceCloseQuestionnaire(Number(this.currentProjectId), questionnaire.id)
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.removeForceClosingId(questionnaire.id);
          this.notification.showSuccess(
            this.translate.instant('projects.messages.questionnaire_closed', { name: questionnaire.name })
          );
          this.loadQuestionnaires(
            this.questionnairesState().pagination.currentPage || 1
          );
        },
        error: () => {
          this.removeForceClosingId(questionnaire.id);
          this.notification.showError(
            this.translate.instant('projects.messages.questionnaire_close_error')
          );
        },
      });
  }

  private removeForceClosingId(questionnaireId: number): void {
    this.forceClosingIds.update((ids) => {
      const next = new Set(ids);
      next.delete(questionnaireId);
      return next;
    });
  }

  navigateToQuestionnaire(
    questionnaire: ProjectQuestionnaireSummary,
    mode: QuestionnaireActionMode = 'respond'
  ): void {
    const projectId = this.currentProjectId ?? this.projectState().data?.id;

    if (!projectId) {
      return;
    }

    void this.router.navigate([
      '/projects',
      projectId,
      'questionnaires',
      questionnaire.id,
    ], {
      queryParams: { mode },
    });
  }

  navigateToQuestionnaireView(questionnaire: ProjectQuestionnaireSummary): void {
    const projectId = this.currentProjectId ?? this.projectState().data?.id;

    if (!projectId) {
      return;
    }

    const viewUrl = this.router.createUrlTree([
      '/projects',
      projectId,
      'questionnaires',
      questionnaire.id,
      'view',
    ]);

    void this.router.navigateByUrl(viewUrl);
  }

  canDisplayRepresentativeActions(questionnaire: ProjectQuestionnaireSummary): boolean {
    return this.isAdmin() || this.isCurrentUserRespondent(questionnaire);
  }

  canCurrentUserRespond(questionnaire: ProjectQuestionnaireSummary): boolean {
    if (!this.isQuestionnaireOpenForResponse(questionnaire)) {
      return false;
    }

    if (!this.canDisplayRepresentativeActions(questionnaire)) {
      return false;
    }

    const respondent = this.getCurrentRespondent(questionnaire);
    if (!respondent) {
      return false;
    }

    return (
      respondent.status === QuestionnaireResponseStatus.Pending ||
      respondent.status === QuestionnaireResponseStatus.InProgress
    );
  }

  private isQuestionnaireOpenForResponse(questionnaire: ProjectQuestionnaireSummary): boolean {
    const normalizedStatus = (questionnaire.status ?? '').toString().toUpperCase();

    return (
      normalizedStatus === TimelineStatus.EmAndamento ||
      normalizedStatus === TimelineStatus.Atrasado ||
      normalizedStatus === 'IN_PROGRESS' ||
      normalizedStatus === 'OVERDUE' ||
      normalizedStatus === 'LATE'
    );
  }

  canCurrentUserView(questionnaire: ProjectQuestionnaireSummary): boolean {
    const respondent = this.getCurrentRespondent(questionnaire);
    if (!respondent) {
      return false;
    }

    return respondent.status === QuestionnaireResponseStatus.Completed;
  }

  shouldDisplayQuestionnaireActions(questionnaire: ProjectQuestionnaireSummary): boolean {
    if (this.isAdmin()) {
      return true;
    }

    return (
      this.canCurrentUserRespond(questionnaire) ||
      this.canCurrentUserView(questionnaire)
    );
  }

  getParticipantActionMessage(questionnaire: ProjectQuestionnaireSummary): string {
    if (this.isAdmin()) {
      return this.translate.instant('projects.detail.view_admin_mode');
    }

    const respondent = this.getCurrentRespondent(questionnaire);
    if (!respondent) {
      return '';
    }

    if (
      respondent.status === QuestionnaireResponseStatus.Pending ||
      respondent.status === QuestionnaireResponseStatus.InProgress
    ) {
      return this.translate.instant('projects.detail.view_representative_mode');
    }

    if (respondent.status === QuestionnaireResponseStatus.Completed) {
      return this.translate.instant('projects.detail.view_completed_mode');
    }

    return '';
  }

  private isCurrentUserRespondent(questionnaire: ProjectQuestionnaireSummary): boolean {
    return !!this.getCurrentRespondent(questionnaire);
  }

  private getCurrentRespondent(
    questionnaire: ProjectQuestionnaireSummary
  ): QuestionnaireRespondentStatus | undefined {
    const currentEmail = this.currentUser()?.email?.toLowerCase();

    if (!currentEmail) {
      return undefined;
    }

    return questionnaire.respondents.find(
      (respondent) => respondent.email?.toLowerCase() === currentEmail
    );
  }

  canDisplayRespondents(): boolean {
    return this.isAdmin();
  }

  isRespondentListExpanded(questionnaireId: number): boolean {
    return this.expandedRespondentLists().has(questionnaireId);
  }

  toggleRespondentList(questionnaireId: number): void {
    this.expandedRespondentLists.update((ids) => {
      const next = new Set(ids);
      if (next.has(questionnaireId)) {
        next.delete(questionnaireId);
      } else {
        next.add(questionnaireId);
      }
      return next;
    });
  }

  copyQuestionnaireLink(questionnaire: ProjectQuestionnaireSummary): void {
    if (!this.isAdmin()) {
      this.notification.showWarning(this.translate.instant('projects.messages.copy_link_admin_only'));
      return;
    }

    const link = this.getQuestionnairePublicLink(questionnaire);
    this.copyToClipboard(link, this.translate.instant('projects.messages.link_copied'));
  }

  getTimelineStatusLabel(status: TimelineStatus | string | null | undefined): string {
    if (!status) {
      return this.translate.instant('questionnaire.timeline_status.no_status');
    }

    const normalizedStatus = status as TimelineStatus;
    return this.timelineStatusLabelMap[normalizedStatus] ?? status;
  }

  getTimelineStatusClass(status: TimelineStatus | string | null | undefined): string {
    switch (status) {
      case TimelineStatus.Concluido:
        return 'status-chip--success';
      case TimelineStatus.EmAndamento:
        return 'status-chip--warning';
      case TimelineStatus.Atrasado:
        return 'status-chip--error';
      case TimelineStatus.Pendente:
      default:
        return 'status-chip--neutral';
    }
  }

  getRespondentStatusLabel(
    status: QuestionnaireResponseStatus | null | undefined
  ): string {
    if (!status) {
      return this.translate.instant('questionnaire.respondent_status.pending');
    }

    return this.respondentStatusLabelMap[status] ?? status;
  }

  getRespondentStatusClass(
    status: QuestionnaireResponseStatus | null | undefined
  ): string {
    switch (status) {
      case QuestionnaireResponseStatus.Completed:
        return 'status-chip--success';
      case QuestionnaireResponseStatus.InProgress:
        return 'status-chip--warning';
      default:
        return 'status-chip--neutral';
    }
  }

  trackByQuestionnaireId(
    _index: number,
    questionnaire: ProjectQuestionnaireSummary
  ): number {
    return questionnaire.id;
  }

  trackByRespondentId(
    _index: number,
    respondent: QuestionnaireRespondentStatus
  ): number {
    return respondent.representativeId;
  }

  get hasQuestionnaires(): boolean {
    return this.questionnairesState().pagination.totalItems > 0;
  }

  get isCascata(): boolean {
    return this.projectState().data?.type === ProjectType.Cascata;
  }

  get isIterativo(): boolean {
    return this.projectState().data?.type === ProjectType.Iterativo;
  }

  getProjectSituation(project: Project | null): string {
    if (!project) {
      return '---';
    }

    if (project.type === ProjectType.Iterativo) {
      const totalIterations = project.iterationCount ?? project.configuredIterationCount;
      if (project.currentIteration && totalIterations) {
        return `Sprint ${project.currentIteration}/${totalIterations}`;
      }

      if (project.currentSituation) {
        return project.currentSituation;
      }

      return project.currentStage || '---';
    }

    if (project.type === ProjectType.Cascata) {
      if (project.currentSituation) {
        return project.currentSituation;
      }

      if (project.currentStage) {
        return project.currentStage;
      }

      return '---';
    }

    return '---';
  }

  isReminderInProgress(questionnaireId: number): boolean {
    return this.sendingReminderIds().has(questionnaireId);
  }

  onSendReminder(questionnaire: ProjectQuestionnaireSummary): void {
    if (!this.isAdmin() || !this.currentProjectId) {
      return;
    }

    if (!this.isQuestionnaireInProgress(questionnaire)) {
      this.notification.showWarning(this.translate.instant('questionnaire.messages.reminder_in_progress_only'));
      return;
    }

    const pendingEmails = this.getPendingRespondentEmails(questionnaire);
    if (!pendingEmails.length) {
      this.notification.showWarning(this.translate.instant('questionnaire.messages.no_pending_emails'));
      return;
    }

    const reference = this.getQuestionnaireReferenceLabel(
      questionnaire,
      this.projectState().data?.type
    );
    const message = this.buildReminderConfirmationMessage(
      questionnaire,
      pendingEmails,
      reference
    );

    this.notification.showConfirm(message, () =>
      this.executeReminderRequest(questionnaire, pendingEmails)
    );
  }

  private executeReminderRequest(
    questionnaire: ProjectQuestionnaireSummary,
    emails: string[]
  ): void {
    if (!this.currentProjectId) {
      return;
    }

    this.sendingReminderIds.update((current) => {
      const next = new Set(current);
      next.add(questionnaire.id);
      return next;
    });

    this.projectStore
      .sendQuestionnaireReminder(this.currentProjectId, questionnaire.id, { emails })
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.removeReminderLoading(questionnaire.id);
          this.notification.showSuccess(this.translate.instant('questionnaire.messages.reminder_sent'));
        },
        error: (error: unknown) => {
          this.removeReminderLoading(questionnaire.id);
          this.notification.showError(error ?? this.translate.instant('questionnaire.messages.reminder_error'));
        },
      });
  }

  private removeReminderLoading(questionnaireId: number): void {
    this.sendingReminderIds.update((current) => {
      const next = new Set(current);
      next.delete(questionnaireId);
      return next;
    });
  }

  private getPendingRespondentEmails(
    questionnaire: ProjectQuestionnaireSummary
  ): string[] {
    const respondents = questionnaire.respondents ?? [];

    const emails = respondents
      .filter((respondent) =>
        respondent && respondent.email && respondent.status !== QuestionnaireResponseStatus.Completed
      )
      .map((respondent) => respondent.email.trim())
      .filter((email) => !!email);

    return Array.from(new Set(emails));
  }

  private buildReminderConfirmationMessage(
    questionnaire: ProjectQuestionnaireSummary,
    emails: string[],
    reference: string
  ): string {
    const total = emails.length;
    const plural = total > 1 ? 's' : '';
    const previewLimit = 5;
    const previewList = emails.slice(0, previewLimit).join(', ');
    const remaining = total - previewLimit;
    const remainingText = remaining > 0 ? ` e outros ${remaining}` : '';

    return `Será enviado um e-mail de lembrete para ${total} representante${plural} pendente${plural} do questionário "${questionnaire.name}" (${reference}). Destinatários: ${previewList}${remainingText}. Deseja continuar?`;
  }

  private listenToUserRoles(): void {
    this.authService.userRoles$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((roles) => this.userRoles.set(roles ?? []));

    this.authService.currentUser$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((user) => this.currentUser.set(user));
  }

  private listenToRouteChanges(): void {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id')),
        filter((id): id is string => !!id),
        distinctUntilChanged(),
        tap((id) => {
          this.currentProjectId = id;
          this.projectContext.setCurrentProjectId(id);
          this.filterForm.reset({ name: '', stage: '', iteration: '' });
          this.loadProject(id);
          this.loadQuestionnaires(1);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadProject(projectId: string): void {
    this.projectState.set({ data: null, status: 'loading', error: null });

    this.projectStore
      .getProjectById(projectId)
      .pipe(take(1))
      .subscribe({
        next: (project) => {
          this.projectState.set({ data: project, status: 'loaded', error: null });
          this.loadCurrentUserRoles(projectId);
        },
        error: (error: unknown) => {
          const message =
            error && typeof error === 'object' && 'message' in error
              ? (error as { message?: string }).message ?? null
              : null;
          this.projectState.set({
            data: null,
            status: 'error',
            error: message ?? this.translate.instant('projects.messages.error_loading_project'),
          });
        },
      });
  }

  private loadCurrentUserRoles(projectId: string): void {
    const user = this.currentUser();
    if (!user?.email) {
      this.currentUserProjectRoles.set([]);
      return;
    }

    forkJoin([
      this.projectStore.getProjectForEdit(projectId).pipe(take(1)),
      this.roleService.getRoles().pipe(take(1)),
    ]).subscribe({
      next: ([editData, roles]) => {
        const userEmail = user.email.toLowerCase();
        const representative = editData.representatives.find(
          (rep) => rep.email?.toLowerCase() === userEmail
        );

        if (!representative || !representative.roleIds?.length) {
          this.currentUserProjectRoles.set([]);
          return;
        }

        const roleMap = new Map(roles.map((r) => [r.id, r.name]));
        const resolvedNames = representative.roleIds
          .map((id) => roleMap.get(id))
          .filter((name): name is string => !!name);

        this.currentUserProjectRoles.set(
          resolvedNames.length > 0
            ? resolvedNames
            : representative.roleNames ?? []
        );
      },
      error: () => {
        this.currentUserProjectRoles.set([]);
      },
    });
  }

  private loadQuestionnaires(page: number): void {
    if (!this.currentProjectId) {
      return;
    }

    const filters = this.getQuestionnaireFilters(page);

    this.questionnairesState.update((state) => ({
      ...state,
      status: 'loading',
      error: null,
    }));

    this.projectStore
      .getProjectQuestionnaires(this.currentProjectId, filters)
      .pipe(take(1))
      .subscribe({
        next: (result: Page<ProjectQuestionnaireSummary>) => {
          console.debug('[ProjectDetailPage] getProjectQuestionnaires result', result);
          const normalizedItems = (result.content ?? []).map((item) => {
            const applicationStartDate = item.applicationStartDate
              ? new Date(item.applicationStartDate as unknown as string)
              : null;
            const applicationEndDate = item.applicationEndDate
              ? new Date(item.applicationEndDate as unknown as string)
              : null;
            const lastResponseAt = item.lastResponseAt ? new Date(item.lastResponseAt as unknown as string) : null;

            return {
              ...item,
              applicationStartDate: Number.isNaN(applicationStartDate?.getTime?.()) ? item.applicationStartDate : applicationStartDate,
              applicationEndDate: Number.isNaN(applicationEndDate?.getTime?.()) ? item.applicationEndDate : applicationEndDate,
              lastResponseAt: Number.isNaN(lastResponseAt?.getTime?.()) ? item.lastResponseAt : lastResponseAt,
            };
          });

          const sortedItems = this.sortQuestionnaires(normalizedItems);
          console.debug('[ProjectDetailPage] sortedItems', sortedItems);
          this.questionnairesState.set({
            items: sortedItems,
            status: 'loaded',
            error: null,
            pagination: {
              currentPage: result.pageable.pageNumber + 1,
              totalItems: result.totalElements,
              pageSize: result.size,
            },
          });
        },
        error: (error: unknown) => {
          const message =
            error && typeof error === 'object' && 'message' in error
              ? (error as { message?: string }).message ?? null
              : null;
          this.questionnairesState.update((state) => ({
            ...state,
            status: 'error',
            error: message ?? this.translate.instant('projects.messages.error_loading_questionnaires'),
          }));
        },
      });
  }

  private getQuestionnaireFilters(page: number): ProjectQuestionnaireFilters {
    const rawValue = this.filterForm.getRawValue();
    const name = rawValue.name?.trim();
    const stage = rawValue.stage?.trim();
    const iteration = rawValue.iteration?.trim();
    const projectType = this.projectState().data?.type;

    return {
      name: name || null,
      stage: projectType === ProjectType.Cascata ? stage || null : null,
      iteration: projectType === ProjectType.Iterativo ? iteration || null : null,
      status: null,
      page: Math.max(page - 1, 0),
      size: this.questionnairesPageSize,
    };
  }

  private sortQuestionnaires(
    questionnaires: ProjectQuestionnaireSummary[]
  ): ProjectQuestionnaireSummary[] {
    return [...(questionnaires ?? [])].sort((a, b) => {
      const pinDiff = Number(this.isQuestionnairePinned(b)) - Number(this.isQuestionnairePinned(a));
      if (pinDiff !== 0) {
        return pinDiff;
      }

      const startDiff = this.compareDates(a.applicationStartDate, b.applicationStartDate);
      if (startDiff !== 0) {
        return startDiff;
      }

      return (b.lastResponseAt ? new Date(b.lastResponseAt).getTime() : 0) -
        (a.lastResponseAt ? new Date(a.lastResponseAt).getTime() : 0);
    });
  }

  private compareDates(
    first: NullableDateLike,
    second: NullableDateLike
  ): number {
    const firstTime = this.getDateValue(first);
    const secondTime = this.getDateValue(second);

    if (firstTime === null && secondTime === null) {
      return 0;
    }

    if (firstTime === null) {
      return 1;
    }

    if (secondTime === null) {
      return -1;
    }

    if (firstTime === secondTime) {
      return 0;
    }

    return firstTime - secondTime;
  }

  private getDateValue(date: string | Date | null | undefined): number | null {
    if (!date) {
      return null;
    }

    const normalized = new Date(date);
    return Number.isNaN(normalized.getTime()) ? null : normalized.getTime();
  }

  private formatDate(date: string | Date | null | undefined): string | null {
    if (!date) {
      return null;
    }

    const normalized = new Date(date);
    if (Number.isNaN(normalized.getTime())) {
      return null;
    }

    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(normalized);
  }

  private getQuestionnairePublicLink(questionnaire: ProjectQuestionnaireSummary): string {
    const sanitizedDomain = environment.domain?.replace(/\/$/, '') ?? '';
    const fallback = globalThis.window?.location?.origin ?? '';
    const base = sanitizedDomain || fallback;
    return `${base}/projects/${this.currentProjectId}/questionnaires/${questionnaire.id}`;
  }

  private copyToClipboard(value: string, successMessage: string): void {
    if (!value) {
      this.notification.showWarning(this.translate.instant('notifications.project_detail.nothing_to_copy'));
      return;
    }

    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(value).then(
        () => this.notification.showSuccess(successMessage),
        () => this.fallbackCopy(value, successMessage)
      );
      return;
    }

    this.fallbackCopy(value, successMessage);
  }

  private fallbackCopy(value: string, successMessage: string): void {
    const result = globalThis.prompt(this.translate.instant('common.copy_prompt'), value);
    if (result !== null) {
      this.notification.showSuccess(successMessage);
    }
  }
}
