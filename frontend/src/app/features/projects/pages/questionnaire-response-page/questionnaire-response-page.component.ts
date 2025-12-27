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
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { take } from 'rxjs';

import { PaginationComponent } from '../../../../shared/components/pagination/pagination.component';
import { QuestionnaireResponseService } from '../../services/questionnaire-response.service';
import { QuestionnaireResponseStatus } from '../../../../shared/enums/questionnaire-response-status.enum';
import {
  QuestionnaireAnswerDocument,
  QuestionnaireAttachmentLink,
  QuestionnaireResponsePayload,
  QuestionnaireResponseSubmission,
} from '../../../../shared/interfaces/questionnaire/questionnaire-response.interface';
import { AttachmentModalValue, QuestionnaireAttachmentModalComponent } from '../../components/questionnaire-attachment-modal/questionnaire-attachment-modal.component';
import { QuestionnaireRespondentStatus } from '../../../../shared/interfaces/project/project-questionnaire.interface';
import { ModalService } from '../../../../core/services/modal.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ProjectContextService } from '../../../../core/services/project-context.service';
import { AuthenticationService, UserInterface } from '../../../../core/services/authentication.service';
import { RoleEnum } from '../../../../shared/enums/role.enum';

interface QuestionnaireResponseState {
  status: 'loading' | 'loaded' | 'error';
  error: string | null;
  data: QuestionnaireResponsePayload | null;
}

type PageMode = 'respond' | 'view';

