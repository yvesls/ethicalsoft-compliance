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
import { take } from 'rxjs';

import { FilterBarComponent } from '../../../../shared/components/filter-bar/filter-bar.component';
import { InputComponent } from '../../../../shared/components/input/input.component';
import { ListComponent } from '../../../../shared/components/list/list.component';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { ProjectStore } from '../../../../shared/stores/project.store';
import { Project } from '../../../../shared/interfaces/project/project.interface';
import {
  ProjectQuestionnaireFilters,
  ProjectQuestionnaireSummary,
  QuestionnaireRespondentStatus,
} from '../../../../shared/interfaces/project/project-questionnaire.interface';
import { ProjectType } from '../../../../shared/enums/project-type.enum';
import { ProjectStatus } from '../../../../shared/enums/project-status.enum';
import { TimelineStatus } from '../../../../shared/enums/timeline-status.enum';
import { Page } from '../../../../shared/interfaces/pageable.interface';
import { AuthenticationService } from '../../../../core/services/authentication.service';
import { RoleEnum } from '../../../../shared/enums/role.enum';
import { ProjectContextService } from '../../../../core/services/project-context.service';
import { QuestionnaireResponseStatus } from '../../../../shared/enums/questionnaire-response-status.enum';

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

  private readonly questionnairesPageSize = 5;
  private currentProjectId: string | null = null;

  private readonly userRoles = signal<string[]>([]);
  readonly isAdmin = computed(() =>
    this.userRoles().includes(RoleEnum.ADMIN)
  );

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

  public ProjectTypeEnum = ProjectType;

  readonly projectTypeLabelMap: Record<ProjectType, string> = {
    [ProjectType.Cascata]: 'Cascata',
    [ProjectType.Iterativo]: 'Iterativo',
  };

  private readonly projectStatusLabelMap: Record<string, string> = {
    ABERTO: ProjectStatus.Aberto,
    RASCUNHO: ProjectStatus.Rascunho,
    CONCLUIDO: ProjectStatus.Concluido,
    ARQUIVADO: ProjectStatus.Arquivado,
  };

  private readonly timelineStatusLabelMap: Record<TimelineStatus, string> = {
    [TimelineStatus.Pendente]: 'Pendente',
    [TimelineStatus.EmAndamento]: 'Em andamento',
    [TimelineStatus.Concluido]: 'Concluído',
    [TimelineStatus.Atrasado]: 'Atrasado',
  };

  private readonly respondentStatusLabelMap: Record<
    QuestionnaireResponseStatus,
    string
  > = {
    [QuestionnaireResponseStatus.Pending]: 'Pendente',
    [QuestionnaireResponseStatus.InProgress]: 'Em andamento',
    [QuestionnaireResponseStatus.Completed]: 'Concluído',
  };

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

    this.router.navigate(['/projects/create'], {
      queryParams: { type: project.type, projectId: project.id },
    });
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

    return `Cod ${String(project.id).padStart(3, '0')}`;
  }

  getQuestionnaireProgress(questionnaire: ProjectQuestionnaireSummary): number {
    if (!questionnaire.totalRespondents) {
      return 0;
    }

    return Math.round(
      (questionnaire.respondedRespondents / questionnaire.totalRespondents) * 100
    );
  }

  getTimelineStatusLabel(status: TimelineStatus | string | null | undefined): string {
    if (!status) {
      return 'Sem status';
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
        return 'status-chip--neutral';
      case TimelineStatus.Pendente:
      default:
        return 'status-chip--neutral';
    }
  }

  getRespondentStatusLabel(
    status: QuestionnaireResponseStatus | null | undefined
  ): string {
    if (!status) {
      return 'Pendente';
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

    if (project.currentSituation) {
      return project.currentSituation;
    }

    if (
      project.type === ProjectType.Iterativo &&
      project.currentIteration &&
      project.iterationCount
    ) {
      return `Sprint ${project.currentIteration}/${project.iterationCount}`;
    }

    if (project.type === ProjectType.Cascata && project.currentStage) {
      return project.currentStage;
    }

    return '---';
  }

  private listenToUserRoles(): void {
    this.authService.userRoles$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((roles) => this.userRoles.set(roles ?? []));
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
        },
        error: (error: unknown) => {
          const message =
            error && typeof error === 'object' && 'message' in error
              ? (error as { message?: string }).message ?? null
              : null;
          this.projectState.set({
            data: null,
            status: 'error',
            error: message ?? 'Falha ao carregar o projeto.',
          });
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
          this.questionnairesState.set({
            items: result.content,
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
            error: message ?? 'Falha ao carregar os questionários.',
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
}