@Component({
  selector: 'app-questionnaire-response-page',
  standalone: true,
  imports: [CommonModule, PaginationComponent],
  templateUrl: './questionnaire-response-page.component.html',
  styleUrls: ['./questionnaire-response-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionnaireResponsePageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly responseService = inject(QuestionnaireResponseService);
  private readonly modalService = inject(ModalService);
  private readonly notification = inject(NotificationService);
  private readonly projectContext = inject(ProjectContextService);
  private readonly authService = inject(AuthenticationService);
  private readonly destroyRef = inject(DestroyRef);

  private projectId: string | null = null;
  private questionnaireId: number | null = null;
  private requestedMode: PageMode = 'respond';

  private readonly userRoles = signal<string[]>([]);
  private readonly currentUser = signal<UserInterface | null>(null);
  readonly isAdmin = computed(() => this.userRoles().includes(RoleEnum.ADMIN));

  readonly state = signal<QuestionnaireResponseState>({
    status: 'loading',
    error: null,
    data: null,
  });

  readonly pageMode = signal<PageMode>('respond');
  readonly currentPage = signal(1);
  pageSize = 10;

  readonly questionnaire = computed(() => this.state().data?.questionnaire ?? null);
  readonly answers = computed(() => this.state().data?.response.answers ?? []);
  readonly pagination = computed(() => this.state().data?.pagination ?? null);
  readonly totalQuestions = computed(() => this.pagination()?.totalElements ?? 0);
  readonly answeredCount = computed(() =>
    this.answers().filter((answer) => answer.response !== null).length
  );

  readonly progressPercentage = computed(() => {
    if (!this.answers().length) {
      return 0;
    }
    return Math.round((this.answeredCount() / this.answers().length) * 100);
  });

  ngOnInit(): void {
    this.listenToAuthState();
    this.listenToRoute();
  }

  onNavigateBack(): void {
    if (this.projectId) {
      this.router.navigate(['/projects', this.projectId]);
      return;
    }
    this.router.navigate(['/projects']);
  }

  onPageChange(page: number): void {
    this.loadResponse(page);
  }

  onSelectAnswer(answer: QuestionnaireAnswerDocument, value: boolean): void {
    if (this.pageMode() === 'view') {
      return;
    }

    this.updateAnswer(answer.questionId, {
      response: value,
      justification: value ? null : answer.justification ?? null,
      evidence: value ? answer.evidence ?? null : null,
    });
  }

  openAttachmentModal(answer: QuestionnaireAnswerDocument): void {
    if (answer.response === null) {
      this.notification.showWarning('Selecione "Sim" ou "Não" antes de anexar evidências.');
      return;
    }

    const initialValue: AttachmentModalValue = {
      note: (answer.response ? answer.evidence?.descricao : answer.justification?.descricao) ?? '',
      attachments: answer.attachments ?? [],
    };

    this.modalService.open(QuestionnaireAttachmentModalComponent, 'medium-card', {
      mode: answer.response ? 'positive' : 'negative',
      initialValue,
      onSave: (value: AttachmentModalValue) =>
        this.updateAnswer(answer.questionId, {
          justification: answer.response ? null : this.buildPrimaryNoteLink(value.note),
          evidence: answer.response ? this.buildPrimaryNoteLink(value.note) : null,
          attachments: value.attachments,
        }),
    });
  }

  submitResponses(): void {
    if (!this.canSubmit()) {
      return;
    }

    const currentAnswers = this.answers();
    const pagination = this.pagination();
    if (!pagination) {
      this.notification.showError('Não foi possível determinar a página atual.');
      return;
    }

    const submission: QuestionnaireResponseSubmission = {
      status: QuestionnaireResponseStatus.Completed,
      answers: currentAnswers,
    };

    if (!this.projectId || this.questionnaireId === null) {
      this.notification.showError('Projeto ou questionário inválido.');
      return;
    }

    this.responseService
      .submitPage(
        this.projectId,
        this.questionnaireId,
        submission,
        { pageNumber: pagination.pageNumber, pageSize: pagination.pageSize },
        this.currentUser()?.email
      )
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.notification.showSuccess('Página enviada com sucesso.');
          const paginationData = this.pagination();
          const hasNextPage = paginationData && this.currentPage() < paginationData.totalPages;
          const nextPage = hasNextPage ? this.currentPage() + 1 : this.currentPage();
          this.loadResponse(nextPage);
        },
        error: (msg) => this.notification.showError(msg),
      });
  }

  getQuestionOrdinal(index: number): number {
    return (this.currentPage() - 1) * this.pageSize + index + 1;
  }

  getRespondentStatusLabel(): string {
    const respondent = this.getCurrentRespondent();
    if (!respondent) {
      return this.isAdmin() ? 'Administrador' : 'Não associado';
    }

    switch (respondent.status) {
      case QuestionnaireResponseStatus.Completed:
        return 'Respondido';
      case QuestionnaireResponseStatus.InProgress:
        return 'Em andamento';
      default:
        return 'Pendente';
    }
  }

  canSubmit(): boolean {
    if (this.pageMode() !== 'respond') {
      return false;
    }

    if (!this.answers().length) {
      return false;
    }

    if (this.pagination()?.completed) {
      return false;
    }

    return this.answers().every((answer) => answer.response !== null);
  }

  get referenceLabel(): string {
    const questionnaire = this.questionnaire();
    if (!questionnaire) {
      return '';
    }

    if (questionnaire.stageName) {
      return `Etapa: ${questionnaire.stageName}`;
    }

    if (questionnaire.iterationName) {
      return `Iteração: ${questionnaire.iterationName}`;
    }

    return '';
  }

  get applicationRange(): string {
    const questionnaire = this.questionnaire();
    if (!questionnaire) {
      return '';
    }

    const start = questionnaire.applicationStartDate
      ? new Date(questionnaire.applicationStartDate).toLocaleDateString('pt-BR')
      : 'Sem data inicial';
    const end = questionnaire.applicationEndDate
      ? new Date(questionnaire.applicationEndDate).toLocaleDateString('pt-BR')
      : 'Sem data final';

    return `${start} - ${end}`;
  }

  get isReadOnly(): boolean {
    return this.pageMode() === 'view';
  }

  private listenToAuthState(): void {
    this.authService.userRoles$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((roles) => this.userRoles.set(roles ?? []));

    this.authService.currentUser$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((user) => this.currentUser.set(user));
  }

  private listenToRoute(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        this.projectId = params.get('projectId');
        const questionnaireId = params.get('questionnaireId');
        this.questionnaireId = questionnaireId ? Number(questionnaireId) : null;
        this.requestedMode = (this.route.snapshot.queryParamMap.get('mode') as PageMode) ?? 'respond';

        if (!this.projectId || this.questionnaireId === null || Number.isNaN(this.questionnaireId)) {
          this.notification.showError('Identificador do questionário inválido.');
          this.onNavigateBack();
          return;
        }

        this.projectContext.setCurrentProjectId(this.projectId);
        this.loadResponse(1);
      });
  }

  loadResponse(page: number = this.currentPage()): void {
    if (!this.projectId || this.questionnaireId === null) {
      return;
    }

    this.state.set({ status: 'loading', error: null, data: null });

    this.currentPage.set(page);
    const zeroBasedPage = Math.max(page - 1, 0);

    this.responseService
      .loadResponsePage(
        this.projectId,
        this.questionnaireId,
        zeroBasedPage,
        this.pageSize,
        this.currentUser()?.email
      )
      .pipe(take(1))
      .subscribe({
        next: (payload: QuestionnaireResponsePayload) => {
          const resolvedMode = this.resolvePageMode(payload);
          this.pageMode.set(resolvedMode);
          this.pageSize = payload.pagination.pageSize;
          this.state.set({ status: 'loaded', error: null, data: payload });
        },
        error: () => {
          this.state.set({ status: 'error', error: 'Não foi possível carregar o questionário.', data: null });
        },
      });
  }

  private resolvePageMode(payload: QuestionnaireResponsePayload): PageMode {
    if (this.isAdmin()) {
      return 'view';
    }

    const respondent = this.getCurrentRespondent(payload);
    if (!respondent) {
      this.notification.showWarning('Você não está associado a este questionário.');
      this.onNavigateBack();
      return 'view';
    }

    if (respondent.status === QuestionnaireResponseStatus.Completed) {
      return 'view';
    }

    return this.requestedMode;
  }

  private getCurrentRespondent(payload?: QuestionnaireResponsePayload): QuestionnaireRespondentStatus | null {
    const source = payload ?? this.state().data;
    if (!source?.questionnaire.respondents?.length) {
      return null;
    }

    const email = this.currentUser()?.email?.toLowerCase() ?? '';
    return (
      source.questionnaire.respondents.find((respondent) =>
        respondent.email?.toLowerCase() === email
      ) ?? null
    );
  }

  private updateAnswer(questionId: number, partial: Partial<QuestionnaireAnswerDocument>): void {
    this.state.update((current) => {
      if (!current.data) {
        return current;
      }

      const answers = current.data.response.answers.map((answer) =>
        answer.questionId === questionId
          ? {
              ...answer,
              ...partial,
            }
          : answer
      );

      const status = answers.every((answer) => answer.response !== null)
        ? QuestionnaireResponseStatus.Completed
        : QuestionnaireResponseStatus.InProgress;

      return {
        ...current,
        data: {
          ...current.data,
          response: {
            ...current.data.response,
            status,
            answers,
          },
        },
      };
    });
  }

  private buildPrimaryNoteLink(note: string): QuestionnaireAttachmentLink | null {
    const descricao = note?.trim();
    if (!descricao) {
      return null;
    }

    return {
      descricao,
      url: '',
    };
  }
}
